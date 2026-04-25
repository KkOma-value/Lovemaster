#!/usr/bin/env bash
#
# wiki-update.sh — Lovemaster Wiki 自动更新入口
#
# 职责:
#   1. 防抖 + 锁机制，防止并发更新
#   2. 调用 graphify update . 生成知识图谱
#   3. 生成 index.md / CHANGELOG.md / .wiki-status.json
#
# 触发源:
#   - Git post-commit hook
#   - Claude Code PostToolUse hook
#   - 手动: bash scripts/wiki-update.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
WIKI_DIR="$PROJECT_ROOT/graphify-out"
LOCK_FILE="$WIKI_DIR/.update.lock"
STATUS_FILE="$WIKI_DIR/.wiki-status.json"
CHANGELOG_FILE="$WIKI_DIR/CHANGELOG.md"
INDEX_FILE="$WIKI_DIR/index.md"
DEBOUNCE_SECONDS=3

# ---- 着色 ----
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[wiki]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[wiki]${NC} $1"; }
log_error() { echo -e "${RED}[wiki]${NC} $1"; }

# ---- 防抖检查 ----
debounce_check() {
  local ts_file="$WIKI_DIR/.last-trigger"
  local now
  now=$(date +%s)

  if [ -f "$ts_file" ]; then
    local last
    last=$(cat "$ts_file")
    local diff=$(( now - last ))
    if [ "$diff" -lt "$DEBOUNCE_SECONDS" ]; then
      log_warn "防抖: 距上次触发仅 ${diff}s，跳过（需 >= ${DEBOUNCE_SECONDS}s）"
      return 1
    fi
  fi
  echo "$now" > "$ts_file"
  return 0
}

# ---- 锁管理 ----
acquire_lock() {
  if [ -f "$LOCK_FILE" ]; then
    local lock_age
    lock_age=$(($(date +%s) - $(stat -c %Y "$LOCK_FILE" 2>/dev/null || stat -f %m "$LOCK_FILE" 2>/dev/null || echo 0)))
    if [ "$lock_age" -lt 60 ]; then
      log_warn "更新锁被占用 (${lock_age}s)，跳过本次更新"
      return 1
    fi
    log_warn "更新锁已超时 (${lock_age}s)，强制释放"
    rm -f "$LOCK_FILE"
  fi
  echo "$$" > "$LOCK_FILE"
  return 0
}

release_lock() {
  rm -f "$LOCK_FILE"
}

# ---- 运行 graphify ----
run_graphify() {
  log_info "正在分析代码 AST 并生成知识图谱..."

  local start_time
  start_time=$(date +%s)

  cd "$PROJECT_ROOT"

  if ! graphify update . 2>&1; then
    log_error "graphify update 失败"
    return 1
  fi

  local end_time
  end_time=$(date +%s)
  local elapsed=$(( end_time - start_time ))

  log_info "知识图谱生成完成 (耗时 ${elapsed}s)"
  return 0
}

# ---- 扫描图谱数据，提取统计 ----
scan_graph_data() {
  local graph_json="$WIKI_DIR/graph.arch3.json"
  local node_count=0
  local edge_count=0
  local community_count=0

  if [ -f "$graph_json" ]; then
    node_count=$(python3 -c "
import json
with open('$graph_json') as f:
    data = json.load(f)
nodes = data.get('graph', {}).get('nodes', [])
# filter: exclude community/hub meta-nodes
real_nodes = [n for n in nodes if not n.get('id', '').startswith('_COMMUNITY_')]
print(len(real_nodes))
" 2>/dev/null || echo 0)

    edge_count=$(python3 -c "
import json
with open('$graph_json') as f:
    data = json.load(f)
edges = data.get('graph', {}).get('links', [])
print(len(edges))
" 2>/dev/null || echo 0)

    community_count=$(python3 -c "
import json
with open('$graph_json') as f:
    data = json.load(f)
hyperedges = data.get('graph', {}).get('hyperedges', [])
communities = [h for h in hyperedges if h.get('relation') == 'community']
print(len(communities))
" 2>/dev/null || echo 0)
  fi

  echo "$node_count $edge_count $community_count"
}

# ---- 生成 index.md ----
generate_index() {
  log_info "生成 wiki 索引..."

  local obsidian_dir="$WIKI_DIR/obsidian"
  local graph_json="$WIKI_DIR/graph.arch3.json"

  cat > "$INDEX_FILE" << 'HEADER'
# Lovemaster Wiki — 知识图谱

> 自动生成 | 最后更新: TIMESTAMP_PLACEHOLDER
> 节点: NODE_COUNT_PLACEHOLDER | 边: EDGE_COUNT_PLACEHOLDER | 社区: COMMUNITY_COUNT_PLACEHOLDER

---

## 快速导航

- [图谱报告](GRAPH_REPORT.arch3.md) — 完整的图谱分析报告
- [更新日志](CHANGELOG.md) — Wiki 变更历史
- [Obsidian Vault](obsidian/) — 所有节点页面的原始 Markdown 文件

---

HEADER

  # 获取统计数据
  read -r node_count edge_count community_count <<< "$(scan_graph_data)"

  # 替换占位符
  local now
  now=$(date '+%Y-%m-%d %H:%M:%S')
  sed -i "s/TIMESTAMP_PLACEHOLDER/$now/" "$INDEX_FILE"
  sed -i "s/NODE_COUNT_PLACEHOLDER/$node_count/" "$INDEX_FILE"
  sed -i "s/EDGE_COUNT_PLACEHOLDER/$edge_count/" "$INDEX_FILE"
  sed -i "s/COMMUNITY_COUNT_PLACEHOLDER/$community_count/" "$INDEX_FILE"

  # 按社区分组
  if [ -f "$graph_json" ]; then
    python3 -c "
import json

with open('$graph_json') as f:
    data = json.load(f)

hyperedges = data.get('graph', {}).get('hyperedges', [])
communities = [h for h in hyperedges if h.get('relation') == 'community']
nodes = data.get('graph', {}).get('nodes', [])

# Build node lookup
node_map = {n['id']: n for n in nodes}

md_lines = ['## 按社区分类', '']

for c in communities:
    label = c.get('label', 'Unknown')
    c_nodes = c.get('nodes', [])
    md_lines.append(f'### {label}')
    md_lines.append('')
    for nid in c_nodes:
        display = node_map.get(nid, {}).get('label', nid) if nid in node_map else nid
        file_slug = nid.replace(' ', '-').replace('/', '-')
        md_lines.append(f'- [[obsidian/{file_slug}|{display}]]')
    md_lines.append('')

# Highlight god nodes (most connected)
god_nodes = sorted(nodes, key=lambda n: len(n.get('connections', [])), reverse=True)[:10]
if god_nodes:
    md_lines.append('---')
    md_lines.append('')
    md_lines.append('## 核心节点 (God Nodes)')
    md_lines.append('')
    for gn in god_nodes:
        label = gn.get('label', gn['id'])
        nid = gn['id'].replace(' ', '-').replace('/', '-')
        conns = len(gn.get('connections', []))
        md_lines.append(f'- [[obsidian/{nid}|{label}]] ({conns} 个连接)')
    md_lines.append('')

with open('$INDEX_FILE', 'a', encoding='utf-8') as f:
    f.write('\n'.join(md_lines))
" 2>/dev/null || true
  fi

  log_info "index.md 生成完成"
}

# ---- 生成 CHANGELOG.md ----
generate_changelog() {
  local trigger="${1:-unknown}"
  local now
  now=$(date '+%Y-%m-%d %H:%M:%S')
  read -r node_count edge_count community_count <<< "$(scan_graph_data)"

  # Ensure changelog file exists
  if [ ! -f "$CHANGELOG_FILE" ]; then
    cat > "$CHANGELOG_FILE" << 'CHANGELOG_HEADER'
# Wiki 更新日志

记录每次自动更新的变更。

CHANGELOG_HEADER
  fi

  # Prepend new entry after header
  local tmp_file="$WIKI_DIR/.changelog-tmp"
  local new_entry="## $now | 触发: $trigger | ${node_count}节点 ${edge_count}边 ${community_count}社区"
  {
    head -3 "$CHANGELOG_FILE"
    echo ""
    echo "$new_entry"
    echo ""
    tail -n +4 "$CHANGELOG_FILE"
  } > "$tmp_file"
  mv "$tmp_file" "$CHANGELOG_FILE"
}

# ---- 写入状态文件 ----
write_status() {
  local trigger="${1:-unknown}"
  local now
  now=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
  read -r node_count edge_count community_count <<< "$(scan_graph_data)"

  cat > "$STATUS_FILE" << EOF
{
  "lastUpdate": "$now",
  "trigger": "$trigger",
  "nodes": $node_count,
  "edges": $edge_count,
  "communities": $community_count,
  "pid": $$
}
EOF
}

# ============================================================
# 主流程
# ============================================================

main() {
  local trigger="${1:-manual}"
  log_info "Wiki 自动更新触发 (来源: $trigger)"

  mkdir -p "$WIKI_DIR"

  # 1. 防抖
  if ! debounce_check; then
    return 0
  fi

  # 2. 获取锁
  if ! acquire_lock; then
    return 0
  fi

  # 3. 执行 graphify
  if ! run_graphify; then
    release_lock
    return 1
  fi

  # 4. 生成辅助文件
  generate_index
  generate_changelog "$trigger"
  write_status "$trigger"

  # 5. 释放锁
  release_lock

  log_info "Wiki 自动更新完成"
}

main "$@"

#!/usr/bin/env bash
#
# setup-wiki-autoupdate.sh — 一键安装 Wiki 自动更新
#
# 安装内容:
#   1. Git post-commit hook
#   2. 初始化 wiki 目录和辅助文件
#   3. 首次运行 graphify 生成图谱
#
# 使用: bash scripts/setup-wiki-autoupdate.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
HOOKS_DIR="$PROJECT_ROOT/.git/hooks"
POST_COMMIT="$HOOKS_DIR/post-commit"
WIKI_DIR="$PROJECT_ROOT/graphify-out"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[setup]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[setup]${NC} $1"; }

echo "============================================"
echo " Lovemaster Wiki 自动更新 — 安装"
echo "============================================"
echo ""

# ---- Step 1: 创建 post-commit hook ----
log_info "[1/4] 安装 Git post-commit hook..."

mkdir -p "$HOOKS_DIR"

cat > "$POST_COMMIT" << 'HOOK_SCRIPT'
#!/usr/bin/env bash
# Lovemaster Wiki Auto-Update — post-commit hook
# 每次 git commit 后自动更新知识图谱 wiki
bash scripts/wiki-update.sh "post-commit" &
disown
HOOK_SCRIPT

# 确保可执行
chmod +x "$POST_COMMIT" 2>/dev/null || true

log_info "  post-commit hook 已安装: $POST_COMMIT"

# ---- Step 2: 确保 wiki 目录存在 ----
log_info "[2/4] 初始化 wiki 目录..."
mkdir -p "$WIKI_DIR"

# ---- Step 3: 首次生成图谱 ----
log_info "[3/4] 首次运行 graphify..."
if command -v graphify &>/dev/null; then
  cd "$PROJECT_ROOT"
  if graphify update . 2>&1; then
    log_info "  首次图谱生成成功"
  else
    log_warn "  首次图谱生成失败，请在修复后手动运行: graphify update ."
  fi
else
  log_warn "  graphify 未安装，跳过首次生成。请先安装: pip install graphify"
fi

# ---- Step 4: 生成索引 ----
log_info "[4/4] 生成初始 wiki 索引..."
bash "$SCRIPT_DIR/wiki-update.sh" "setup" 2>/dev/null || true

echo ""
log_info "安装完成！"
echo ""
echo "使用说明:"
echo "  - 每次 git commit 后 wiki 自动更新 (后台异步)"
echo "  - Claude Code 编辑文件后自动更新 (PostToolUse hook)"
echo "  - 手动更新: bash scripts/wiki-update.sh"
echo "  - 查看 wiki: 打开 graphify-out/index.md"
echo "  - 查看状态: cat graphify-out/.wiki-status.json"

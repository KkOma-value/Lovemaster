# UIUX: Lovemaster 后端优化 - Dify RAG + Step 2 蒸馏触点

> 版本：v1.1
> 日期：2026-04-19
> 关联文档：
> - PRD: `output/backend-optimize-prd.md`
> - Architecture: `output/backend-optimize-architecture.md`
> - UI Contract: `output/Lovemaster-ui-contract.json`

## 1. 概述

本 UI/UX 文档承接 PRD §2 Step 2 经验蒸馏管道和 §5 前端触点需求，定义轻量化的用户界面变更。Step 1 保持现有对话 UI 完全不变（后端无感知），Step 2 仅新增以下三个触点：
1. SaveToWikiButton（消息级入口）
2. 自动反馈埋点提示（无评分）
3. Admin 审核入口（非主导航）

所有 UI 决策遵循已冻结的 Design Token 系统（`--color-primary: #C47B5A`）和 Lucide 图标库。

本轮不是“纯后端无感”改造，而是“后端主导 + 轻量前端触点”方案：
1. Step 1 保持现有对话 UI 稳定，仅优化检索链路。
2. Step 2 新增知识蒸馏触点，但不引入复杂新页面流程。
3. Step 3 才展示策略有效性，不在 Step 2 暴露成功率。

---

## 2. Step 2 前端触点

### 2.1 SaveToWikiButton（消息级入口）

位置：消息气泡底部操作条。

交互状态：
1. `idle`：可提交候选。
2. `submitting`：按钮 loading，禁止重复点击。
3. `candidate`：提交成功，图标切换为确认态。
4. `rejected`：提交失败或被拒绝。
5. `unknown_topic`：提示“待人工归类”。

视觉规范：
1. 图标使用 Lucide（例如 BookmarkPlus/BookmarkCheck）。
2. 最小点击区域 32x32。
3. 必须有 aria-label。

### 2.2 自动反馈埋点提示（无评分）

在 Kiko 高分候选自动入池时，卡片底部显示轻量提示：
1. 已提交知识蒸馏。
2. 仅说明“已采集”，不展示任何策略成功率。

同时保留用户主动反馈入口：
1. ThumbsUp。
2. ThumbsDown。

### 2.3 Admin 审核入口（非主导航）

入口路径：`/admin/knowledge/review`。

页面要求：
1. Tabs：待审核 / 已通过 / 已驳回。
2. 列表中展示 Topic 标签组（阶段/意图/问题）与 Merge 预览摘要。
3. 支持快捷键（如 J/K/Y/N）提高审核效率。

---

## 3. Topic 分类的前端呈现约束

1. Topic 标签必须来自后端校验后的枚举结果，不做前端自由生成。
2. 若分类 fallback 为 `unknown`，前端只显示“待人工归类”，不做猜测。
3. Step 2 阶段禁止展示策略分、胜率或推荐等级，避免误导用户。
4. Topic 粒度遵循“先粗后细”，第一版只展示粗粒度标签。

---

## 4. 核心交互流程

### 4.1 手动提交候选

1. 用户点击 SaveToWikiButton。
2. 前端调用候选提交接口，按钮进入 `submitting`。
3. 成功后显示 `candidate`，并展示“已提交蒸馏队列”。
4. 若返回 `unknown_topic`，显示“待人工归类”。

### 4.2 自动触发候选

1. 触发条件命中（例如 Kiko 分数达到阈值）。
2. 前端展示轻提示“已提交知识蒸馏”。
3. 不展示策略评分，只保留反馈按钮。

---

## 5. Step 3 预留（本轮不做）

本轮不展示以下内容：
1. 策略成功率。
2. Topic 内策略排行榜。
3. 对话实时策略推荐分。

这些内容统一放入 Step 3 的策略看板和灰度推荐中。

---

## 6. UI 红线

1. 图标仅使用 Lucide/Heroicons/Tabler。
2. 禁用 emoji 作为功能图标。
3. 禁用紫粉渐变模板化风格。
4. 复用现有 Button/Card/Input 设计体系，避免额外设计分叉。

---

## 7. 不做清单（Step 2）

1. 不做用户端“知识库浏览器”页面。
2. 不做策略分可视化图表。
3. 不做知识图谱可视化页面。
4. 不改主聊天路径和 SSE 协议。

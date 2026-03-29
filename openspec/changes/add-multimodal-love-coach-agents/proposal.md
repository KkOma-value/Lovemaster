# Change: add multimodal love coach agents

## Why
Lovemaster 当前已经支持前端图片上传，但图片只完成了存储，尚未进入 LoveApp 或 Coach 的模型推理链路。用户现在无法真正通过聊天截图获得“对方是什么意思 / 我该怎么回”的分析结果，也无法在 Coach 模式里先完成截图理解再决定是否进入工具执行。

## What Changes
- **ADDED** 多模态恋爱聊天能力：支持 Love / Coach 模式接收聊天截图，并在进入主回复流程前执行 OCR 与问题重写。
- **ADDED** Love 模式截图分析链路：对聊天截图进行识别、整理、重写，并输出关系分析与可直接发送的话术建议。
- **ADDED** Coach 模式决策链路：在工具执行前先完成输入理解，由大脑层判断是否真的需要进入 Manus 工具执行。
- **MODIFIED** 前端聊天体验：输入区明确支持聊天截图，显示分阶段理解状态，并区分 Love 模式与 Coach 模式的文案和行为。
- **MODIFIED** Coach 面板打开策略：普通分析不自动展开面板，只有进入任务执行态时才展开。

## Impact
- Affected specs:
  - `frontend-chat-ui`
  - `manus-agent`
  - `multimodal-relationship-chat` (new)
- Affected code:
  - `springai-front-react/src/pages/Home/*`
  - `springai-front-react/src/pages/Chat/*`
  - `springai-front-react/src/components/Chat/*`
  - `springai-front-react/src/hooks/usePanelData.js`
  - `springai-front-react/src/hooks/useSSEConnection.js`
  - `src/main/java/org/example/springai_learn/controller/AiController.java`
  - `src/main/java/org/example/springai_learn/app/LoveApp.java`
  - `src/main/java/org/example/springai_learn/agent/*`
  - new orchestrator / multimodal service classes under `src/main/java/org/example/springai_learn`

# Graph Report - .  (2026-04-18)

## Corpus Check
- Large corpus: 366 files · ~1,271,195 words. Semantic extraction will be expensive (many Claude tokens). Consider running on a subfolder, or use --no-semantic to run AST-only.

## Summary
- 39 nodes · 44 edges · 5 communities detected
- Extraction: 68% EXTRACTED · 32% INFERRED · 0% AMBIGUOUS · INFERRED: 14 edges (avg confidence: 0.5)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]

## God Nodes (most connected - your core abstractions)

## Surprising Connections (you probably didn't know these)
- `CoachChatOrchestrator` --related_to--> `BrainAgent`  [INFERRED]
   →   _Bridges community 4 → community 3_
- `AiController` --related_to--> `ChatInputContext`  [INFERRED]
   →   _Bridges community 2 → community 4_
- `多Agent编排系统` --related_to--> `ToolsAgent`  [EXTRACTED]
   →   _Bridges community 2 → community 3_
- `ToolsAgentResult` --related_to--> `MarkdownRenderer组件`  [EXTRACTED]
   →   _Bridges community 3 → community 0_
- `ChatPage组件` --related_to--> `ChatMessages组件`  [EXTRACTED]
   →   _Bridges community 1 → community 0_

## Hyperedges (group relationships)
- **多Agent编排流程** — chat-input-context, intake-rewrite-agent, intake-analysis-result, brain-agent, brain-decision, tools-agent, tools-agent-result [EXTRACTED]
- **图片内联渲染全链路** — image-upload-service, tools-agent-result, image-storage-service, supabase-storage, tools-agent-result, markdown-renderer, image-lightbox, chat-messages [EXTRACTED]
- **前端视觉设计系统** — chat-page, sidebar-component, manus-panel [EXTRACTED]
- **两种使用模式** — love-mode, coach-mode, ai-controller, intake-rewrite-agent, brain-agent, manus-panel [EXTRACTED]
- **后端多模态支持** — spring-ai-multimodal, qwen-vl-ocr, spring-ai-alibaba-lib, intake-rewrite-agent, chat-input-context [EXTRACTED]
- **前后端图片流** — chat-input, image-upload-service, chat-api-client, ai-controller, image-storage-service, chat-messages [EXTRACTED]
- **Agent继承体系** — tool-registration, mcp-client [EXTRACTED]
- **数据持久化链路** — conversation-images-table, postgres-db, image-storage-service, supabase-storage, redis-cache [EXTRACTED]
- **SSE实时反馈系统** — chat-context, chat-messages, manus-panel [EXTRACTED]

## Communities

### Community 0 - "Community 0"
Cohesion: 0.0
Nodes (10): ChatMessages组件, conversation_images表, ImageWithLightbox组件, ConversationImageStorageService, Coach 模式图片内联渲染, LoveApp应用, MarkdownRenderer组件, PostgreSQL数据库 (+2 more)

### Community 1 - "Community 1"
Cohesion: 0.0
Nodes (10): chatApi.js, ChatContext.jsx, ChatInput组件, ChatPage组件, 图片上传服务, Intake阶段, ManusPanel组件, Redis缓存 (+2 more)

### Community 2 - "Community 2"
Cohesion: 0.0
Nodes (7): ChatInputContext, IntakeAnalysisResult, IntakeRewriteAgent, 多Agent编排系统, OCR集成方案, 阿里云Qwen-VL-OCR模型, spring-ai-alibaba库

### Community 3 - "Community 3"
Cohesion: 0.0
Nodes (6): CoachChatOrchestrator, MCP客户端, mcp-servers模块, ToolRegistration, ToolsAgent, ToolsAgentResult

### Community 4 - "Community 4"
Cohesion: 0.0
Nodes (6): AiController, BrainAgent, BrainDecision, Coach/Manus模式 - 任务助手, Love模式 - 陪聊分析, Spring AI多模态接口
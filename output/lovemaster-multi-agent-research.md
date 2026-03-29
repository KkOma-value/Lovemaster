# Research: Lovemaster 多 Agent 恋爱陪聊 / Manus 模式

> 日期：2026-03-27  
> 输入来源：`2026-03-27-003644-command-messagesuper-devcommand-message.txt`

## 1. 结论先行

这次需求不是简单加一个 OCR 接口，而是把现有 Lovemaster 从“双聊天入口”升级成“两种工作模式 + 三类 Agent + 截图理解能力”的产品。

最重要的结论有 4 个：

1. 当前前端已经支持图片上传，并把 `imageUrl` 追加到 LoveApp / Coach 的 SSE 请求里，但后端两个聊天接口都没有接收或消费这个参数，所以“上传截图让 AI 分析聊天记录”现在实际上没有闭环。
2. 现有后端已经具备可复用的 Agent 基础设施：`BaseAgent`、`ReActAgent`、`ToolCallAgent`、`KkomaManus`，适合做第一阶段的增量式多 Agent 改造。
3. Spring AI 本身已经提供了多模态 `UserMessage.media(...)` / `ChatClient.user(...).media(...)` 抽象，DashScope 侧也已经提供面向 OCR 的 `qwen-vl-ocr` 能力，所以图片理解不需要自建 OCR 服务。
4. 对当前仓库最稳妥的路线不是立刻整体迁移到新版 Graph 框架，而是先基于现有 Agent 体系补出“截图理解/重写 Agent + 大脑 Agent + 工具代理 Agent”三段式编排；Graph / Supervisor 能力可以作为第二阶段升级方向。

---

## 2. 用户需求还原

命令文件里描述的是两个核心用户旅程：

### 2.1 陪聊模式

- 用户上传和对象/暧昧对象的聊天记录文本或截图
- 用户提问“我该怎么回”或“对方是什么意思”
- 系统先做文字提取、意图重写、上下文补全
- 再由大脑 Agent 进行情绪陪伴、关系分析、回复建议生成

### 2.2 Manus 模式

- 用户输入问题或上传截图后
- AI 可以选择继续陪伴聊天
- 也可以自主调用工具去完成任务
- 这里需要更清晰的“决策层”和“执行层”分工

命令文件中给出的目标 Agent 划分是：

1. 文字重写和识别 Agent
2. 大脑 Agent
3. 工具调用 Agent

这个拆分方向合理，且与当前项目已有结构兼容。

---

## 3. 当前项目现状

## 3.1 前端现状

React 前端当前已经有两种聊天入口：

- `loveapp`：普通陪聊
- `coach`：带 `ManusPanel` 的工具执行模式

当前已存在的相关能力：

- `ChatInput.jsx` 支持图片选择、压缩、上传
- `chatApi.js` 会把 `imageUrl` 拼到 `/ai/love_app/chat/sse` 和 `/ai/manus/chat`
- `ChatPage.jsx` 已支持 `loveapp` / `coach` 两种类型切换
- `ManusPanel` 已具备任务、终端、预览区

当前缺口：

- 上传图片只完成“存储”，没有进入模型推理链路
- UI 没有“截图解析中 / OCR 成功 / 识别失败 / 重写后的问题”这类反馈
- Love 模式与 Coach 模式在交互层差异还不够清晰，本质只是不同 SSE 地址

## 3.2 后端现状

当前聊天入口：

- `AiController#doChatWithLoveAppSSE`
- `AiController#doChatWithManus`

当前 Agent / App 结构：

- `LoveApp`：普通聊天、RAG、工具聊天、MCP 聊天
- `BaseAgent`：状态机 + SSE 事件发送
- `ReActAgent`：think / act 循环
- `ToolCallAgent`：工具调用编排
- `KkomaManus`：已有的自主工具代理

当前多模态缺口：

- 后端接口未接收 `imageUrl`
- `LoveApp` 仅调用纯文本 `.user(message)`
- 项目中没有 `UserMessage.media(...)` 或 `ChatClient.user(...).media(...)` 的使用
- 已有图片上传服务只负责保存文件与返回访问 URL

## 3.3 技术基础判断

现有代码已经具备这些可以复用的资产：

- 持久化会话记忆
- SSE 流式输出
- 工具注册与 MCP 集成
- 分模式的前端会话页
- 已存在的图片上传 / 预览链路

因此第一阶段不需要推倒重写。

---

## 4. 外部研究

## 4.1 同类产品模式

### A. Rizz / AI Dating Assistant 类产品

市场上成熟的“恋爱回复助手”产品，核心共性很一致：

- 支持粘贴聊天记录或上传聊天截图
- 生成多种语气的回复候选
- 强调“降低措辞焦虑”和“跨平台可用”
- 不是深度 Agent，而是高频、轻任务、快反馈

外部样本：

- TIME 在 2024 年 Best Inventions 中提到，Rizz 的核心价值就是基于截图判断“现在该推进、继续聊还是放弃”，并支持 iMessage、WhatsApp 等任意平台的截图输入。
- Winggg 官方站点强调“基于资料和正在进行的对话做上下文分析”，覆盖 dating app 与普通短信，核心卖点是 message replies、openers、ask-outs。
- App Store 上多个 Rizz 类产品都把“Upload screenshots / paste messages / choose vibe”作为主路径。

这说明 Lovemaster 的陪聊模式应该优先做到：

1. 截图和文本都能输入
2. 能分析对方意图和关系状态
3. 能一次给出多种回复风格
4. 结果要快，不能像长流程代理一样拖沓

### B. Manus 类产品

Manus 官方文档把自己定义为“不是只回答问题，而是能执行任务并交付结果的自主 Agent”，核心特征包括：

- 有自己的沙箱环境和文件系统
- 能执行多步骤任务
- 能实时展示过程
- 强调 secure isolated VM 和 task delivery

这和当前仓库里的 `coach` 模式方向是一致的。问题不在于“要不要做 Manus 模式”，而在于当前实现还缺少：

- 输入理解层
- 明确的路由决策层
- 对图片输入的支持
- 陪聊和工具执行之间的切换策略

## 4.2 模型与框架能力

### A. Alibaba Cloud Qwen-OCR

截至 2026-03-26，Alibaba Cloud Model Studio 的 `qwen-vl-ocr` 已明确支持：

- 从图片提取文本和结构化数据
- 多语言识别
- 文字定位
- 表格解析、关键信息抽取等 OCR 任务
- OpenAI compatible 方式调用，或 DashScope SDK 调用

关键约束：

- 图片建议不超过 15.68 MP
- 公网 URL、本地路径、Base64 都有 10 MB 上限
- 稳定模型上下文窗口 38,192 tokens
- 单图最大输入 30,000 tokens
- 最大输出 8,192 tokens

这足够覆盖“聊天截图识别 + 结构化提取 + 对模糊字保守处理”的场景。

### B. Spring AI 多模态能力

Spring AI 官方文档已经给出标准多模态写法：

- `UserMessage.builder().text(...).media(...)`
- 或 `ChatClient.create(chatModel).prompt().user(u -> u.text(...).media(...))`

这意味着当前仓库只要把 `imageUrl` 或本地图片资源转成 `Media`，就可以在现有 Spring AI ChatModel 体系内实现图片输入，不需要绕开 Spring AI 自写 HTTP client。

### C. Spring AI Alibaba 多 Agent / Graph

Spring AI Alibaba 官网当前明确提供：

- `ReactAgent`
- `ParallelAgent`
- `LlmRoutingAgent`
- `LoopAgent`
- Graph 状态流转
- Streaming
- Human in the loop
- Memory / Context

这说明从框架演进角度，未来可以迁移到 Graph / Supervisor 风格的编排。

但当前仓库依赖仍是：

- `spring-ai-alibaba-starter: 1.0.0-M6.1`
- `spring-ai-mcp-client-spring-boot-starter: 1.0.0-M6`

而官网文档已经在 1.1.x 线演进。直接升级会引入额外的不确定性，因此第一阶段更适合“沿用现有自定义 Agent 基础设施，先做产品闭环”，而不是先赌框架升级。

---

## 5. 产品机会判断

Lovemaster 的差异化不应该只是“又一个 Rizz”。

更合理的定位是：

- 陪聊模式：更像懂关系博弈和情绪陪伴的恋爱顾问
- Manus 模式：更像能代替用户规划、搜索、生成资料、执行步骤的恋爱任务助理

也就是说，它要同时覆盖两层价值：

1. 高频、轻量、即时的“我现在怎么回”
2. 低频、复杂、需要任务拆解的“帮我做完整方案”

如果做成一个模型一句话全包，会很快失控；拆成三层 Agent 是正确方向。

---

## 6. 推荐方案

## 6.1 第一阶段推荐架构

采用三段式增量改造：

1. `IntakeRewriteAgent`
   - 负责截图 OCR、聊天文本清洗、角色归因、问题重写、缺失信息标注
2. `BrainAgent`
   - 负责陪伴式回复、关系判断、是否需要调用工具的决策
3. `ToolBrokerAgent`
   - 负责实际工具编排，可复用 `ToolCallAgent` / `KkomaManus`

模式差异：

- Love 模式：默认只走 `IntakeRewriteAgent -> BrainAgent`
- Coach / Manus 模式：走 `IntakeRewriteAgent -> BrainAgent -> ToolBrokerAgent(可选)`

## 6.2 为什么不第一阶段直接上 Graph

原因很实际：

- 当前仓库已经有自定义 SSE、工具事件、会话记忆
- `KkomaManus` 已在线上主链路里承担 Coach 模式
- 先把图片输入和多 Agent 决策闭环做通，比先升级底层框架更值

Graph / Supervisor 的价值仍然很高，但更适合第二阶段：

- 需要并行子代理
- 需要 human-in-the-loop
- 需要更精细的状态节点恢复
- 需要更复杂的图形化运行时

---

## 7. 核心风险

## 7.1 隐私与安全

用户上传的是恋爱聊天截图，往往包含：

- 真实姓名
- 社交账号
- 头像
- 关系状态
- 敏感对话

这类数据必须在产品设计层面加入：

- 明确的“仅用于当前分析”的提示
- 默认不在前端长期暴露图片 URL
- 识别后尽量只保留结构化文本和最小必要摘要
- 日志脱敏

## 7.2 OCR 误读

聊天截图常见问题：

- 截图裁切不完整
- 模糊、夜间模式、气泡重叠
- 双方说话人识别错误

因此 Intake Agent 不能只输出“识别文本”，还要输出：

- 识别置信提示
- 无法确认的片段
- 说话人归属不确定项

## 7.3 自主工具调用失控

Coach 模式如果默认过度调用工具，会让本来只想“问怎么回”的用户遭遇过长流程，体验变差。

因此需要明确路由规则：

- 轻咨询默认不调工具
- 只有搜索、生成计划、整理文案、文件输出等长任务才切到工具代理

---

## 8. 对后续文档的直接影响

基于这份研究，后续 PRD / Architecture / UIUX 应该坚持以下边界：

1. 第一阶段不做全量框架升级，优先打通图片理解闭环。
2. 新能力不是替换现有 LoveApp / Coach，而是在其上增加明确的多 Agent 分层。
3. Love 模式优先“快、准、共情”； Coach 模式优先“能规划、能执行、过程可见”。
4. 图片上传后必须有可见状态，不允许“用户上传了图但系统其实没看图”。

---

## 9. 参考来源

- Alibaba Cloud Model Studio, Qwen-OCR, last updated 2026-03-26  
  https://www.alibabacloud.com/help/en/model-studio/qwen-vl-ocr
- Spring AI Reference, Multimodality API  
  https://docs.spring.io/spring-ai/reference/api/multimodality.html
- Spring AI Alibaba Overview / Workflow / Multi-Agent docs  
  https://java2ai.com/docs/overview/  
  https://java2ai.com/en/docs/frameworks/agent-framework/advanced/workflow/  
  https://java2ai.com/en/docs/frameworks/agent-framework/advanced/multi-agent
- TIME Best Inventions 2024: Rizz  
  https://time.com/collections/best-inventions-2024/7094844/rizz/
- Winggg official website  
  https://winggg.com/
- Manus documentation / help center  
  https://manus.im/docs/ja/introduction/welcome  
  https://help.manus.im/en/articles/14033996-is-my-data-safe-when-using-manus-agents-in-telegram

package org.example.springai_learn.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.ChatMemory.DatabaseChatMemory;
import org.example.springai_learn.agent.KkomaManus;
import org.example.springai_learn.ai.context.BrainDecision;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * ToolsAgent — 工具供给与执行代理。
 * <p>
 * 职责：
 * 当 BrainAgent 判断需要工具时，由 Orchestrator 激活本服务。
 * ToolsAgent 将已注册的工具交给执行引擎 (KkomaManus)，同步执行并返回结果给 BrainAgent。
 * <p>
 * 关系：BrainAgent.decide() → 需要工具 → ToolsAgent.activate() → KkomaManus 执行 → 结果返回 → BrainAgent.synthesize()
 */
@Service
@Slf4j
public class ToolsAgentService {

    private final ToolCallback[] allTools;
    private final ChatModel toolsModel;
    private final DatabaseChatMemory databaseChatMemory;

    @Autowired(required = false)
    private ToolCallbackProvider mcpToolCallbackProvider;

    public ToolsAgentService(
            ToolCallback[] allTools,
            @Qualifier("toolsModel") ChatModel toolsModel,
            DatabaseChatMemory databaseChatMemory) {
        this.allTools = allTools;
        this.toolsModel = toolsModel;
        this.databaseChatMemory = databaseChatMemory;
    }

    /**
     * 激活 ToolsAgent，同步执行 Brain 分配的任务。
     * <p>
     * 工具执行过程中的 file_created 事件会通过 emitter 推送给客户端。
     * 执行完成后返回工具结果文本，供 BrainAgent 综合。
     *
     * @param decision       BrainAgent 的决策
     * @param conversationId 会话复合 ID
     * @param emitter        SSE 发送器（file_created 等事件通过此推送）
     * @return 工具执行的结果文本
     */
    public String activate(BrainDecision decision, String conversationId, SseEmitter emitter) {
        log.info("ToolsAgent 被激活: conversationId={}, taskPrompt={}",
                conversationId, shorten(decision.toolTaskPrompt(), 120));

        KkomaManus kkomaManus = new KkomaManus(
                allTools, toolsModel, mcpToolCallbackProvider,
                conversationId, databaseChatMemory);

        // 设置 emitter 以便工具执行期间 file_created 事件能推送到客户端
        kkomaManus.setCurrentEmitter(emitter);

        try {
            String result = kkomaManus.run(decision.toolTaskPrompt());
            log.info("ToolsAgent 执行完成: result={}", shorten(result, 200));
            return result;
        } finally {
            kkomaManus.saveToChatMemory();
        }
    }

    private String shorten(String text, int limit) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }
}

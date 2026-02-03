package org.example.springai_learn.agent;

import org.example.springai_learn.agent.model.AgentState;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolCallAgentTest {

    @Test
    void act_shouldNotCastLastMessageAndShouldFinishOnTerminate() throws Exception {
        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[]{});
        agent.setName("test-agent");

        ChatResponse chatResponse = Mockito.mock(ChatResponse.class);
        Mockito.when(chatResponse.hasToolCalls()).thenReturn(true);

        ToolCallingManager toolCallingManager = Mockito.mock(ToolCallingManager.class);

        ToolResponseMessage.ToolResponse writeFile = new ToolResponseMessage.ToolResponse("1", "WriteFile", "ok");
        ToolResponseMessage.ToolResponse terminate = new ToolResponseMessage.ToolResponse("2", "doTerminate", "done");
        ToolResponseMessage toolResponseMessage = new ToolResponseMessage(List.of(writeFile, terminate));

        // 最后一条消息不是 ToolResponseMessage，验证不会发生 ClassCastException
        List<Message> history = List.of(
                new AssistantMessage("calling tools"),
                toolResponseMessage,
                new AssistantMessage("after tool")
        );

        ToolExecutionResult toolExecutionResult = () -> history;

        Mockito.when(toolCallingManager.executeToolCalls(Mockito.any(Prompt.class), Mockito.eq(chatResponse)))
                .thenReturn(toolExecutionResult);

        setField(agent, "toolCallChatResponse", chatResponse);
        setField(agent, "toolCallingManager", toolCallingManager);

        String result = agent.act();

        assertTrue(result.contains("工具 WriteFile"), result);
        assertEquals(AgentState.FINISHED, agent.getState());
    }

    @Test
    void act_shouldReturnReadableMessageWhenNoToolResponseMessagePresent() throws Exception {
        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[]{});
        agent.setName("test-agent");

        ChatResponse chatResponse = Mockito.mock(ChatResponse.class);
        Mockito.when(chatResponse.hasToolCalls()).thenReturn(true);

        ToolCallingManager toolCallingManager = Mockito.mock(ToolCallingManager.class);

        List<Message> history = List.of(
                new AssistantMessage("calling tools"),
                new AssistantMessage("still no tool response")
        );
        ToolExecutionResult toolExecutionResult = () -> history;

        Mockito.when(toolCallingManager.executeToolCalls(Mockito.any(Prompt.class), Mockito.eq(chatResponse)))
                .thenReturn(toolExecutionResult);

        setField(agent, "toolCallChatResponse", chatResponse);
        setField(agent, "toolCallingManager", toolCallingManager);

        String result = agent.act();

        assertTrue(result.contains("未找到工具响应消息"), result);
        assertEquals(AgentState.IDLE, agent.getState());
    }

    @Test
    void act_shouldFailFastWithDiagnosisWhenNoToolCallsRepeated() throws Exception {
        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[]{});
        agent.setName("test-agent");

        setField(agent, "failFastNoToolCallsThisStep", true);
        setField(agent, "lastHasToolCalls", false);
        setField(agent, "lastAssistantToolCallCount", 0);
        setField(agent, "lastAssistantText", "上海 3 天游玩攻略：...（但未调用工具）");

        String result = agent.act();

        assertTrue(result.contains("未检测到工具调用"), result);
        assertTrue(result.contains("ChatResponse.hasToolCalls()"), result);
        assertEquals(AgentState.FINISHED, agent.getState());
    }

    @Test
    void enforceMessageBudgets_shouldTruncateLongToolResponse() throws Exception {
        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[]{});

        // Create a tool response with content exceeding TOOL_RESPONSE_CHAR_BUDGET (20,000)
        String longContent = "x".repeat(25_000);
        ToolResponseMessage.ToolResponse resp = new ToolResponseMessage.ToolResponse("1", "ReadFile", longContent);
        ToolResponseMessage toolMsg = new ToolResponseMessage(List.of(resp));

        List<Message> messages = List.of(toolMsg);

        // Use reflection to call private method
        java.lang.reflect.Method method = ToolCallAgent.class.getDeclaredMethod("enforceMessageBudgets", List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Message> result = (List<Message>) method.invoke(agent, messages);

        assertEquals(1, result.size());
        ToolResponseMessage truncated = (ToolResponseMessage) result.get(0);
        String data = truncated.getResponses().get(0).responseData();
        assertTrue(data.length() <= 20_000 + 20, "Tool response should be truncated, actual: " + data.length());
        assertTrue(data.contains("[truncated]"), "Should contain truncation marker");
    }

    @Test
    void enforceMessageBudgets_shouldDropOldMessagesWhenOverBudget() throws Exception {
        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[]{});

        // Create messages totaling more than REQUEST_CHAR_BUDGET (900,000)
        // After truncation each message is ~20k chars, so we need ~50 messages to exceed 900k
        List<Message> messages = new java.util.ArrayList<>();
        for (int i = 0; i < 50; i++) {
            // Note: content will be truncated to 20k by clampToolResponse, so total ~50*20k = 1M > 900k
            String content = "msg" + i + "-" + "y".repeat(100_000);
            ToolResponseMessage.ToolResponse resp = new ToolResponseMessage.ToolResponse(
                    String.valueOf(i), "Tool" + i, content);
            messages.add(new ToolResponseMessage(List.of(resp)));
        }

        java.lang.reflect.Method method = ToolCallAgent.class.getDeclaredMethod("enforceMessageBudgets", List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Message> result = (List<Message>) method.invoke(agent, messages);

        // Should have dropped some old messages to fit within budget
        assertTrue(result.size() < messages.size(), "Should have dropped messages: " + result.size() + " vs " + messages.size());
        // Most recent messages should be retained (at the end of the list)
        // The last message in result should be from the original last message
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

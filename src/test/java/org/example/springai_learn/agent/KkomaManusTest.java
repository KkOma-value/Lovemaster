package org.example.springai_learn.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KkomaManusTest {

    @Test
    void constructor_shouldLoadHistoryUsingCompositeConversationId() {
        ChatMemory chatMemory = mock(ChatMemory.class);
        ChatModel chatModel = mock(ChatModel.class);

        when(chatMemory.get("user-1:coach:chat-1", 20)).thenReturn(List.of(new AssistantMessage("历史记录")));

        new KkomaManus(new ToolCallback[]{}, chatModel, null, "user-1:coach:chat-1", chatMemory);

        verify(chatMemory).get("user-1:coach:chat-1", 20);
    }

    @Test
    void saveToChatMemory_shouldPersistUsingCompositeConversationId() {
        ChatMemory chatMemory = mock(ChatMemory.class);
        ChatModel chatModel = mock(ChatModel.class);
        KkomaManus manus = new KkomaManus(new ToolCallback[]{}, chatModel, null, "user-1:coach:chat-2", chatMemory);

        manus.setMessageList(List.of(
                new UserMessage("请帮我整理计划"),
                new UserMessage("Analyze the current state and immediately take action using the most appropriate tool."),
                new AssistantMessage("这是最终结果")
        ));

        manus.saveToChatMemory();

        verify(chatMemory).clear("user-1:coach:chat-2");
        verify(chatMemory).add(eq("user-1:coach:chat-2"), anyList());
        verify(chatMemory, atLeastOnce()).get(eq("user-1:coach:chat-2"), anyInt());
    }
}

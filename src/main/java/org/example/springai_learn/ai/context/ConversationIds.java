package org.example.springai_learn.ai.context;

public final class ConversationIds {

    private ConversationIds() {
    }

    public static String forMode(String userId, ChatMode mode, String chatId) {
        String chatType = mode == ChatMode.COACH ? "coach" : "loveapp";
        return forType(userId, chatType, chatId);
    }

    public static String forType(String userId, String chatType, String chatId) {
        String safeUserId = (userId == null || userId.isBlank()) ? "anonymous" : userId;
        String safeChatType = (chatType == null || chatType.isBlank()) ? "loveapp" : chatType;
        return safeUserId + ":" + safeChatType + ":" + chatId;
    }
}

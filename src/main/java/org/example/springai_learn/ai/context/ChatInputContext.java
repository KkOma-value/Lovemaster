package org.example.springai_learn.ai.context;

public record ChatInputContext(
        String userId,
        String chatId,
        ChatMode mode,
        String userMessage,
        String imageUrl
) {
    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isBlank();
    }
}

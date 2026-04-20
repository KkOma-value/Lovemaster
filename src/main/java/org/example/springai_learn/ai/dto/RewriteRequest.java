package org.example.springai_learn.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RewriteRequest(
        @NotBlank(message = "userMessage 不能为空")
        @Size(max = 4000, message = "userMessage 过长")
        String userMessage,
        String imageUrl,
        String mode
) {
}

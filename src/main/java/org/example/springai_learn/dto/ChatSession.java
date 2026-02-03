package org.example.springai_learn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话信息DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatSession {
    private String id;
    private String title;
}

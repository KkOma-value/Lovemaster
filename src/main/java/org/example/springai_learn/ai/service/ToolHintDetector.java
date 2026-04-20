package org.example.springai_learn.ai.service;

import org.springframework.stereotype.Component;

/**
 * 启发式判断用户消息是否需要工具调用（搜索/生成/推荐等）。
 * 从旧 {@code MultimodalIntakeService} 与 {@code CoachChatOrchestrator} 内部方法抽出。
 */
@Component
public class ToolHintDetector {

    public boolean detect(String userMessage) {
        if (userMessage == null) {
            return false;
        }
        String text = userMessage.toLowerCase();
        return text.contains("计划")
                || text.contains("方案")
                || text.contains("帮我查")
                || text.contains("搜索")
                || text.contains("推荐")
                || text.contains("攻略")
                || text.contains("景点")
                || text.contains("地点")
                || text.contains("餐厅")
                || text.contains("旅游")
                || text.contains("旅行")
                || text.contains("照片")
                || text.contains("图片")
                || text.contains("看看")
                || text.contains("整理")
                || text.contains("生成")
                || text.contains("pdf")
                || text.contains("文档");
    }
}

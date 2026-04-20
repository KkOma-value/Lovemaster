package org.example.springai_learn.ai.service;

import org.springframework.stereotype.Component;

/**
 * 判断用户消息是否带"成功概率评估"意图的关键词兜底。
 * 从旧 {@code MultimodalIntakeService} 抽出，供 Love/Coach Orchestrator 在去掉 Intake 层后继续识别 Kiko 意图。
 */
@Component
public class ProbabilityKeywordDetector {

    public boolean detect(String userMessage) {
        if (userMessage == null) {
            return false;
        }
        String text = userMessage.toLowerCase();
        return text.contains("有没有戏")
                || text.contains("有戏吗")
                || text.contains("有机会")
                || text.contains("成功概率")
                || text.contains("成功率")
                || text.contains("胜算")
                || text.contains("能追到")
                || text.contains("追到吗")
                || text.contains("追得到")
                || text.contains("能不能在一起")
                || text.contains("可能性多大")
                || text.contains("可能在一起")
                || text.contains("有多大可能")
                || text.contains("几成把握")
                || text.contains("概率")
                || text.contains("有希望")
                || text.contains("还有救")
                || text.contains("分析一下");
    }
}

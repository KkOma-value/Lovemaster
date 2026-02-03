package org.example.springai_learn.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ReAct (Reasoning and Acting) 模式的代理抽象类
 * 实现了思考-行动的循环模式
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class ReActAgent extends BaseAgent {

    // 保存最新的AI思考内容，用于在无工具调用时返回
    protected String lastThinkingContent = "";

    /**
     * 处理当前状态并决定下一步行动
     * 
     * @return 是否需要执行行动，true表示需要执行，false表示不需要执行
     */
    public abstract boolean think();

    /**
     * 执行决定的行动
     * 
     * @return 行动执行结果
     */
    public abstract String act();

    /**
     * 获取最近一次思考的AI回复内容
     * 子类应在think()方法中设置这个值
     */
    public String getLastThinkingContent() {
        return lastThinkingContent;
    }

    public void setLastThinkingContent(String content) {
        this.lastThinkingContent = content;
    }

    /**
     * 执行单个步骤：思考和行动
     * 
     * @return 步骤执行结果
     */
    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                // 返回AI的实际回复内容，而非固定文本
                String content = getLastThinkingContent();
                if (content != null && !content.isEmpty()) {
                    return content;
                }
                return "思考完成 - 无需行动";
            }
            return act();
        } catch (Exception e) {
            // 记录异常日志
            e.printStackTrace();
            return "步骤执行失败: " + e.getMessage();
        }
    }
}

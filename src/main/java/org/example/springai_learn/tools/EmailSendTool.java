package org.example.springai_learn.tools;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 邮件发送工具 - 供 AI Agent 调用
 * 仅支持纯文本邮件，配置统一由 MailConfig 管理
 */
@Slf4j
@Component
public class EmailSendTool {

    @Resource
    private JavaMailSender mailSender;

    // 保留原有默认值以保持向后兼容
    @Value("${spring.mail.username:826285065@qq.com}")
    private String fromEmail;

    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;

    /**
     * 发送纯文本邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return true 发送成功，false 发送失败
     */
    @Tool(description = "Send email to specified recipient")
    public boolean sendEmail(
            @ToolParam(description = "Recipient email address") String to,
            @ToolParam(description = "Email subject") String subject,
            @ToolParam(description = "Email content") String content) {

        // 1. 参数校验
        if (!isValidEmail(to)) {
            log.warn("[EmailTool] Invalid recipient email: {}", to);
            return false;
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            log.error("[EmailTool] From email not configured (spring.mail.username)");
            return false;
        }

        // 2. 构建邮件消息
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to.trim());
        message.setSubject(subject != null ? subject.trim() : "(No Subject)");
        message.setText(content != null ? content.trim() : "");

        // 3. 发送邮件（带重试机制）
        return sendWithRetry(message);
    }

    /**
     * 带重试机制的邮件发送
     */
    private boolean sendWithRetry(SimpleMailMessage message) {
        int attempts = 0;
        Exception lastException = null;
        String recipient = message.getTo() != null && message.getTo().length > 0 ? message.getTo()[0] : "unknown";

        while (attempts < MAX_RETRIES) {
            try {
                if (mailSender == null) {
                    log.error("[EmailTool] Mail sender not initialized");
                    return false;
                }

                mailSender.send(message);
                log.info("[EmailTool] Email sent successfully to: {}", recipient);
                return true;

            } catch (MailAuthenticationException e) {
                // 认证错误不重试
                log.error("[EmailTool] Authentication failed: {}", e.getMessage());
                return false;
            } catch (MailSendException e) {
                attempts++;
                lastException = e;
                log.warn("[EmailTool] Send failed (attempt {}/{}): {}", attempts, MAX_RETRIES, e.getMessage());

                if (attempts < MAX_RETRIES) {
                    sleepBeforeRetry();
                }
            } catch (Exception e) {
                attempts++;
                lastException = e;
                log.warn("[EmailTool] Unexpected error (attempt {}/{}): {} - {}",
                        attempts, MAX_RETRIES, e.getClass().getSimpleName(), e.getMessage());

                if (attempts < MAX_RETRIES) {
                    sleepBeforeRetry();
                }
            }
        }

        if (lastException != null) {
            log.error("[EmailTool] All {} attempts failed. Last error: {}", MAX_RETRIES, lastException.getMessage());
        }
        return false;
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 邮箱格式校验
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.trim().matches(emailRegex);
    }
}

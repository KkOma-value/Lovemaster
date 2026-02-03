package org.example.springai_learn.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;


/**
 * 邮件发送配置类 - 单一配置源
 * 所有邮件相关配置统一在此处管理，避免重复配置
 */
@Configuration
public class MailConfig {

    @Value("${spring.mail.host:smtp.qq.com}")
    private String host;

    @Value("${spring.mail.port:465}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean auth;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable:true}")
    private boolean sslEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private boolean starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout:15000}")
    private String connectionTimeout;

    @Value("${spring.mail.properties.mail.smtp.timeout:15000}")
    private String timeout;

    @Value("${spring.mail.properties.mail.smtp.writetimeout:15000}")
    private String writeTimeout;

    @Value("${spring.mail.properties.mail.smtp.debug:false}")
    private boolean debug;

    /**
     * 配置 JavaMailSender（单一配置源）
     * 支持 QQ 邮箱 SMTP SSL 连接（默认 465 端口）
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        // 认证
        props.put("mail.smtp.auth", String.valueOf(auth));
        // SSL 和 STARTTLS 配置（二选一）
        // QQ 邮箱 465 端口需要 SSL；Gmail 587 端口需要 STARTTLS
        if (sslEnable) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port));
            props.put("mail.smtp.starttls.enable", "false");
        } else if (starttlsEnable) {
            props.put("mail.smtp.ssl.enable", "false");
            props.put("mail.smtp.starttls.enable", "true");
        } else {
            props.put("mail.smtp.ssl.enable", "false");
            props.put("mail.smtp.starttls.enable", "false");
        }
        // 超时配置
        props.put("mail.smtp.connectiontimeout", connectionTimeout);
        props.put("mail.smtp.timeout", timeout);
        props.put("mail.smtp.writetimeout", writeTimeout);
        // 调试开关（默认关闭，避免日志噪音）
        props.put("mail.debug", String.valueOf(debug));

        return mailSender;
    }

    /**
     * 获取发件人邮箱地址（供其他组件使用）
     */
    public String getFromEmail() {
        return username;
    }
} 
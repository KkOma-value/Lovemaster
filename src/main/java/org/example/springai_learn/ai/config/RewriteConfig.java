package org.example.springai_learn.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RewriteProperties.class)
public class RewriteConfig {
}

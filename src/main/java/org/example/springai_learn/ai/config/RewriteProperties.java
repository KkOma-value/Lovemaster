package org.example.springai_learn.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rewrite")
public record RewriteProperties(
        boolean legacyMode,
        RateLimit rateLimit
) {

    public RewriteProperties {
        if (rateLimit == null) {
            rateLimit = new RateLimit(5, 20);
        }
    }

    public record RateLimit(int perUserPerMinute, int perIpPerMinute) {
    }
}

package org.example.springai_learn.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class KnowledgeCacheConfig {

    @Bean(name = "knowledgeRetrievalCache")
    public Cache<String, String> knowledgeRetrievalCache(KnowledgeProperties properties) {
        KnowledgeProperties.Cache cacheProps = properties.getCache();
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(Math.max(1, cacheProps.getTtlSeconds())))
                .maximumSize(Math.max(1L, cacheProps.getMaximumSize()))
                .recordStats()
                .build();
    }
}

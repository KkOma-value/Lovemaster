package org.example.springai_learn.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * VectorStore Bean 配置。
 * 文档加载由 LoveAppVectorStoreLoader 在启动后异步完成。
 */
@Configuration
public class LoveAppVectorStoreConfig {

    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        return SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
    }
}

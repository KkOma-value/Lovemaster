package org.example.springai_learn.ai.service;

import org.example.springai_learn.config.KnowledgeProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopicSchemaServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void classify_shouldUseConfiguredSchema() throws IOException {
        Path schemaPath = tempDir.resolve("topic-schema.yml");
        Files.writeString(schemaPath, """
                version: v2-test
                stages:
                  - 破冰期
                  - 冷淡期
                intents:
                  - 聊天
                  - 邀约
                problems:
                  - 不回复
                  - 冷淡
                """, StandardCharsets.UTF_8);

        KnowledgeProperties properties = new KnowledgeProperties();
        properties.getSink().setTopicSchemaPath(schemaPath.toString());

        TopicSchemaService service = new TopicSchemaService(properties);
        TopicSchemaService.TopicClassification classification =
                service.classify("现在在冷淡期还能继续邀约吗", "她最近表现很冷淡");

        assertEquals("冷淡期", classification.stage());
        assertEquals("邀约", classification.intent());
        assertEquals("冷淡", classification.problem());
        assertEquals("v2-test", classification.schemaVersion());
        assertFalse(classification.unknownTopic());
    }

    @Test
    void classify_shouldFallbackToUnknown_whenNoKeywordMatched() throws IOException {
        Path schemaPath = tempDir.resolve("topic-schema.yml");
        Files.writeString(schemaPath, """
                version: v2-test
                stages:
                  - 破冰期
                intents:
                  - 聊天
                problems:
                  - 不回复
                """, StandardCharsets.UTF_8);

        KnowledgeProperties properties = new KnowledgeProperties();
        properties.getSink().setTopicSchemaPath(schemaPath.toString());
        properties.getSink().setUnknownLabel("unknown");

        TopicSchemaService service = new TopicSchemaService(properties);
        TopicSchemaService.TopicClassification classification =
                service.classify("这是一个泛化问题", "这是一个泛化回答");

        assertEquals("unknown", classification.stage());
        assertEquals("unknown", classification.intent());
        assertEquals("unknown", classification.problem());
        assertTrue(classification.unknownTopic());
    }
}

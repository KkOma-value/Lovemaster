package org.example.springai_learn.ai.service;

import org.example.springai_learn.config.KnowledgeProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WikiKnowledgeServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void retrieveKnowledge_shouldReturnMatchedWikiContent() throws IOException {
        Path wikiRoot = prepareWikiFiles();
        KnowledgeProperties properties = defaultProperties(wikiRoot);

        WikiKnowledgeService service = new WikiKnowledgeService(properties);
        WikiKnowledgeService.WikiKnowledgeResult result = service.retrieveKnowledge("第一次约会去哪里更轻松");

        assertTrue(result.hasContent());
        assertTrue(result.content().contains("第一次约会建议"));
        service.shutdown();
    }

    @Test
    void retrieveKnowledge_shouldExpandByWikiLink() throws IOException {
        Path wikiRoot = prepareWikiFiles();
        KnowledgeProperties properties = defaultProperties(wikiRoot);
        properties.getWiki().getRetrieval().setTopN(2);

        WikiKnowledgeService service = new WikiKnowledgeService(properties);
        WikiKnowledgeService.WikiKnowledgeResult result = service.retrieveKnowledge("第一次约会建议");

        assertTrue(result.content().contains("第一次约会建议"));
        assertTrue(result.content().contains("咖啡馆场景"));
        service.shutdown();
    }

    @Test
    void retrieveKnowledge_shouldReturnEmptyWhenDisabled() {
        KnowledgeProperties properties = new KnowledgeProperties();
        properties.getWiki().setEnabled(false);

        WikiKnowledgeService service = new WikiKnowledgeService(properties);
        WikiKnowledgeService.WikiKnowledgeResult result = service.retrieveKnowledge("任意问题");

        assertEquals("", result.content());
        service.shutdown();
    }

    private KnowledgeProperties defaultProperties(Path wikiRoot) {
        KnowledgeProperties properties = new KnowledgeProperties();
        properties.getWiki().setRoot(wikiRoot.toString());
        properties.getWiki().setHotReload(false);
        properties.getWiki().getRetrieval().setTopN(3);
        properties.getWiki().getRetrieval().setMaxCharsPerPage(300);
        properties.getWiki().getRetrieval().setTotalBudgetChars(1200);
        properties.getWiki().getGraph().setExpandHops(1);
        properties.getWiki().getGraph().setExpansionWeight(0.5);
        return properties;
    }

    private Path prepareWikiFiles() throws IOException {
        Path wikiRoot = tempDir.resolve("wiki");
        Path conceptsDir = wikiRoot.resolve("concepts");
        Path entitiesDir = wikiRoot.resolve("entities");
        Files.createDirectories(conceptsDir);
        Files.createDirectories(entitiesDir);

        Files.writeString(
                conceptsDir.resolve("first-date.md"),
                """
                        ---
                        title: 第一次约会建议
                        ---
                        第一次约会可以选择安静环境，方便建立舒适感与安全感。[[entities/coffee-shop]]
                        """,
                StandardCharsets.UTF_8
        );

        Files.writeString(
                entitiesDir.resolve("coffee-shop.md"),
                """
                        ---
                        title: 咖啡馆场景
                        ---
                        咖啡馆适合轻松聊天，也便于观察对方边界感和节奏感。
                        """,
                StandardCharsets.UTF_8
        );
        return wikiRoot;
    }
}
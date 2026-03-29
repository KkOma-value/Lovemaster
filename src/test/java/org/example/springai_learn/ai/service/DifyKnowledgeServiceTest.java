package org.example.springai_learn.ai.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.example.springai_learn.config.DifyClientConfig;
import org.example.springai_learn.config.DifyProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DifyKnowledgeServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void retrieveKnowledge_shouldSendBearerHeaderAndFormatSegments() throws Exception {
        AtomicReference<String> authHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/datasets/test-dataset/retrieve", exchange -> {
            authHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, """
                    {
                      "query": { "content": "测试查询" },
                      "records": [
                        { "segment": { "content": "第一段知识" }, "score": 0.91 },
                        { "segment": { "content": "第二段知识" }, "score": 0.88 }
                      ]
                    }
                    """);
        });
        server.start();

        DifyKnowledgeService service = new DifyKnowledgeService(newRestClient(server), configuredProperties(server));

        String result = service.retrieveKnowledge("测试查询");

        assertEquals("第一段知识\n---\n第二段知识", result);
        assertEquals("Bearer dataset-test-key", authHeader.get());
        assertTrue(requestBody.get().contains("\"query\":\"测试查询\""));
        assertTrue(requestBody.get().contains("\"search_method\":\"hybrid_search\""));
        assertTrue(requestBody.get().contains("\"top_k\":4"));
    }

    @Test
    void retrieveKnowledge_shouldReturnEmpty_whenConfigMissing() {
        DifyProperties properties = new DifyProperties();
        RestClient client = new DifyClientConfig().difyRestClient(properties);
        DifyKnowledgeService service = new DifyKnowledgeService(client, properties);

        String result = service.retrieveKnowledge("测试查询");

        assertEquals("", result);
    }

    @Test
    void retrieveKnowledge_shouldRetryOnServerErrorAndThenReturnEmpty() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/datasets/test-dataset/retrieve", exchange -> {
            requestCount.incrementAndGet();
            respond(exchange, 500, "{\"message\":\"server error\"}");
        });
        server.start();

        DifyProperties properties = configuredProperties(server);
        properties.getRetry().setMaxAttempts(2);
        properties.getRetry().setBackoffMs(1);
        DifyKnowledgeService service = new DifyKnowledgeService(newRestClient(server), properties);

        String result = service.retrieveKnowledge("测试查询");

        assertEquals("", result);
        assertEquals(2, requestCount.get());
    }

    private RestClient newRestClient(HttpServer httpServer) {
        DifyProperties properties = configuredProperties(httpServer);
        return new DifyClientConfig().difyRestClient(properties);
    }

    private DifyProperties configuredProperties(HttpServer httpServer) {
        DifyProperties properties = new DifyProperties();
        properties.setBaseUrl("http://localhost:" + httpServer.getAddress().getPort());
        properties.setDatasetKey("dataset-test-key");
        properties.setDatasetId("test-dataset");
        properties.getRetry().setBackoffMs(1);
        properties.getTimeout().setConnectMs(1000);
        properties.getTimeout().setReadMs(1000);
        return properties;
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        } finally {
            exchange.close();
        }
    }
}

package org.example.springai_learn.tools;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageSearchToolTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void searchImage_returnsMediumImageUrls() {
        server.createContext("/search", exchange -> {
            assertTrue(exchange.getRequestURI().getQuery().contains("query=teamlab"));
            assertTrue("test-key".equals(exchange.getRequestHeaders().getFirst("Authorization")));

            byte[] body = """
                    {"photos":[
                      {"src":{"medium":"https://images.example.com/a.jpg"}},
                      {"src":{"medium":"https://images.example.com/b.jpg"}}
                    ]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        ImageSearchTool tool = new ImageSearchTool(
                "test-key",
                5000,
                3,
                baseUrl + "/search",
                HttpClient.newHttpClient(),
                0);

        String result = tool.searchImage("teamlab");

        assertTrue(result.contains("Found 2 images"), result);
        assertTrue(result.contains("https://images.example.com/a.jpg"), result);
        assertTrue(result.contains("https://images.example.com/b.jpg"), result);
    }
}

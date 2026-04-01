package org.example.springai_learn.auth.service;

import com.sun.net.httpserver.HttpServer;
import org.example.springai_learn.auth.entity.ConversationImage;
import org.example.springai_learn.auth.repository.ConversationImageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationImageStorageServiceTest {

    private ConversationImageRepository repository;
    private ConversationImageStorageService service;
    private SupabaseStorageClient supabaseStorageClient;
    private HttpServer server;
    private String baseUrl;
    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        repository = mock(ConversationImageRepository.class);
        service = new ConversationImageStorageService(repository);
        supabaseStorageClient = mock(SupabaseStorageClient.class);

        tempDir = Files.createTempDirectory("conversation-image-storage-");
        ReflectionTestUtils.setField(service, "configuredDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "supabaseStorageClient", supabaseStorageClient);

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.stop(0);
        }
        if (tempDir != null) {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                        }
                    });
        }
    }

    @Test
    void storeRemoteImage_fallsBackToLocalDownload_whenSupabaseUploadFails() throws Exception {
        server.createContext("/img.jpg", exchange -> {
            byte[] body = "fake-jpg".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "image/jpeg");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        when(repository.findFirstByConversationIdAndSourceUrl(eq("chat-1"), eq(baseUrl + "/img.jpg")))
                .thenReturn(Optional.empty());
        when(supabaseStorageClient.isConfigured()).thenReturn(true);
        when(supabaseStorageClient.upload(any(), any(), any()))
                .thenThrow(new IllegalStateException("Bucket not found"));

        ConversationImageStorageService.StoredConversationImage storedImage =
                service.storeRemoteImage("chat-1", baseUrl + "/img.jpg", "teamlab.jpg");

        assertEquals("teamlab.jpg", storedImage.getFileName());
        assertEquals("/api/files/download/teamlab.jpg", storedImage.getPublicUrl());
        assertEquals("download/teamlab.jpg", storedImage.getStoragePath());
        assertTrue(Files.exists(tempDir.resolve("download").resolve("teamlab.jpg")));

        ArgumentCaptor<ConversationImage> imageCaptor = ArgumentCaptor.forClass(ConversationImage.class);
        verify(repository).save(imageCaptor.capture());
        assertEquals("/api/files/download/teamlab.jpg", imageCaptor.getValue().getPublicUrl());
        assertEquals("download/teamlab.jpg", imageCaptor.getValue().getStoragePath());
    }
}

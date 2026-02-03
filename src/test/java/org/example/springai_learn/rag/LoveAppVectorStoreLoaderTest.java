package org.example.springai_learn.rag;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoveAppVectorStoreLoaderTest {

    @Test
    void onApplicationReady_whenLoadModeOff_shouldNotLoad() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        LoveAppDocumentLoader docLoader = mock(LoveAppDocumentLoader.class);

        LoveAppVectorStoreLoader loader = new LoveAppVectorStoreLoader(ctx, docLoader);
        setField(loader, "loadMode", "off");

        loader.onApplicationReady();

        verify(docLoader, never()).loadMarkdowns();
        assertFalse(loader.isLoaded());
    }

    @Test
    void loadDocumentsAsync_whenSuccess_shouldSetLoadedTrue() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        LoveAppDocumentLoader docLoader = mock(LoveAppDocumentLoader.class);
        VectorStore vectorStore = mock(VectorStore.class);

        when(ctx.getBean("loveAppVectorStore", VectorStore.class)).thenReturn(vectorStore);
        when(docLoader.loadMarkdowns()).thenReturn(List.of(new Document("test content")));

        LoveAppVectorStoreLoader loader = new LoveAppVectorStoreLoader(ctx, docLoader);
        setField(loader, "loadMode", "async");
        setField(loader, "maxAttempts", 3);
        setField(loader, "backoffSeconds", 1);

        loader.loadDocumentsAsync().join();

        assertTrue(loader.isLoaded());
        verify(vectorStore, times(1)).add(anyList());
    }

    @Test
    void loadDocumentsAsync_whenNoDocuments_shouldSetLoadedTrue() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        LoveAppDocumentLoader docLoader = mock(LoveAppDocumentLoader.class);

        when(docLoader.loadMarkdowns()).thenReturn(Collections.emptyList());

        LoveAppVectorStoreLoader loader = new LoveAppVectorStoreLoader(ctx, docLoader);
        setField(loader, "loadMode", "async");
        setField(loader, "maxAttempts", 3);
        setField(loader, "backoffSeconds", 1);

        loader.loadDocumentsAsync().join();

        assertTrue(loader.isLoaded());
        verify(ctx, never()).getBean(anyString(), eq(VectorStore.class));
    }

    @Test
    void loadDocumentsAsync_whenAllAttemptsFail_shouldNotSetLoaded() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        LoveAppDocumentLoader docLoader = mock(LoveAppDocumentLoader.class);
        VectorStore vectorStore = mock(VectorStore.class);

        when(ctx.getBean("loveAppVectorStore", VectorStore.class)).thenReturn(vectorStore);
        when(docLoader.loadMarkdowns()).thenReturn(List.of(new Document("test")));
        doThrow(new RuntimeException("Embedding failed")).when(vectorStore).add(anyList());

        LoveAppVectorStoreLoader loader = new LoveAppVectorStoreLoader(ctx, docLoader);
        setField(loader, "loadMode", "async");
        setField(loader, "maxAttempts", 2);
        setField(loader, "backoffSeconds", 1);

        loader.loadDocumentsAsync().join();

        assertFalse(loader.isLoaded());
        verify(vectorStore, times(2)).add(anyList());
    }

    @Test
    void loadDocumentsAsync_whenSecondAttemptSucceeds_shouldSetLoaded() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        LoveAppDocumentLoader docLoader = mock(LoveAppDocumentLoader.class);
        VectorStore vectorStore = mock(VectorStore.class);

        when(ctx.getBean("loveAppVectorStore", VectorStore.class)).thenReturn(vectorStore);
        when(docLoader.loadMarkdowns()).thenReturn(List.of(new Document("test")));
        doThrow(new RuntimeException("Temporary failure"))
                .doNothing()
                .when(vectorStore).add(anyList());

        LoveAppVectorStoreLoader loader = new LoveAppVectorStoreLoader(ctx, docLoader);
        setField(loader, "loadMode", "async");
        setField(loader, "maxAttempts", 3);
        setField(loader, "backoffSeconds", 1);

        loader.loadDocumentsAsync().join();

        assertTrue(loader.isLoaded());
        verify(vectorStore, times(2)).add(anyList());
    }

    @Test
    void triggerRetryIfNotLoaded_whenOff_shouldSkip() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        LoveAppDocumentLoader docLoader = mock(LoveAppDocumentLoader.class);

        LoveAppVectorStoreLoader loader = Mockito.spy(new LoveAppVectorStoreLoader(ctx, docLoader));
        setField(loader, "loadMode", "off");

        Mockito.doReturn(CompletableFuture.completedFuture(null)).when(loader).loadDocumentsAsync();

        loader.triggerRetryIfNotLoaded("test");

        Mockito.verify(loader, never()).loadDocumentsAsync();
        assertFalse(loader.isLoaded());
    }

    @Test
    void triggerRetryIfNotLoaded_whenNotLoaded_shouldInvokeOnce() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        LoveAppDocumentLoader docLoader = mock(LoveAppDocumentLoader.class);

        LoveAppVectorStoreLoader loader = Mockito.spy(new LoveAppVectorStoreLoader(ctx, docLoader));
        setField(loader, "loadMode", "async");

        Mockito.doReturn(CompletableFuture.completedFuture(null)).when(loader).loadDocumentsAsync();

        loader.triggerRetryIfNotLoaded("first");
        loader.triggerRetryIfNotLoaded("second");

        Mockito.verify(loader, times(1)).loadDocumentsAsync();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

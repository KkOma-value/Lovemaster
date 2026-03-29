package org.example.springai_learn.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RagKnowledgeServiceTest {

    private DifyKnowledgeService difyKnowledgeService;
    private RagKnowledgeService ragKnowledgeService;

    @BeforeEach
    void setUp() {
        difyKnowledgeService = mock(DifyKnowledgeService.class);
        ragKnowledgeService = new RagKnowledgeService(difyKnowledgeService);
    }

    @Test
    void retrieveKnowledge_shouldDelegateToDifyService() {
        when(difyKnowledgeService.retrieveKnowledge("测试问题")).thenReturn("知识块1\n---\n知识块2");

        String result = ragKnowledgeService.retrieveKnowledge("测试问题");

        assertEquals("知识块1\n---\n知识块2", result);
        verify(difyKnowledgeService).retrieveKnowledge("测试问题");
    }

    @Test
    void retrieveKnowledge_shouldReturnEmpty_whenDifyReturnsEmpty() {
        when(difyKnowledgeService.retrieveKnowledge("无结果问题")).thenReturn("");

        String result = ragKnowledgeService.retrieveKnowledge("无结果问题");

        assertEquals("", result);
        verify(difyKnowledgeService).retrieveKnowledge("无结果问题");
    }
}

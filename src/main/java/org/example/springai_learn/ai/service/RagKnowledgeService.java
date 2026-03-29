package org.example.springai_learn.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * RAG 知识检索服务。
 * 接收重写后的查询文本，从向量知识库中检索相关文档片段，
 * 供后续 Orchestrator 注入到系统提示词中增强回答质量。
 */
@Service
@RequiredArgsConstructor
public class RagKnowledgeService {

    private final DifyKnowledgeService difyKnowledgeService;

    /**
     * 根据查询词从恋爱知识库检索相关内容。
     *
     * @param query 查询文本，通常为 IntakeAnalysisResult.rewrittenQuestion()
     * @return 格式化的知识片段字符串，无匹配结果时返回空字符串
     */
    public String retrieveKnowledge(String query) {
        return difyKnowledgeService.retrieveKnowledge(query);
    }
}

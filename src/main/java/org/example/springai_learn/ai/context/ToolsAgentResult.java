package org.example.springai_learn.ai.context;

import java.util.List;

/**
 * ToolsAgent 执行结果 — 携带工具文本结果和已存储图片引用。
 */
public record ToolsAgentResult(
        String textResult,
        List<StoredImageRef> storedImages
) {
    public record StoredImageRef(String fileName, String publicUrl) {}

    public boolean hasImages() {
        return storedImages != null && !storedImages.isEmpty();
    }
}

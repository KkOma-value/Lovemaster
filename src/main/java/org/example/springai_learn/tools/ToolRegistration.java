package org.example.springai_learn.tools;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class ToolRegistration {

    /**
     * 一次性将所有工具提供给AI
     */

    @Value("${search-api.api-key:}")
    private String searchApiKey;

    @Value("${app.file-save-dir:}")
    private String fileSaveDir;

    @Value("${pexels.api-key:}")
    private String pexelsApiKey;

    @Resource
    private EmailSendTool emailSendTool;

    @Bean
    public ToolCallback[] allTools() {
        List<Object> tools = new ArrayList<>();

        // 基础工具（始终注册）
        tools.add(new FileOperationTool());
        tools.add(new WebScrapingTool());
        tools.add(new ResourceDownloadTool(fileSaveDir));
        tools.add(new TerminalOperationTool());
        tools.add(new PDFGenerationTool(fileSaveDir));
        tools.add(new TerminateTool());
        tools.add(emailSendTool); // 使用注入的实例，而非手动 new

        // 可选工具：仅当 api-key 配置时注册
        if (searchApiKey != null && !searchApiKey.isBlank()) {
            tools.add(new WebSearchTool(searchApiKey));
            log.info("WebSearchTool 已注册（search-api.api-key 已配置）");
        } else {
            log.warn("WebSearchTool 未注册：search-api.api-key 未配置");
        }

        // Pexels 图片搜索工具：仅当 pexels.api-key 配置时注册
        if (pexelsApiKey != null && !pexelsApiKey.isBlank()) {
            tools.add(new ImageSearchTool(pexelsApiKey));
            log.info("ImageSearchTool 已注册（pexels.api-key 已配置）");
        } else {
            log.warn("ImageSearchTool 未注册：pexels.api-key 未配置。AI将无法搜索直接图片URL。");
        }

        return ToolCallbacks.from(tools.toArray());
    }
}

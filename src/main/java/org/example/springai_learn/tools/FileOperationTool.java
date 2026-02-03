package org.example.springai_learn.tools;

import cn.hutool.core.io.FileUtil;
import org.example.springai_learn.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;


//读写工具
public class FileOperationTool {
    private final String FILE_DIR = FileConstant.FILE_SAVE_DIR + "/file";

    /** Maximum characters returned inline. Larger outputs are truncated. */
    private static final int MAX_OUTPUT_CHARS = 20_000;
    private static final String TRUNCATION_SUFFIX = "... [truncated]";

    @Tool(description = "Read content from a file")
    public String ReadFile(@ToolParam(description = "Name of the file to read") String fileName) {
        String filePath = FILE_DIR + "/" + fileName;
        try {
            String content = FileUtil.readUtf8String(filePath);
            if (content != null && content.length() > MAX_OUTPUT_CHARS) {
                return content.substring(0, MAX_OUTPUT_CHARS) + TRUNCATION_SUFFIX
                        + "\n[full content: " + content.length() + " chars at " + filePath + "]";
            }
            return content;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Write content to a file")
    public String WriteFile(
            @ToolParam(description = "Name of the file to write") String fileName,
            @ToolParam(description = "Content to write to the file") String content) {
        String filePath = FILE_DIR + "/" + fileName;
        try {
            // 创建目录
            FileUtil.mkdir(FILE_DIR);
            FileUtil.writeUtf8String(content, filePath);
            return "File written successfully to: " + filePath;
        } catch (Exception e) {
            return "Error writing to file: " + e.getMessage();
        }
    }
}

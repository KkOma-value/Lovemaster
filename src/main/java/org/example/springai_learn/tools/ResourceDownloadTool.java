package org.example.springai_learn.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import org.example.springai_learn.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class ResourceDownloadTool {

    private final String fileSaveDir;

    public ResourceDownloadTool() {
        this.fileSaveDir = FileConstant.FILE_SAVE_DIR;
    }

    public ResourceDownloadTool(String fileSaveDir) {
        this.fileSaveDir = StrUtil.isBlank(fileSaveDir) ? FileConstant.FILE_SAVE_DIR : fileSaveDir.trim();
    }

    @Tool(description = "Download a resource from a given URL")
    public String downloadResource(
            @ToolParam(description = "URL of the resource to download") String url,
            @ToolParam(description = "Name of the file to save the downloaded resource") String fileName) {
        if (StrUtil.isBlank(url)) {
            return "Error downloading resource: url is blank";
        }
        if (StrUtil.isBlank(fileName)) {
            return "Error downloading resource: fileName is blank";
        }

        String fileDir = fileSaveDir + "/download";

        // 如果文件已存在，添加时间戳避免覆盖
        String baseName = FileUtil.mainName(fileName);
        String ext = FileUtil.extName(fileName);
        String finalFileName = fileName;
        java.io.File checkFile = new java.io.File(fileDir, fileName);
        if (checkFile.exists()) {
            long timestamp = System.currentTimeMillis();
            finalFileName = baseName + "_" + timestamp + (StrUtil.isNotBlank(ext) ? "." + ext : "");
        }

        String filePath = fileDir + "/" + finalFileName;

        try {
            // 创建目录
            FileUtil.mkdir(fileDir);

            // 使用 Hutool 的 createGet 方法以便检查响应头
            var response = HttpUtil.createGet(url)
                    .timeout(30000)
                    .execute();

            int status = response.getStatus();
            String contentType = response.header("Content-Type");

            // 检查HTTP状态码
            if (status < 200 || status >= 300) {
                return "Error downloading resource: http status=" + status;
            }

            // 检查Content-Type (目前主要用于图片下载，避免保存HTML页面)
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                return "Error downloading resource: unexpected content-type=" + contentType
                        + ". Only images are supported.";
            }

            // 保存文件
            FileUtil.writeBytes(response.bodyBytes(), filePath);

            return "Resource downloaded successfully to: " + filePath + " (Content-Type: " + contentType + ")";
        } catch (Exception e) {
            return "Error downloading resource: " + e.getMessage();
        }
    }
}

package org.example.springai_learn.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import org.example.springai_learn.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class ResourceDownloadTool {

    private final String fileSaveDir;
    private final boolean persistLocally;

    public ResourceDownloadTool() {
        this(FileConstant.FILE_SAVE_DIR, true);
    }

    public ResourceDownloadTool(String fileSaveDir) {
        this(fileSaveDir, true);
    }

    public ResourceDownloadTool(String fileSaveDir, boolean persistLocally) {
        this.fileSaveDir = StrUtil.isBlank(fileSaveDir) ? FileConstant.FILE_SAVE_DIR : fileSaveDir.trim();
        this.persistLocally = persistLocally;
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

        try {
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

            byte[] body = response.bodyBytes();
            String finalFileName = fileName;

            if (persistLocally) {
                String fileDir = fileSaveDir + "/download";
                FileUtil.mkdir(fileDir);

                String baseName = FileUtil.mainName(fileName);
                String ext = FileUtil.extName(fileName);
                java.io.File checkFile = new java.io.File(fileDir, fileName);
                if (checkFile.exists()) {
                    long timestamp = System.currentTimeMillis();
                    finalFileName = baseName + "_" + timestamp + (StrUtil.isNotBlank(ext) ? "." + ext : "");
                }

                String filePath = fileDir + "/" + finalFileName;
                FileUtil.writeBytes(body, filePath);
                return "Resource downloaded successfully\n"
                        + "sourceUrl: " + url + "\n"
                        + "fileName: " + finalFileName + "\n"
                        + "contentType: " + contentType + "\n"
                        + "bytes: " + body.length + "\n"
                        + "localPath: " + filePath;
            }

            return "Resource downloaded successfully\n"
                    + "sourceUrl: " + url + "\n"
                    + "fileName: " + finalFileName + "\n"
                    + "contentType: " + contentType + "\n"
                    + "bytes: " + body.length + "\n"
                    + "storageMode: deferred-upload";
        } catch (Exception e) {
            return "Error downloading resource: " + e.getMessage();
        }
    }
}

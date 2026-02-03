package org.example.springai_learn.constant;

public interface FileConstant {

    /**
     * 文件保存目录
     */
    String FILE_SAVE_DIR = resolveFileSaveDir();

    static String resolveFileSaveDir() {
        String configured = System.getProperty("app.file-save-dir");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("APP_FILE_SAVE_DIR");
        }
        if (configured == null || configured.isBlank()) {
            return System.getProperty("user.dir") + "/tmp";
        }
        // Keep behavior simple: treat configured value as-is (absolute recommended).
        return configured.trim();
    }
}

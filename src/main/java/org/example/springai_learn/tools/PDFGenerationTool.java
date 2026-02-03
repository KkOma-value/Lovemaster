package org.example.springai_learn.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PDFGenerationTool {

    private final String fileSaveDir;

    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[[^\\]]*\\]\\(([^)]+)\\)");

    public PDFGenerationTool() {
        this.fileSaveDir = FileConstant.FILE_SAVE_DIR;
    }

    public PDFGenerationTool(String fileSaveDir) {
        this.fileSaveDir = StrUtil.isBlank(fileSaveDir) ? FileConstant.FILE_SAVE_DIR : fileSaveDir.trim();
    }

    @Tool(description = "Generate a PDF file with given content. fileName must end with .pdf and content must be non-empty text.")
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF (include .pdf)") String fileName,
            @ToolParam(description = "Content to be included in the PDF (required)") String content) {

        if (StrUtil.isBlank(fileName)) {
            return "Error generating PDF: fileName is blank";
        }
        if (!fileName.toLowerCase().endsWith(".pdf")) {
            fileName = fileName + ".pdf";
        }
        if (StrUtil.isBlank(content)) {
            return "Error generating PDF: content is empty";
        }

        SanitizeResult sanitized = sanitizeNonBmp(content);
        String safeContent = sanitized.cleaned();
        if (StrUtil.isBlank(safeContent)) {
            return "Error generating PDF: content became empty after removing unsupported characters (emoji)";
        }

        Path baseDir = Path.of(fileSaveDir);
        Path pdfDirPath = baseDir.resolve("pdf");
        Path downloadDirPath = baseDir.resolve("download");
        String fileDir = pdfDirPath.toString();
        String downloadDir = downloadDirPath.toString();

        // 如果文件已存在，添加时间戳避免覆盖
        String baseName = fileName.substring(0, fileName.length() - 4); // remove .pdf
        Path checkPath = pdfDirPath.resolve(fileName);
        if (Files.exists(checkPath)) {
            long timestamp = System.currentTimeMillis();
            fileName = baseName + "_" + timestamp + ".pdf";
        }

        Path filePath = pdfDirPath.resolve(fileName);

        // Pre-parse markdown images and validate local image files before writing
        // anything.
        List<ImageRef> imageRefs;
        try {
            imageRefs = parseMarkdownImages(safeContent, downloadDir);
        } catch (Exception e) {
            return "Error generating PDF: " + e.getMessage();
        }
        if (!imageRefs.isEmpty()) {
            List<String> missing = new ArrayList<>();
            for (ImageRef ref : imageRefs) {
                if (!Files.isRegularFile(ref.resolvedPath) || !Files.isReadable(ref.resolvedPath)) {
                    missing.add(ref.originalPath + " (expected at " + ref.resolvedPath + ")");
                }
            }
            if (!missing.isEmpty()) {
                return "Error generating PDF: missing images: " + String.join(", ", missing)
                        + ". Hint: download images to " + downloadDir + " before generating the PDF.";
            }
        }

        try {
            FileUtil.mkdir(fileDir);
            log.info("Generating PDF to {} (content length={}, sanitizedRemoved={})", filePath, safeContent.length(),
                    sanitized.removed());

            try (PdfWriter writer = new PdfWriter(filePath.toString());
                    PdfDocument pdf = new PdfDocument(writer);
                    Document document = new Document(pdf)) {
                PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
                document.setFont(font);

                // Render: minimal Markdown support (text + ![](...) images).
                Matcher matcher = MARKDOWN_IMAGE_PATTERN.matcher(safeContent);
                int cursor = 0;
                while (matcher.find()) {
                    String textPart = safeContent.substring(cursor, matcher.start());
                    if (StrUtil.isNotBlank(textPart)) {
                        document.add(new Paragraph(textPart));
                    }
                    String rawPath = matcher.group(1);
                    Path resolved = resolveDownloadPath(downloadDir, rawPath);
                    Image image = new Image(ImageDataFactory.create(resolved.toString()));
                    image.setAutoScale(true);
                    document.add(image);
                    cursor = matcher.end();
                }
                String tail = safeContent.substring(cursor);
                if (StrUtil.isNotBlank(tail)) {
                    document.add(new Paragraph(tail));
                }
            } catch (Exception e) {
                log.error("Failed writing PDF {}", filePath, e);
                return "Error generating PDF: " + e.getMessage()
                        + " (hint: ensure com.itextpdf:font-asian is available at runtime for CJK fonts)";
            }

            long size = FileUtil.size(filePath.toFile());
            if (size <= 0) {
                log.warn("PDF generated but empty: {}", filePath);
                return "Error generating PDF: empty file generated at " + filePath;
            }
            String notice = sanitized.removed() > 0
                    ? " (note: removed " + sanitized.removed() + " unsupported non-BMP characters such as emoji)"
                    : "";
            return "PDF generated successfully to: " + filePath + " (" + size + " bytes)"
                    + " (downloadDir=" + downloadDir + ", pdfDir=" + fileDir + ")" + notice;
        } catch (Exception e) {
            log.error("Error generating PDF {}", filePath, e);
            return "Error generating PDF: " + e.getMessage();
        }
    }

    private static List<ImageRef> parseMarkdownImages(String content, String downloadDir) {
        List<ImageRef> refs = new ArrayList<>();
        Matcher matcher = MARKDOWN_IMAGE_PATTERN.matcher(content);
        while (matcher.find()) {
            String raw = matcher.group(1);
            Path resolved = resolveDownloadPath(downloadDir, raw);
            refs.add(new ImageRef(raw, resolved));
        }
        return refs;
    }

    private static Path resolveDownloadPath(String downloadDir, String rawPath) {
        if (StrUtil.isBlank(rawPath)) {
            throw new IllegalArgumentException("invalid markdown image path");
        }
        // Basic sanitization: disallow traversal out of tmp/download.
        Path base = Path.of(downloadDir).toAbsolutePath().normalize();
        Path candidate;
        try {
            Path provided = Path.of(rawPath.trim());
            if (provided.isAbsolute()) {
                candidate = provided.toAbsolutePath().normalize();
            } else {
                candidate = base.resolve(provided).normalize();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid markdown image path: " + rawPath);
        }
        if (!candidate.startsWith(base)) {
            throw new IllegalArgumentException("invalid markdown image path (path traversal): " + rawPath);
        }
        return candidate;
    }

    private record ImageRef(String originalPath, Path resolvedPath) {
    }

    private static SanitizeResult sanitizeNonBmp(String input) {
        if (input == null) {
            return new SanitizeResult("", 0);
        }
        StringBuilder sb = new StringBuilder(input.length());
        int removed = 0;
        for (int i = 0; i < input.length();) {
            int codePoint = input.codePointAt(i);
            if (codePoint <= 0xFFFF) {
                sb.appendCodePoint(codePoint);
            } else {
                removed++;
            }
            i += Character.charCount(codePoint);
        }
        return new SanitizeResult(sb.toString(), removed);
    }

    private record SanitizeResult(String cleaned, int removed) {
    }
}

package org.example.springai_learn.tools;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfStream;
import org.example.springai_learn.constant.FileConstant;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PDFGenerationToolTest {

    @Test
    void generatePDF_writesProvidedContent() throws Exception {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "pdf-test-" + UUID.randomUUID() + ".pdf";
        String content = "Hello PDF 内容123";

        String result = tool.generatePDF(fileName, content);

        assertTrue(result.startsWith("PDF generated successfully"), result);
        Path pdfPath = Paths.get(FileConstant.FILE_SAVE_DIR, "pdf", fileName);
        assertTrue(Files.exists(pdfPath), "PDF file should exist");

        String extracted = extractText(pdfPath);
        assertTrue(extracted.contains(content), "PDF should contain provided content");

        Files.deleteIfExists(pdfPath);
    }

    @Test
    void generatePDF_rejectsBlankContent() throws Exception {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "pdf-empty-" + UUID.randomUUID() + ".pdf";

        String result = tool.generatePDF(fileName, "   ");

        assertTrue(result.contains("content is empty"), result);
        Path pdfPath = Paths.get(FileConstant.FILE_SAVE_DIR, "pdf", fileName);
        if (Files.exists(pdfPath)) {
            assertTrue(Files.size(pdfPath) == 0, "Empty content should not create non-empty PDF");
            Files.deleteIfExists(pdfPath);
        }
    }

    @Test
    void generatePDF_embedsLocalImagesReferencedByMarkdown() throws Exception {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "pdf-img-" + UUID.randomUUID() + ".pdf";

        Path downloadDir = Paths.get(FileConstant.FILE_SAVE_DIR, "download");
        Files.createDirectories(downloadDir);
        Path imagePath = downloadDir.resolve("gugong.jpg");

        // Create a tiny JPEG image for testing.
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "jpg", imagePath.toFile());

        String content = "### 北京三日游行程\n![](gugong.jpg)\n结束";

        String result = tool.generatePDF(fileName, content);

        assertTrue(result.startsWith("PDF generated successfully"), result);
        Path pdfPath = Paths.get(FileConstant.FILE_SAVE_DIR, "pdf", fileName);
        assertTrue(Files.exists(pdfPath), "PDF file should exist");
        assertTrue(countEmbeddedImages(pdfPath) >= 1, "PDF should contain at least one embedded image");

        Files.deleteIfExists(pdfPath);
        Files.deleteIfExists(imagePath);
    }

    @Test
    void generatePDF_missingMarkdownImageReturnsErrorAndDoesNotCreatePdf() throws Exception {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "pdf-missing-img-" + UUID.randomUUID() + ".pdf";

        Path imagePath = Paths.get(FileConstant.FILE_SAVE_DIR, "download", "missing.jpg");
        Files.deleteIfExists(imagePath);

        String content = "Hello\n![](missing.jpg)\nWorld";
        String result = tool.generatePDF(fileName, content);

        assertTrue(result.contains("missing images"), result);
        assertTrue(result.contains("missing.jpg"), result);
        assertTrue(result.contains(imagePath.toAbsolutePath().normalize().toString()), result);

        Path pdfPath = Paths.get(FileConstant.FILE_SAVE_DIR, "pdf", fileName);
        assertFalse(Files.exists(pdfPath), "PDF should not be created when images are missing");
    }

    private String extractText(Path pdfPath) throws Exception {
        try (PdfReader reader = new PdfReader(pdfPath.toString());
             PdfDocument pdfDocument = new PdfDocument(reader)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= pdfDocument.getNumberOfPages(); i++) {
                sb.append(PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i)));
            }
            return sb.toString();
        }
    }

    private int countEmbeddedImages(Path pdfPath) throws Exception {
        try (PdfReader reader = new PdfReader(pdfPath.toString());
             PdfDocument pdfDocument = new PdfDocument(reader)) {
            int count = 0;
            for (int i = 1; i <= pdfDocument.getNumberOfPages(); i++) {
                var resources = pdfDocument.getPage(i).getResources();
                var xObjectDict = resources.getResource(PdfName.XObject);
                if (xObjectDict == null) {
                    continue;
                }
                for (PdfName key : xObjectDict.keySet()) {
                    PdfObject obj = xObjectDict.get(key);
                    if (obj instanceof PdfStream stream) {
                        if (PdfName.Image.equals(stream.getAsName(PdfName.Subtype))) {
                            count++;
                        }
                    }
                }
            }
            return count;
        }
    }
}

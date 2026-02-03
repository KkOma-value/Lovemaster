package org.example.springai_learn.tools;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfStream;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PDFGenerationToolConfiguredDirTest {

    @Test
    void generatePDF_usesConfiguredFileSaveDirForPdfAndDownload() throws Exception {
        Path baseDir = Files.createTempDirectory("app-file-save-").toAbsolutePath().normalize();
        Path downloadDir = baseDir.resolve("download");
        Path pdfDir = baseDir.resolve("pdf");
        Files.createDirectories(downloadDir);

        Path imagePath = downloadDir.resolve("gugong.jpg");
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "jpg", imagePath.toFile());

        PDFGenerationTool tool = new PDFGenerationTool(baseDir.toString());
        String fileName = "configured-dir.pdf";
        String content = "Hello\n![](gugong.jpg)\nWorld";

        String result = tool.generatePDF(fileName, content);
        assertTrue(result.startsWith("PDF generated successfully"), result);
        assertTrue(result.contains(downloadDir.toString()), result);
        assertTrue(result.contains(pdfDir.toString()), result);

        Path pdfPath = pdfDir.resolve(fileName);
        assertTrue(Files.exists(pdfPath), "PDF file should exist: " + pdfPath);
        assertTrue(countEmbeddedImages(pdfPath) >= 1, "PDF should contain at least one embedded image");

        Files.deleteIfExists(pdfPath);
        Files.deleteIfExists(imagePath);
        Files.deleteIfExists(downloadDir);
        Files.deleteIfExists(pdfDir);
        Files.deleteIfExists(baseDir);
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

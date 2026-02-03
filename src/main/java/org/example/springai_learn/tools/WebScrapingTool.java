package org.example.springai_learn.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class WebScrapingTool {

    private static final int MAX_TEXT_CHARS = 20_000;
    private static final int MAX_LINKS = 5;

    @Tool(description = "Scrape the content of a web page")
    public String scrapeWebPage(
            @ToolParam(description = "URL of the web page to scrape") String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            String title = doc.title();
            String bodyText = doc.body() == null ? "" : doc.body().text();
            boolean truncated = bodyText.length() > MAX_TEXT_CHARS;
            String clipped = truncated ? bodyText.substring(0, MAX_TEXT_CHARS) + "... [truncated]" : bodyText;

            List<String> sampleLinks = doc.select("a[href]").stream()
                    .limit(MAX_LINKS)
                    .map(el -> formatLink(el))
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());

            StringBuilder sb = new StringBuilder();
            sb.append("title: ").append(title).append('\n');
            sb.append("url: ").append(url).append('\n');
            sb.append("text: ").append(clipped);
            if (truncated) {
                sb.append("\n[content truncated to ").append(MAX_TEXT_CHARS).append(" chars]");
            }
            if (!sampleLinks.isEmpty()) {
                sb.append("\nsample_links: ").append(String.join(", ", sampleLinks));
            }
            return sb.toString();
        } catch (IOException e) {
            return "Error scraping web page: " + e.getMessage();
        }
    }

    private String formatLink(Element element) {
        try {
            String href = element.absUrl("href");
            if (href == null || href.isBlank()) {
                return "";
            }
            String text = element.text();
            if (text != null && text.length() > 80) {
                text = text.substring(0, 80) + "...";
            }
            return text == null || text.isBlank() ? href : text + " -> " + href;
        } catch (Exception ignored) {
            return "";
        }
    }
}

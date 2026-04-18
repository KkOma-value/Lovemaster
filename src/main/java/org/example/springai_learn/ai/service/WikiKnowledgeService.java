package org.example.springai_learn.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springai_learn.config.KnowledgeProperties;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class WikiKnowledgeService {

    private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile("(?s)^---\\n(.*?)\\n---\\n(.*)$");
    private static final Pattern TITLE_PATTERN = Pattern.compile("(?m)^title\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEADING_PATTERN = Pattern.compile("(?m)^#\\s+(.+)$");
    private static final Pattern WIKI_LINK_PATTERN = Pattern.compile("\\[\\[([^\\]|]+)(?:\\|[^\\]]+)?\\]\\]");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);

    private static final Set<String> ENGLISH_STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", "he",
            "in", "is", "it", "its", "of", "on", "that", "the", "to", "was", "were", "will", "with",
            "i", "you", "we", "they", "this", "those", "these", "or", "if", "but", "not", "can"
    );

    private final KnowledgeProperties properties;

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean watcherStarted = new AtomicBoolean(false);
    private final Map<WatchKey, Path> watchKeyToDir = new ConcurrentHashMap<>();
    private volatile WikiIndex currentIndex = WikiIndex.empty();
    private volatile WatchService watchService;
    private volatile ExecutorService watchExecutor;

    public WikiKnowledgeResult retrieveKnowledge(String query) {
        if (!properties.getWiki().isEnabled()) {
            return WikiKnowledgeResult.empty();
        }
        if (query == null || query.isBlank()) {
            return WikiKnowledgeResult.empty();
        }

        ensureInitialized();
        WikiIndex indexSnapshot = currentIndex;
        if (indexSnapshot.isEmpty()) {
            return WikiKnowledgeResult.empty();
        }

        long startNanos = System.nanoTime();
        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return WikiKnowledgeResult.empty();
        }

        Map<String, Double> scores = calculateBaseScores(indexSnapshot, queryTokens);
        if (scores.isEmpty()) {
            return WikiKnowledgeResult.empty();
        }

        expandByGraph(indexSnapshot, scores);
        List<Map.Entry<String, Double>> ranked = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .toList();

        if (ranked.isEmpty()) {
            return WikiKnowledgeResult.empty();
        }

        String formatted = buildContext(indexSnapshot, ranked);
        if (formatted.isBlank()) {
            return WikiKnowledgeResult.empty();
        }

        double topScore = normalizeTopScore(ranked.get(0).getValue(), queryTokens.size());
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        log.debug("Wiki retrieval completed in {} ms, hits={}, topScore={}", elapsedMs, ranked.size(), topScore);
        return new WikiKnowledgeResult(formatted, topScore, ranked.size());
    }

    private void ensureInitialized() {
        if (initialized.compareAndSet(false, true)) {
            rebuildIndex("startup");
        }
        if (properties.getWiki().isHotReload() && !watcherStarted.get()) {
            startWatcherIfPossible();
        }
    }

    private Map<String, Double> calculateBaseScores(WikiIndex index, List<String> queryTokens) {
        Map<String, Double> scores = new HashMap<>();
        for (String token : queryTokens) {
            List<Posting> postings = index.invertedIndex().get(token);
            if (postings == null || postings.isEmpty()) {
                continue;
            }
            double idf = 1.0 + Math.log((index.docCount() + 1.0) / (postings.size() + 1.0));
            for (Posting posting : postings) {
                scores.merge(posting.pageId(), posting.weight() * idf, Double::sum);
            }
        }
        return scores;
    }

    private void expandByGraph(WikiIndex index, Map<String, Double> scores) {
        int maxHops = Math.max(0, properties.getWiki().getGraph().getExpandHops());
        if (maxHops <= 0 || scores.isEmpty()) {
            return;
        }
        double expansionWeight = Math.max(0.0, properties.getWiki().getGraph().getExpansionWeight());
        if (expansionWeight <= 0.0) {
            return;
        }

        Map<String, Double> frontier = new HashMap<>(scores);
        for (int hop = 0; hop < maxHops; hop++) {
            Map<String, Double> next = new HashMap<>();
            for (Map.Entry<String, Double> entry : frontier.entrySet()) {
                Set<String> neighbors = index.outLinks().getOrDefault(entry.getKey(), Set.of());
                for (String neighbor : neighbors) {
                    double expanded = entry.getValue() * expansionWeight;
                    scores.merge(neighbor, expanded, Math::max);
                    next.merge(neighbor, expanded, Math::max);
                }
            }
            if (next.isEmpty()) {
                return;
            }
            frontier = next;
        }
    }

    private String buildContext(WikiIndex index, List<Map.Entry<String, Double>> ranked) {
        int topN = Math.max(1, properties.getWiki().getRetrieval().getTopN());
        int maxCharsPerPage = Math.max(80, properties.getWiki().getRetrieval().getMaxCharsPerPage());
        int totalBudget = Math.max(maxCharsPerPage, properties.getWiki().getRetrieval().getTotalBudgetChars());

        List<String> segments = new ArrayList<>();
        int used = 0;

        for (Map.Entry<String, Double> entry : ranked) {
            if (segments.size() >= topN || used >= totalBudget) {
                break;
            }
            WikiPage page = index.pages().get(entry.getKey());
            if (page == null) {
                continue;
            }
            String snippet = normalizeWhitespace(page.content());
            if (snippet.isBlank()) {
                continue;
            }
            snippet = truncate(snippet, maxCharsPerPage);
            String segment = page.title() + "\n" + snippet;

            int remaining = totalBudget - used;
            if (segment.length() > remaining) {
                segment = truncate(segment, remaining);
            }
            if (segment.isBlank()) {
                continue;
            }

            segments.add(segment);
            used += segment.length();
        }

        return String.join("\n---\n", segments);
    }

    private void rebuildIndex(String reason) {
        Path root = resolveRootPath();
        if (!Files.isDirectory(root)) {
            currentIndex = WikiIndex.empty();
            log.info("Wiki index skipped because directory does not exist: {}", root);
            return;
        }

        try {
            List<Path> markdownFiles = listMarkdownFiles(root);
            if (markdownFiles.isEmpty()) {
                currentIndex = WikiIndex.empty();
                log.info("Wiki index rebuilt with 0 pages, reason={}", reason);
                return;
            }

            double titleBoost = Math.max(0.0, properties.getWiki().getRetrieval().getTitleBoost());
            List<ParsedPage> parsedPages = new ArrayList<>(markdownFiles.size());
            for (Path file : markdownFiles) {
                parsedPages.add(parsePage(root, file, titleBoost));
            }

            Map<String, ParsedPage> parsedById = parsedPages.stream()
                    .collect(Collectors.toMap(ParsedPage::id, page -> page, (left, right) -> left, LinkedHashMap::new));

            Map<String, String> lookup = buildLookup(parsedPages);
            Map<String, Set<String>> outLinks = buildResolvedOutLinks(parsedPages, parsedById.keySet(), lookup);

            Map<String, WikiPage> pages = new LinkedHashMap<>();
            Map<String, List<Posting>> inverted = new HashMap<>();

            for (ParsedPage parsed : parsedPages) {
                Set<String> resolvedLinks = outLinks.getOrDefault(parsed.id(), Set.of());
                pages.put(parsed.id(), new WikiPage(parsed.id(), parsed.title(), parsed.content(), resolvedLinks));

                for (Map.Entry<String, Double> tokenWeight : parsed.tokenWeights().entrySet()) {
                    inverted.computeIfAbsent(tokenWeight.getKey(), ignored -> new ArrayList<>())
                            .add(new Posting(parsed.id(), tokenWeight.getValue()));
                }
            }

            currentIndex = new WikiIndex(pages, inverted, outLinks, pages.size());
            log.info("Wiki index rebuilt, pages={}, tokens={}, reason={}", pages.size(), inverted.size(), reason);
        } catch (Exception e) {
            log.warn("Wiki index rebuild failed, keep previous index: {}", e.getMessage(), e);
        }
    }

    private Map<String, String> buildLookup(List<ParsedPage> pages) {
        Map<String, String> lookup = new HashMap<>();
        for (ParsedPage page : pages) {
            lookup.putIfAbsent(normalizeIdentifier(page.id()), page.id());
            lookup.putIfAbsent(normalizeIdentifier(page.title()), page.id());

            int idx = page.id().lastIndexOf('/');
            String basename = idx >= 0 ? page.id().substring(idx + 1) : page.id();
            lookup.putIfAbsent(normalizeIdentifier(basename), page.id());
        }
        return lookup;
    }

    private Map<String, Set<String>> buildResolvedOutLinks(List<ParsedPage> pages, Set<String> allPageIds, Map<String, String> lookup) {
        Map<String, Set<String>> resolved = new HashMap<>();
        for (ParsedPage page : pages) {
            Set<String> targets = new HashSet<>();
            for (String rawLink : page.rawLinks()) {
                String linkKey = normalizeIdentifier(rawLink);
                String targetId = lookup.get(linkKey);
                if (targetId != null && allPageIds.contains(targetId) && !Objects.equals(targetId, page.id())) {
                    targets.add(targetId);
                }
            }
            resolved.put(page.id(), Collections.unmodifiableSet(targets));
        }
        return resolved;
    }

    private ParsedPage parsePage(Path root, Path file, double titleBoost) throws IOException {
        String raw = Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
        FrontMatterSplit split = splitFrontMatter(raw);

        String title = parseTitle(split.frontMatter(), split.body(), file);
        Set<String> rawLinks = extractWikiLinks(split.body());
        String body = normalizeContent(split.body());

        Map<String, Double> tokenWeights = new HashMap<>();
        tokenize(body).forEach(token -> tokenWeights.merge(token, 1.0, Double::sum));
        tokenize(title).forEach(token -> tokenWeights.merge(token, titleBoost, Double::sum));

        return new ParsedPage(toPageId(root, file), title, body, rawLinks, tokenWeights);
    }

    private FrontMatterSplit splitFrontMatter(String text) {
        Matcher matcher = FRONT_MATTER_PATTERN.matcher(text);
        if (!matcher.find()) {
            return new FrontMatterSplit("", text);
        }
        return new FrontMatterSplit(matcher.group(1), matcher.group(2));
    }

    private String parseTitle(String frontMatter, String body, Path file) {
        Matcher frontMatterTitle = TITLE_PATTERN.matcher(frontMatter);
        if (frontMatterTitle.find()) {
            return normalizeWhitespace(frontMatterTitle.group(1));
        }

        Matcher heading = HEADING_PATTERN.matcher(body);
        if (heading.find()) {
            return normalizeWhitespace(heading.group(1));
        }

        String filename = file.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private Set<String> extractWikiLinks(String body) {
        Matcher matcher = WIKI_LINK_PATTERN.matcher(body);
        Set<String> links = new HashSet<>();
        while (matcher.find()) {
            links.add(matcher.group(1));
        }
        return links;
    }

    private String normalizeContent(String body) {
        return WIKI_LINK_PATTERN.matcher(body).replaceAll("$1");
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        String lower = text.toLowerCase(Locale.ROOT);
        Matcher matcher = TOKEN_PATTERN.matcher(lower);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() < 2) {
                continue;
            }
            if (containsCjk(token)) {
                tokens.addAll(toCjkBigrams(token));
                continue;
            }
            if (!ENGLISH_STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private List<String> toCjkBigrams(String token) {
        String compact = token.replaceAll("\\s+", "");
        if (compact.length() < 2) {
            return List.of(compact);
        }

        List<String> bigrams = new ArrayList<>(Math.max(1, compact.length() - 1));
        for (int i = 0; i < compact.length() - 1; i++) {
            bigrams.add(compact.substring(i, i + 2));
        }
        return bigrams;
    }

    private boolean containsCjk(String text) {
        for (int i = 0; i < text.length(); i++) {
            Character.UnicodeScript script = Character.UnicodeScript.of(text.charAt(i));
            if (script == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    private Path resolveRootPath() {
        Path path = Path.of(properties.getWiki().getRoot());
        if (!path.isAbsolute()) {
            path = Path.of(System.getProperty("user.dir")).resolve(path);
        }
        return path.normalize();
    }

    private List<Path> listMarkdownFiles(Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".md"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    private String toPageId(Path root, Path file) {
        String relative = root.relativize(file).toString().replace('\\', '/');
        int dot = relative.lastIndexOf('.');
        if (dot > 0) {
            relative = relative.substring(0, dot);
        }
        return normalizeIdentifier(relative);
    }

    private String normalizeIdentifier(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('\\', '/');
        if (normalized.endsWith(".md")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        return normalized.replaceAll("\\s+", "-");
    }

    private String normalizeWhitespace(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private String truncate(String text, int maxChars) {
        if (text == null || maxChars <= 0) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        if (maxChars <= 3) {
            return text.substring(0, maxChars);
        }
        return text.substring(0, maxChars - 3) + "...";
    }

    private double normalizeTopScore(double rawScore, int queryTokenCount) {
        double denominator = Math.max(1.0, queryTokenCount * Math.max(1.0, properties.getWiki().getRetrieval().getTitleBoost()));
        return Math.min(1.0, rawScore / denominator);
    }

    private void startWatcherIfPossible() {
        if (!watcherStarted.compareAndSet(false, true)) {
            return;
        }
        Path root = resolveRootPath();
        if (!Files.isDirectory(root)) {
            watcherStarted.set(false);
            return;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();
            registerDirectoryRecursively(root);
            watchExecutor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "wiki-file-watcher");
                thread.setDaemon(true);
                return thread;
            });
            watchExecutor.submit(() -> watchLoop(root));
        } catch (Exception e) {
            watcherStarted.set(false);
            closeWatchServiceQuietly();
            log.warn("Failed to start wiki file watcher: {}", e.getMessage(), e);
        }
    }

    private void watchLoop(Path root) {
        long lastChangeAt = -1L;
        int debounceMs = Math.max(0, properties.getWiki().getHotReloadDebounceMs());

        while (!Thread.currentThread().isInterrupted()) {
            try {
                WatchKey key = watchService.poll(300, TimeUnit.MILLISECONDS);
                long now = System.currentTimeMillis();
                if (key != null) {
                    Path parent = watchKeyToDir.get(key);
                    if (parent != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                                lastChangeAt = now;
                                continue;
                            }

                            @SuppressWarnings("unchecked")
                            WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                            Path changed = parent.resolve(pathEvent.context());

                            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(changed)) {
                                registerDirectoryRecursively(changed);
                            }

                            if (changed.toString().toLowerCase(Locale.ROOT).endsWith(".md")) {
                                lastChangeAt = now;
                            }
                        }
                    }
                    if (!key.reset()) {
                        watchKeyToDir.remove(key);
                    }
                }

                if (lastChangeAt > 0 && now - lastChangeAt >= debounceMs) {
                    rebuildIndex("watcher");
                    lastChangeAt = -1L;
                    if (!Files.isDirectory(root)) {
                        return;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("Wiki watcher loop error: {}", e.getMessage(), e);
            }
        }
    }

    private void registerDirectoryRecursively(Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path directory : stream.filter(Files::isDirectory).toList()) {
                WatchKey watchKey = directory.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY
                );
                watchKeyToDir.put(watchKey, directory);
            }
        }
    }

    @PreDestroy
    void shutdown() {
        if (watchExecutor != null) {
            watchExecutor.shutdownNow();
        }
        closeWatchServiceQuietly();
        watchKeyToDir.clear();
        watcherStarted.set(false);
    }

    private void closeWatchServiceQuietly() {
        if (watchService == null) {
            return;
        }
        try {
            watchService.close();
        } catch (IOException ignored) {
            // ignore close exception
        } finally {
            watchService = null;
        }
    }

    public record WikiKnowledgeResult(String content, double topScore, int hitCount) {
        public static WikiKnowledgeResult empty() {
            return new WikiKnowledgeResult("", 0.0, 0);
        }

        public boolean hasContent() {
            return content != null && !content.isBlank();
        }
    }

    private record FrontMatterSplit(String frontMatter, String body) {
    }

    private record ParsedPage(
            String id,
            String title,
            String content,
            Set<String> rawLinks,
            Map<String, Double> tokenWeights
    ) {
    }

    private record Posting(String pageId, double weight) {
    }

    private record WikiPage(String id, String title, String content, Set<String> outLinks) {
    }

    private record WikiIndex(
            Map<String, WikiPage> pages,
            Map<String, List<Posting>> invertedIndex,
            Map<String, Set<String>> outLinks,
            int docCount
    ) {
        static WikiIndex empty() {
            return new WikiIndex(Map.of(), Map.of(), Map.of(), 0);
        }

        boolean isEmpty() {
            return pages.isEmpty();
        }
    }
}
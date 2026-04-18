package org.example.springai_learn.ai.service;

import java.util.HashSet;
import java.util.Set;

public final class KnowledgeSimilarityUtils {

    private KnowledgeSimilarityUtils() {
    }

    public static Set<String> shingles(String text) {
        Set<String> result = new HashSet<>();
        if (text == null) {
            return result;
        }
        String normalized = text.trim();
        if (normalized.isEmpty()) {
            return result;
        }
        int length = normalized.length();
        if (length == 1) {
            result.add(normalized);
            return result;
        }
        for (int i = 0; i < length - 1; i++) {
            String bigram = normalized.substring(i, i + 2);
            if (!bigram.isBlank()) {
                result.add(bigram);
            }
        }
        return result;
    }

    public static double jaccard(String a, String b) {
        Set<String> left = shingles(a);
        Set<String> right = shingles(b);
        if (left.isEmpty() && right.isEmpty()) {
            return 0.0;
        }
        int intersection = 0;
        for (String token : left) {
            if (right.contains(token)) {
                intersection++;
            }
        }
        int union = left.size() + right.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }

    public static boolean isDuplicate(String a, String b, double threshold) {
        if (a == null || b == null) {
            return false;
        }
        return jaccard(a, b) >= threshold;
    }
}

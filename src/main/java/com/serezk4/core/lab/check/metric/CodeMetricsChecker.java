package com.serezk4.core.lab.check.metric;

import com.serezk4.core.lab.check.Checker;
import com.serezk4.core.lab.model.Clazz;

import java.util.*;
import java.util.stream.Collectors;

public class CodeMetricsChecker implements Checker {

    @Override
    public double detect(Clazz source, Clazz target) {
        String code1 = source.source();
        String code2 = target.source();

        double levenshteinSim = calculateLevenshteinSimilarity(code1, code2);
        double jaccardSim = calculateJaccardSimilarity(code1, code2);
        double cosineSim = calculateCosineSimilarity(code1, code2);

        return Math.max(Math.max(levenshteinSim, jaccardSim), cosineSim);
    }

    private double calculateLevenshteinSimilarity(String code1, String code2) {
        int distance = levenshteinDistance(code1, code2);
        int maxLength = Math.max(code1.length(), code2.length());
        return 1.0 - ((double) distance / maxLength);
    }

    private int levenshteinDistance(String code1, String code2) {
        int n = code1.length();
        int m = code2.length();

        if (n == 0) return m;
        if (m == 0) return n;

        int[] previousRow = new int[m + 1];
        int[] currentRow = new int[m + 1];

        for (int j = 0; j <= m; j++) {
            previousRow[j] = j;
        }

        for (int i = 1; i <= n; i++) {
            currentRow[0] = i;
            for (int j = 1; j <= m; j++) {
                int cost = (code1.charAt(i - 1) == code2.charAt(j - 1)) ? 0 : 1;
                currentRow[j] = Math.min(Math.min(
                        currentRow[j - 1] + 1,
                        previousRow[j] + 1
                ), previousRow[j - 1] + cost);
            }
            System.arraycopy(currentRow, 0, previousRow, 0, m + 1);
        }

        return previousRow[m];
    }

    private double calculateJaccardSimilarity(String code1, String code2) {
        Set<String> set1 = tokenize(code1);
        Set<String> set2 = tokenize(code2);

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private double calculateCosineSimilarity(String code1, String code2) {
        Map<String, Integer> freqMap1 = buildFrequencyMap(code1);
        Map<String, Integer> freqMap2 = buildFrequencyMap(code2);

        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(freqMap1.keySet());
        allTerms.addAll(freqMap2.keySet());

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (String term : allTerms) {
            int freq1 = freqMap1.getOrDefault(term, 0);
            int freq2 = freqMap2.getOrDefault(term, 0);

            dotProduct += freq1 * freq2;
            norm1 += freq1 * freq1;
            norm2 += freq2 * freq2;
        }

        return norm1 == 0.0 || norm2 == 0.0 ? 0.0 : dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private Set<String> tokenize(String code) {
        return Arrays.stream(code.split("\\W+"))
                .map(String::toLowerCase)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toSet());
    }

    private Map<String, Integer> buildFrequencyMap(String code) {
        Map<String, Integer> freqMap = new HashMap<>();
        for (String token : code.split("\\W+")) {
            token = token.toLowerCase();
            if (!token.isEmpty()) {
                freqMap.put(token, freqMap.getOrDefault(token, 0) + 1);
            }
        }
        return freqMap;
    }
}
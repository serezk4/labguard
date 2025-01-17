package com.serezk4.core.apted.costmodel;

import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.TreeMap;

public class WeightedCostModel extends StringUnitCostModel {

    private final Map<String, Float> baseCostCache = new ConcurrentHashMap<>();
    private final Map<String, Float> similarityCache = new ConcurrentHashMap<>();

    private static final TreeMap<Integer, Float> LEVENSHTEIN_SIMILARITY_MAP = new TreeMap<>();

    static {
        for (int i = 0; i <= 128; i++) {
            LEVENSHTEIN_SIMILARITY_MAP.put(i, i / 128.0f);
        }
    }

    @Override
    public float del(Node<StringNodeData> n) {
        return computeCost(n);
    }

    @Override
    public float ins(Node<StringNodeData> n) {
        return computeCost(n);
    }

    @Override
    public float ren(Node<StringNodeData> n1, Node<StringNodeData> n2) {
        String label1 = n1.getNodeData().getLabel();
        String label2 = n2.getNodeData().getLabel();

        if (label1.equals(label2)) return 0f;

        float baseCost = getBaseCost(label1) + getBaseCost(label2);
        float structurePenalty = Math.abs(n1.getChildren().size() - n2.getChildren().size()) * 0.15f;
        float similarity = calculateSemanticSimilarity(label1, label2);
        return baseCost + structurePenalty + (1.0f - similarity) * 3.0f;
    }

    private float computeCost(Node<StringNodeData> n) {
        String label = n.getNodeData().getLabel();
        float baseCost = getBaseCost(label);
        float structurePenalty = calculateStructurePenalty(n);
        return baseCost + structurePenalty;
    }

    private float getBaseCost(String label) {
        return baseCostCache.computeIfAbsent(label, this::computeBaseCost);
    }

    private float computeBaseCost(String label) {
        return switch (label.split(" ")[0]) {
            case "Class", "Interface" -> 4.0f;
            case "Method", "Constructor" -> 3.5f;
            case "Field", "Variable" -> 2.5f;
            case "If", "Else", "Switch" -> 3.0f;
            case "For", "While", "DoWhile" -> 2.8f;
            case "Try", "Catch", "Finally" -> 3.2f;
            case "Annotation" -> 1.5f;
            case "Lambda", "FunctionalInterface" -> 2.8f;
            case "Operator" -> 1.2f;
            case "Return", "Throw" -> 2.5f;
            case "Block", "Statement" -> 1.8f;
            case "Expression" -> 2.0f;
            case "Parameter" -> 1.8f;
            default -> 1.0f;
        };
    }

    private float calculateStructurePenalty(Node<StringNodeData> n) {
        return n.getChildren().stream()
                .map(child -> getBaseCost(child.getNodeData().getLabel()) * 0.1f)
                .reduce(0f, Float::sum);
    }

    private float calculateSemanticSimilarity(String s1, String s2) {
        String key = s1 + "|" + s2;
        return similarityCache.computeIfAbsent(key, k -> {
            int len1 = s1.length(), len2 = s2.length();
            if (len1 == 0 && len2 == 0) return 1.0f;
            if (len1 == 0 || len2 == 0) return 0.0f;

            int levenshteinDist = calculateLevenshteinDistance(s1, s2);
            return LEVENSHTEIN_SIMILARITY_MAP.floorEntry(levenshteinDist).getValue();
        });
    }

    private int calculateLevenshteinDistance(String s1, String s2) {
        int len1 = s1.length(), len2 = s2.length();
        int[][] dp = new int[2][len2 + 1];

        for (int j = 0; j <= len2; j++) dp[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            dp[i % 2][0] = i;
            for (int j = 1; j <= len2; j++) {
                dp[i % 2][j] = Math.min(Math.min(
                                dp[(i - 1) % 2][j] + 1,
                                dp[i % 2][j - 1] + 1),
                        dp[(i - 1) % 2][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1));
            }
        }
        return dp[len1 % 2][len2];
    }
}

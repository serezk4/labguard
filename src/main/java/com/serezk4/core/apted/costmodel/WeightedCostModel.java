package com.serezk4.core.apted.costmodel;

import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WeightedCostModel extends StringUnitCostModel {

    private final Map<String, Float> baseCostCache = new ConcurrentHashMap<>();
    private final Map<String, Float> similarityCache = new ConcurrentHashMap<>();

    @Override
    public float del(Node<StringNodeData> n) {
        return baseCostCache.computeIfAbsent(n.getNodeData().getLabel(), this::computeBaseCost);
    }

    @Override
    public float ins(Node<StringNodeData> n) {
        return baseCostCache.computeIfAbsent(n.getNodeData().getLabel(), this::computeBaseCost);
    }

    @Override
    public float ren(Node<StringNodeData> n1, Node<StringNodeData> n2) {
        String label1 = compressLabel(n1.getNodeData().getLabel());
        String label2 = compressLabel(n2.getNodeData().getLabel());

        if (label1.equals(label2)) return 0f;

        float baseCost = baseCostCache.computeIfAbsent(label1, this::computeBaseCost) +
                baseCostCache.computeIfAbsent(label2, this::computeBaseCost);

        if (baseCost > 5.0f) return baseCost; // Early exit for high base costs

        float structurePenalty = precomputeStructurePenalty(n1, n2);
        float similarity = similarityCache.computeIfAbsent(label1 + "|" + label2, k -> computeSemanticSimilarity(label1, label2));

        if (similarity > 0.7) return 0.1f; // Early exit for high similarity

        return baseCost + structurePenalty + (1.0f - similarity) * 1.5f;
    }

    private float computeBaseCost(String label) {
        if (label.length() < 2) return 1.0f;
        return switch (label.substring(0, 2)) {
            case "Cl", "In" -> 4.0f; // Class, Interface
            case "Me", "Co" -> 3.5f; // Method, Constructor
            case "Fi", "Va" -> 2.5f; // Field, Variable
            case "If", "Sw" -> 3.0f; // If, Switch
            case "Fo", "Wh", "Do" -> 2.8f; // For, While, DoWhile
            case "Tr", "Ca" -> 3.2f; // Try, Catch, Finally
            case "An" -> 1.5f; // Annotation
            case "La", "Fu" -> 2.8f; // Lambda, FunctionalInterface
            case "Op" -> 1.2f; // Operator
            case "Re", "Th" -> 2.5f; // Return, Throw
            case "Bl", "St" -> 1.8f; // Block, Statement
            case "Ex" -> 2.0f; // Expression
            case "Pa" -> 1.8f; // Parameter
            default -> 1.0f;
        };
    }

    private float precomputeStructurePenalty(Node<StringNodeData> n1, Node<StringNodeData> n2) {
        int size1 = n1.getChildren().size();
        int size2 = n2.getChildren().size();
        return Math.min(size1, size2) * 0.05f;
    }

    private float computeSemanticSimilarity(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        float maxLen = Math.max(len1, len2);
        return maxLen == 0 ? 1.0f : 1.0f - (Math.abs(len1 - len2) / maxLen);
    }

    private String compressLabel(String label) {
        return label.length() > 8 ? label.substring(0, 8) : label;
    }
}


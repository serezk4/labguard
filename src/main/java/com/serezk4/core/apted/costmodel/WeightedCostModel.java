package com.serezk4.core.apted.costmodel;

import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements a weighted cost model for the APTED algorithm, providing customizable costs for insertion,
 * deletion, and renaming operations based on structural and semantic characteristics of the nodes.
 *
 * <p>
 * This cost model introduces the following enhancements:
 * <ul>
 *     <li>Semantic similarity between node labels using Levenshtein distance.</li>
 *     <li>Structural penalties based on the difference in subtree sizes and children counts.</li>
 *     <li>Dynamic weighting of costs depending on the type of node (e.g., classes, methods, fields).</li>
 * </ul>
 * </p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * WeightedCostModel costModel = new WeightedCostModel();
 * APTED<WeightedCostModel, StringNodeData> apted = new APTED<>(costModel);
 * float distance = apted.computeEditDistance(node1, node2);
 * }</pre>
 *
 * @see com.serezk4.core.apted.distance.APTED
 * @see Node
 * @see StringNodeData
 * @see StringUnitCostModel
 *
 * @author serezk4
 * @version 1.0
 * @since 1.0
 */
public class WeightedCostModel extends StringUnitCostModel {

    private final Map<String, Float> baseCostCache = new ConcurrentHashMap<>();
    private final Map<String, Float> similarityCache = new ConcurrentHashMap<>();

    private static final TreeMap<Integer, Float> LEVENSHTEIN_SIMILARITY_MAP = new TreeMap<>();

    static {
        for (int i = 0; i <= 128; i++) {
            LEVENSHTEIN_SIMILARITY_MAP.put(i, i / 128.0f);
        }
    }

    /**
     * Computes the deletion cost for a node.
     *
     * @param n the node to be deleted
     * @return the computed deletion cost
     */
    @Override
    public float del(Node<StringNodeData> n) {
        return computeCost(n);
    }

    /**
     * Computes the insertion cost for a node.
     *
     * @param n the node to be inserted
     * @return the computed insertion cost
     */
    @Override
    public float ins(Node<StringNodeData> n) {
        return computeCost(n);
    }

    /**
     * Computes the renaming cost between two nodes.
     *
     * <p>
     * The renaming cost is calculated as:
     * <pre>{@code
     * baseCost + structurePenalty + (1 - semanticSimilarity) * 3
     * }</pre>
     * where:
     * <ul>
     *     <li>{@code baseCost} is the combined base cost of the two nodes.</li>
     *     <li>{@code structurePenalty} is the penalty for differing children counts.</li>
     *     <li>{@code semanticSimilarity} is the Levenshtein-based similarity between node labels.</li>
     * </ul>
     * </p>
     *
     * @param n1 the first node
     * @param n2 the second node
     * @return the computed renaming cost
     */
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

    /**
     * Computes the cost of a node based on its label and structure.
     *
     * @param n the node to analyze
     * @return the computed cost
     */
    private float computeCost(Node<StringNodeData> n) {
        String label = n.getNodeData().getLabel();
        float baseCost = getBaseCost(label);
        float structurePenalty = calculateStructurePenalty(n);
        return baseCost + structurePenalty;
    }


    /**
     * Retrieves the base cost for a given label, using caching for optimization.
     *
     * @param label the label of the node
     * @return the base cost
     */
    private float getBaseCost(String label) {
        return baseCostCache.computeIfAbsent(label, this::computeBaseCost);
    }

    /**
     * Computes the base cost for a label based on predefined rules.
     *
     * @param label the label of the node
     * @return the computed base cost
     */
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

    /**
     * Calculates the structure penalty based on the children of a node.
     *
     * @param n the node to analyze
     * @return the calculated penalty
     */
    private float calculateStructurePenalty(Node<StringNodeData> n) {
        return n.getChildren().stream()
                .map(child -> getBaseCost(child.getNodeData().getLabel()) * 0.1f)
                .reduce(0f, Float::sum);
    }

    /**
     * Calculates the semantic similarity between two strings using Levenshtein distance.
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return a similarity score between 0.0 and 1.0
     */
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

    /**
     * Calculates the Levenshtein distance between two strings.
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return the Levenshtein distance
     */
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

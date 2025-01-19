package com.serezk4.core.lab.check.apted;

import com.serezk4.core.apted.costmodel.WeightedCostModel;
import com.serezk4.core.apted.distance.APTED;
import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;
import com.serezk4.core.apted.util.NodeUtil;
import com.serezk4.core.lab.check.Checker;
import com.serezk4.core.lab.model.Clazz;

import java.util.stream.Stream;

/**
 * Implementation of the {@link Checker} interface that uses the APTED (All Path Tree Edit Distance) algorithm
 * to calculate the similarity between two Java classes represented as tree structures.
 *
 * <p>
 * The similarity is computed as:
 * <pre>{@code
 * similarity = 1.0 - (editDistance / maxSubtreeSize)
 * }</pre>
 * where:
 * <ul>
 *     <li>{@code editDistance} is the cost of transforming one tree into another.</li>
 *     <li>{@code maxSubtreeSize} is the size of the larger tree, ensuring the score is normalized.</li>
 * </ul>
 * </p>
 *
 * <p>
 * This implementation is particularly suited for structural comparisons of code, leveraging the
 * {@link APTED} algorithm with a customizable {@link WeightedCostModel}.
 * </p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * Checker checker = new AptedCheck();
 * double similarity = checker.detect(sourceClazz, targetClazz);
 * System.out.println("Similarity score: " + similarity);
 * }</pre>
 *
 * @see Checker
 * @see APTED
 * @see NodeUtil
 * @see com.serezk4.core.lab.model.Clazz
 * @see WeightedCostModel
 *
 * @author serezk4
 * @version 1.0
 * @since 1.0
 */
public class AptedCheck implements Checker {

    /**
     * Detects the similarity between two Java classes using the APTED algorithm.
     *
     * <p>
     * This method converts the {@link Clazz} objects into tree representations using {@link NodeUtil}.
     * It then computes the edit distance between the two trees and normalizes the score based on the
     * size of the larger tree.
     * </p>
     *
     * @param source the source {@link Clazz} to be compared
     * @param target the target {@link Clazz} to compare against
     * @return a {@code double} value between 0.0 and 1.0 representing the similarity score, where:
     *         <ul>
     *             <li>{@code 0.0} indicates no similarity.</li>
     *             <li>{@code 1.0} indicates the classes are identical.</li>
     *         </ul>
     */
    @Override
    public double detect(
            final Clazz source,
            final Clazz target
    ) {
        final Node<StringNodeData> node1 = NodeUtil.parseTreeToNode(source.tree());
        final Node<StringNodeData> node2 = NodeUtil.parseTreeToNode(target.tree());

        final APTED<WeightedCostModel, StringNodeData> apted = new APTED<>(new WeightedCostModel());

        return 1.0 - (apted.computeEditDistance(node1, node2) / Math.max(
                calculateSubtreeSize(node1),
                calculateSubtreeSize(node2)
        ));
    }

    /**
     * Calculates the size of a subtree rooted at the given node.
     *
     * <p>
     * The size of a subtree is defined as the total number of nodes it contains,
     * including the root node and all of its descendants. This method is implemented
     * recursively and optimized for parallel computation using {@link Stream#parallel()} ()}.
     * </p>
     *
     * @param node the root node of the subtree
     * @return the size of the subtree as an integer; returns 0 if the node is {@code null}
     */
    private static int calculateSubtreeSize(final Node<StringNodeData> node) {
        if (node == null) return 0;
        return 1 + node.getChildren().parallelStream()
                .mapToInt(AptedCheck::calculateSubtreeSize)
                .sum();
    }
}

package com.serezk4.core.lab.check;

import com.serezk4.core.lab.model.Clazz;

/**
 * Defines a contract for detecting similarity between two Java classes.
 *
 * <p>
 * Implementations of this interface are responsible for comparing two {@link Clazz} instances
 * and calculating a similarity score. The score is represented as a {@code double} value in the range
 * of {@code 0.0} to {@code 1.0}, where:
 * <ul>
 *     <li>{@code 0.0} indicates no similarity.</li>
 *     <li>{@code 1.0} indicates the classes are identical.</li>
 * </ul>
 * </p>
 *
 * <p>
 * This interface is designed to be implemented by various algorithms that detect similarity, such as
 * tree-based, token-based, or semantic comparison algorithms.
 * </p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * Checker checker = new SomeCheckerImplementation();
 * double similarity = checker.detect(sourceClazz, targetClazz);
 * System.out.println("Similarity: " + similarity);
 * }</pre>
 *
 * @see Clazz
 *
 * @author serezk4
 * @version 1.0
 * @since 1.0
 */
public interface Checker {

    /**
     * Detects the similarity between two {@link Clazz} objects.
     *
     * <p>
     * The method evaluates how similar the source class is to the target class based on a specific algorithm.
     * Implementations may use various strategies such as tree-edit distance, token matching, or structural
     * analysis to calculate the similarity score.
     * </p>
     *
     * @param source the source {@link Clazz} to be compared
     * @param target the target {@link Clazz} to compare against
     * @return a {@code double} value representing the similarity score, ranging from {@code 0.0} to {@code 1.0}
     */
    double detect(Clazz source, Clazz target);
}

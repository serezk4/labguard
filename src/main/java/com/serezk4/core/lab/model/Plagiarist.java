package com.serezk4.core.lab.model;

/**
 * Represents a detected case of plagiarism between two Java classes.
 *
 * <p>
 * The {@code Plagiarist} record stores information about:
 * <ul>
 *     <li>The target class that is suspected of being plagiarized.</li>
 *     <li>The plagiarized class that shows significant similarity to the target.</li>
 *     <li>The similarity score between the two classes, expressed as a value between 0.0 and 1.0.</li>
 * </ul>
 * This class is typically used to encapsulate the results of plagiarism detection algorithms.
 * </p>
 *
 * @param targetClazz      The {@link Clazz} that is being compared as the original source.
 * @param plagiarizedClazz The {@link Clazz} suspected of plagiarizing the {@code targetClazz}.
 * @param similarity       A {@code double} value between 0.0 and 1.0 representing the similarity score.
 *                         A higher score indicates greater similarity, where 1.0 represents identical content.
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * Plagiarist case = new Plagiarist(target, suspect, 0.85);
 * System.out.println("Plagiarism detected with similarity: " + case.similarity());
 * }</pre>
 *
 * <p>
 * This class is immutable and can be used in collections or concurrent environments without additional synchronization.
 * </p>
 *
 * @see Clazz
 *
 * @author serez
 * @version 1.0
 * @since 1.0
 */
public record Plagiarist(
        Clazz targetClazz,
        Clazz plagiarizedClazz,
        double similarity
) {
}

package com.serezk4.core.lab.model;

import com.serezk4.core.apted.util.NodeUtil;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

/**
 * Represents a Java class parsed from a source file.
 *
 * <p>
 * This class contains metadata and analysis results for a Java class, including:
 * <ul>
 *     <li>The name of the class.</li>
 *     <li>The parsed {@link ParseTree} representation of the class.</li>
 *     <li>The raw source code of the class.</li>
 *     <li>A list of Checkstyle analysis results.</li>
 * </ul>
 * It also provides a method to convert the class into a {@link StoredClazz} format for storage purposes.
 * </p>
 *
 * @param name       The name of the class, typically derived from the file name.
 * @param tree       The {@link ParseTree} representing the syntactic structure of the class.
 * @param source     The raw source code of the class.
 * @param checkstyle A list of strings representing Checkstyle analysis results for the class.
 * @author serezk4
 * @version 1.0
 * @since 1.0
 */
public record Clazz(
        String name,
        ParseTree tree,
        String source,
        String normalizedSource,
        List<String> checkstyle
) {

    /**
     * Converts the current {@code Clazz} instance into a {@link StoredClazz}.
     *
     * <p>
     * This method serializes the {@link ParseTree} into a simplified node structure
     * using {@link NodeUtil#parseTreeToNode(ParseTree)}. The resulting {@link StoredClazz}
     * can be used for caching or storage.
     * </p>
     *
     * @return a {@link StoredClazz} object containing the serialized representation of the class.
     */
    public StoredClazz toStoredTree() {
        return new StoredClazz(name, NodeUtil.parseTreeToNode(tree), source, checkstyle);
    }
}

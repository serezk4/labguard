package com.serezk4.core.lab.model;

import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;
import com.serezk4.core.apted.util.NodeUtil;

import java.util.List;

/**
 * Represents a serialized version of a Java class for storage and caching purposes.
 *
 * <p>
 * The {@code StoredClazz} record contains the essential data of a parsed Java class, including:
 * <ul>
 *     <li>The file path of the class.</li>
 *     <li>The structural representation of the class as a tree of {@link StringNodeData} nodes.</li>
 *     <li>The original source code of the class.</li>
 *     <li>A list of Checkstyle analysis results for the class.</li>
 * </ul>
 * This class is optimized for persistence and can be converted back to its original {@link Clazz} form.
 * </p>
 *
 * @param filePath   The path to the file containing the Java class.
 *                   Typically used to identify and locate the original source.
 * @param node       A tree representation of the Java class using {@link Node} with {@link StringNodeData}.
 *                   This structure is used for tree-based analysis and comparisons.
 * @param source     The raw source code of the class as a {@code String}.
 * @param checkstyle A list of strings representing Checkstyle analysis results for the class.
 *                   Each string corresponds to a specific rule violation or warning.
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * StoredClazz stored = new StoredClazz(filePath, node, source, checkstyleResults);
 * Clazz clazz = stored.toClazz();
 * }</pre>
 *
 * <p>
 * This class is typically used in conjunction with storage systems and tree comparison algorithms.
 * </p>
 *
 * @see Clazz
 * @see Node
 * @see NodeUtil
 *
 * @author serezk4
 * @version 1.0
 * @since 1.0
 */
public record StoredClazz(
        String filePath,
        Node<StringNodeData> node,
        String source,
        List<String> checkstyle
) {

    /**
     * Converts this {@code StoredClazz} back into a {@link Clazz} object.
     *
     * <p>
     * This method reconstructs the {@link Clazz} by parsing the {@link Node} tree representation
     * into an ANTLR {@link org.antlr.v4.runtime.tree.ParseTree} using {@link NodeUtil#parseNodeToTree(Node)}.
     * </p>
     *
     * @return a {@link Clazz} object representing the original parsed Java class
     */
    public Clazz toClazz() {
        return new Clazz(filePath, NodeUtil.parseNodeToTree(node), source, checkstyle);
    }
}

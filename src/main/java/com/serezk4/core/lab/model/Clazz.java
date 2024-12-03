package com.serezk4.core.lab.model;

import com.serezk4.core.apted.util.NodeUtil;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

public record Clazz(String name, ParseTree tree, String source, List<String> checkstyle) {
    public StoredClazz toStoredTree() {
        return new StoredClazz(name, NodeUtil.parseTreeToNode(tree), source, checkstyle);
    }
}

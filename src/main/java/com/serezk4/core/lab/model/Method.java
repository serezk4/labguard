package com.serezk4.core.lab.model;

import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;
import org.antlr.v4.runtime.tree.ParseTree;

public record Method(String name, ParseTree tree, String source, Node<StringNodeData> treeStructure) {
}

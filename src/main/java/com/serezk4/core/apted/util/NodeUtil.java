package com.serezk4.core.apted.util;

import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

import java.util.Optional;

public class NodeUtil {
    public static Node<StringNodeData> parseTreeToNode(ParseTree tree) {
        if (tree == null || tree.getChildCount() == 0) return null;

        String type = tree.getClass().getSimpleName();
        Node<StringNodeData> node = new Node<>(new StringNodeData(type));

        for (int i = 0; i < tree.getChildCount(); i++) {
            Node<StringNodeData> childNode = parseTreeToNode(tree.getChild(i));
            Optional.ofNullable(childNode).ifPresent(node::addChild);
        }

        return node;
    }

    public static ParseTree parseNodeToTree(Node<StringNodeData> node) {
        if (node == null) return null;
        return null;
    }
}

package com.serezk4.core.lab.model;

import com.serezk4.core.antlr4.JavaParser;
import com.serezk4.core.antlr4.JavaParserBaseVisitor;
import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;

public record Clazz(
        String name,
        ParseTree tree,
        String source,
        List<String> checkstyle,
        List<Method> methods
) {
    public StoredClazz toStoredTree() {
        return new StoredClazz(name, tree, source, checkstyle, methods);
    }

    public Clazz(String name, ParseTree tree, String source, List<String> checkstyle) {
        this(name, tree, source, checkstyle, extractMethods(tree));
    }

    private static Node<StringNodeData> parseTreeToNode(ParseTree tree) {
        if (tree == null) return null;

        String label = tree.getClass().getSimpleName();
        Node<StringNodeData> node = new Node<>(new StringNodeData(label));

        for (int i = 0; i < tree.getChildCount(); i++) {
            Node<StringNodeData> childNode = parseTreeToNode(tree.getChild(i));
            if (childNode != null) {
                node.addChild(childNode);
            }
        }
        return node;
    }

    private static List<Method> extractMethods(ParseTree tree) {
        List<Method> methods = new ArrayList<>();

        tree.accept(new JavaParserBaseVisitor<Void>() {
            @Override
            public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
                String methodName = ctx.identifier().getText();
                System.out.println(methodName);
                String methodSource = ctx.getText();
                methods.add(new Method(methodName, ctx, methodSource, parseTreeToNode(ctx)));
                return null;
            }
        });

        return methods;
    }
}

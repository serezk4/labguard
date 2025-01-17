package com.serezk4.core.lab.storage;

import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

import java.util.Optional;
import java.util.StringJoiner;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class CustomParseTree implements ParseTree {

    Node<StringNodeData> node;

    @Override
    public ParseTree getParent() {
        return null;
    }

    @Override
    public void setParent(RuleContext ruleContext) {
        throw new UnsupportedOperationException("CustomParseTree does not support parents.");
    }

    @Override
    public ParseTree getChild(int i) {
        return Optional.of(i)
                .filter(index -> index >= 0 && index < node.getChildren().size())
                .map(index -> new CustomParseTree(node.getChildren().get(index)))
                .orElse(null);
    }

    @Override
    public int getChildCount() {
        return node.getChildren().size();
    }

    @Override
    public String getText() {
        return node.getNodeData().getLabel();
    }

    @Override
    public String toStringTree(Parser parser) {
        throw new UnsupportedOperationException("CustomParseTree does not support parsers.");
    }

    @Override
    public String toStringTree() {
        if (getChildCount() == 0) return getText();

        StringJoiner joiner = new StringJoiner(", ", getText() + " (", ")");
        for (int i = 0; i < getChildCount(); i++) joiner.add(getChild(i).toStringTree());

        return joiner.toString();
    }

    @Override
    public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
        throw new UnsupportedOperationException("CustomParseTree does not support visitors.");
    }

    @Override
    public ParseTree getPayload() {
        return this;
    }

    @Override
    public Interval getSourceInterval() {
        return Interval.INVALID;
    }

    @Override
    public String toString() {
        return getText();
    }
}

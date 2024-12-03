package com.serezk4.core.lab.cache;

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

import java.util.StringJoiner;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class CustomParseTree implements ParseTree {
    Node<StringNodeData> node;

    @Override
    public ParseTree getParent() {
        return null;
    }

    @Override
    public ParseTree getChild(int i) {
        if (i < 0 || i >= node.getChildren().size()) return null;
        return new CustomParseTree(node.getChildren().get(i));
    }

    @Override
    public void setParent(RuleContext ruleContext) {
        throw new UnsupportedOperationException("CustomParseTree does not support parents.");
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
        StringBuilder sb = new StringBuilder(getText());
        if (getChildCount() <= 0) return sb.toString();

        sb.append(" (");
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < getChildCount(); i++) joiner.add(getChild(i).toStringTree());
        sb.append(joiner).append(")");

        return sb.toString();
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

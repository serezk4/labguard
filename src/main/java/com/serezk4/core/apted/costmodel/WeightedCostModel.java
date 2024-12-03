package com.serezk4.core.apted.costmodel;

import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;

public class WeightedCostModel extends StringUnitCostModel {
    @Override
    public float del(Node<StringNodeData> n) {
        return n.getNodeData().getLabel().startsWith("Method") ? 2.0f : 1.0f;
    }

    @Override
    public float ins(Node<StringNodeData> n) {
        return n.getNodeData().getLabel().startsWith("Method") ? 2.0f : 1.0f;
    }

    @Override
    public float ren(Node<StringNodeData> n1, Node<StringNodeData> n2) {
        if (n1.getNodeData().getLabel().equals(n2.getNodeData().getLabel())) return 0f;
        if (n1.getNodeData().getLabel().startsWith("Method") && n2.getNodeData().getLabel().startsWith("Method"))
            return 0.5f;
        return 1.0f;
    }
}

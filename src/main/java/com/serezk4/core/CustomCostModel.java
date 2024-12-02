package com.serezk4.core;

import com.serezk4.core.apted.costmodel.StringUnitCostModel;
import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;

public class CustomCostModel extends StringUnitCostModel {
    @Override
    public float del(Node<StringNodeData> n) {
        return 0.5f;
    }

    @Override
    public float ins(Node<StringNodeData> n) {
        return 0.5f;
    }

    @Override
    public float ren(Node<StringNodeData> n1, Node<StringNodeData> n2) {
        return n1.getNodeData().getLabel().equals(n2.getNodeData().getLabel()) ? 0.1f : 1.0f;
    }
}
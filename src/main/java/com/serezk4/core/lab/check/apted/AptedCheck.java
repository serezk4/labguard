package com.serezk4.core.lab.check.apted;

import com.serezk4.core.apted.costmodel.WeightedCostModel;
import com.serezk4.core.apted.distance.APTED;
import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;
import com.serezk4.core.apted.util.NodeUtil;
import com.serezk4.core.lab.check.Detector;
import com.serezk4.core.lab.model.Clazz;

public class AptedCheck implements Detector {
    @Override
    public double detect(Clazz clazz1, Clazz clazz2) {
        Node<StringNodeData> node1 = NodeUtil.parseTreeToNode(clazz1.tree());
        Node<StringNodeData> node2 = NodeUtil.parseTreeToNode(clazz2.tree());

        APTED<WeightedCostModel, StringNodeData> apted = new APTED<>(new WeightedCostModel());
        double distance = apted.computeEditDistance(node1, node2);
        int maxSize = Math.max(calculateSubtreeSize(node1), calculateSubtreeSize(node2));

        return 1.0 - (distance / maxSize);
    }

    public static int calculateSubtreeSize(Node<StringNodeData> node) {
        if (node == null) return 0;
        return 1 + node.getChildren().stream().mapToInt(AptedCheck::calculateSubtreeSize).sum();
    }
}

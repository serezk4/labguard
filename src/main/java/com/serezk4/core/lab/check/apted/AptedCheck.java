package com.serezk4.core.lab.check.apted;

import com.serezk4.core.apted.costmodel.WeightedCostModel;
import com.serezk4.core.apted.distance.APTED;
import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;
import com.serezk4.core.lab.check.Checker;
import com.serezk4.core.lab.model.Method;

public class AptedCheck implements Checker {

    @Override
    public double detect(Method source, Method target) {
        APTED<WeightedCostModel, StringNodeData> apted = new APTED<>(new WeightedCostModel());

        return 1.0 - (apted.computeEditDistance(source.treeStructure(), target.treeStructure()) / Math.max(
                calculateSubtreeSize(source.treeStructure()),
                calculateSubtreeSize(target.treeStructure())
        ));
    }

    public static int calculateSubtreeSize(Node<StringNodeData> node) {
        if (node == null) return 0;
        return 1 + node.getChildren().parallelStream()
                .mapToInt(AptedCheck::calculateSubtreeSize)
                .sum();
    }
}

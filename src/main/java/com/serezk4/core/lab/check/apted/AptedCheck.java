package com.serezk4.core.lab.check.apted;

import com.serezk4.core.apted.costmodel.WeightedCostModel;
import com.serezk4.core.apted.distance.APTED;
import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;
import com.serezk4.core.lab.check.Checker;
import com.serezk4.core.lab.model.Method;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AptedCheck implements Checker {

    private final Map<Node<StringNodeData>, Integer> subtreeSizeCache = new ConcurrentHashMap<>();

    @Override
    public double detect(
            final Method source,
            final Method target
    ) {
        APTED<WeightedCostModel, StringNodeData> apted = new APTED<>(new WeightedCostModel());

        double editDistance = apted.computeEditDistance(source.treeStructure(), target.treeStructure());
        double maxSubtreeSize = Math.max(
                getCachedSubtreeSize(source.treeStructure()),
                getCachedSubtreeSize(target.treeStructure())
        );

        if (maxSubtreeSize == 0) return 1.0;
        return 1.0 - (editDistance / maxSubtreeSize);
    }

    private int getCachedSubtreeSize(final Node<StringNodeData> node) {
        if (node == null) return 0;
        return subtreeSizeCache.computeIfAbsent(node, this::calculateSubtreeSize);
    }

    private int calculateSubtreeSize(final Node<StringNodeData> node) {
        if (node == null) return 0;

        int size = 0;
        ConcurrentHashMap<Node<StringNodeData>, Boolean> visited = new ConcurrentHashMap<>();
        Deque<Node<StringNodeData>> stack = new ArrayDeque<>();
        stack.push(node);

        while (!stack.isEmpty()) {
            Node<StringNodeData> current = stack.pop();
            if (visited.putIfAbsent(current, true) != null) continue;
            size++;
            stack.addAll(current.getChildren());
        }

        return size;
    }
}

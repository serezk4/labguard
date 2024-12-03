package com.serezk4.core.lab.model;

import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;
import com.serezk4.core.apted.util.NodeUtil;

import java.util.List;

public record StoredClazz(String filePath, Node<StringNodeData> node, String source, List<String> checkstyle) {
    public Clazz toClazz() {
        return new Clazz(filePath, NodeUtil.parseNodeToTree(node), source, checkstyle);
    }
}
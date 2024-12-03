package com.serezk4.core.lab.model;

import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;

public record StoredTree(String filePath, Node<StringNodeData> node) {
}
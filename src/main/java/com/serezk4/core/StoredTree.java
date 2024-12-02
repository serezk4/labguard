package com.serezk4.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;

public class StoredTree {
    private String filePath;
    private Node<StringNodeData> tree;

    @JsonCreator
    public StoredTree(
            @JsonProperty("filePath") String filePath,
            @JsonProperty("tree") Node<StringNodeData> tree) {
        this.filePath = filePath;
        this.tree = tree;
    }

    // Геттеры и сеттеры
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Node<StringNodeData> getTree() {
        return tree;
    }

    public void setTree(Node<StringNodeData> tree) {
        this.tree = tree;
    }
}
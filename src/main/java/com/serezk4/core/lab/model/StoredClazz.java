package com.serezk4.core.lab.model;

import com.google.gson.annotations.JsonAdapter;
import com.serezk4.core.lab.storage.adapter.ParseTreeAdapter;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

public record StoredClazz(String filePath, @JsonAdapter(ParseTreeAdapter.class) ParseTree tree, String source, List<String> checkstyle, List<Method> methods) {
    public Clazz toClazz() {
        return new Clazz(filePath, tree, source, checkstyle, methods);
    }
}
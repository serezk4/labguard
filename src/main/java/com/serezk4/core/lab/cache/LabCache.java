package com.serezk4.core.lab.cache;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import com.serezk4.core.StoredTree;
import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;
import com.serezk4.core.apted.util.NodeUtil;
import com.serezk4.core.lab.model.Lab;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

public class LabCache {
    private static final Path CACHE_ROOT = Paths.get("lab_cache");
    private final Gson gson;

    public LabCache() throws IOException {
        Files.createDirectories(CACHE_ROOT);
        this.gson = new Gson();
    }

    public void save(Lab lab) throws IOException {
        Path labPath = getLabPath(lab.getIsu(), lab.getLabNumber());
        Files.createDirectories(labPath);

        Node<StringNodeData> node = NodeUtil.parseTreeToNode(lab.getTree());
        StoredTree storedTree = new StoredTree(UUID.randomUUID().toString(), node);
        Path outputPath = labPath.resolve(UUID.randomUUID().toString().concat(".json"));

        try (Writer writer = Files.newBufferedWriter(outputPath);
             JsonWriter jsonWriter = gson.newJsonWriter(writer)) {
            gson.toJson(storedTree, StoredTree.class, jsonWriter);
            jsonWriter.flush();
        }
    }

    public Optional<StoredTree> loadLab(String isu, int labNumber, String fileName) throws IOException {
        Path inputPath = getLabPath(isu, labNumber).resolve(fileName.concat(".json"));

        if (Files.notExists(inputPath)) return Optional.empty();
        try (BufferedReader reader = Files.newBufferedReader(inputPath)) {
            return Optional.ofNullable(gson.fromJson(reader, StoredTree.class));
        }
    }

    private Path getLabPath(String isu, int labNumber) {
        return CACHE_ROOT.resolve(isu).resolve(String.valueOf(labNumber));
    }
}
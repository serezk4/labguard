package com.serezk4.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class LabCache {
    private static final Path CACHE_ROOT = Paths.get("lab_cache");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LabCache() throws IOException {
        if (!Files.exists(CACHE_ROOT)) {
            Files.createDirectories(CACHE_ROOT);
        }
    }

    public void saveLab(String isu, int labNumber, String fileName, Node<StringNodeData> node) throws IOException {
        Path labPath = getLabPath(isu, labNumber);
        if (!Files.exists(labPath)) {
            Files.createDirectories(labPath);
        }

        Path outputPath = labPath.resolve(fileName.concat(".json"));
        StoredTree storedTree = new StoredTree(fileName, node);
        objectMapper.writeValue(outputPath.toFile(), storedTree);
    }

    public Optional<StoredTree> loadLab(String isu, int labNumber, String fileName) throws IOException {
        Path labPath = getLabPath(isu, labNumber);
        Path inputPath = labPath.resolve(fileName.concat(".json"));

        if (Files.exists(inputPath)) {
            return Optional.of(objectMapper.readValue(inputPath.toFile(), StoredTree.class));
        }

        return Optional.empty();
    }

    private Path getLabPath(String isu, int labNumber) {
        return CACHE_ROOT.resolve(isu).resolve(String.valueOf(labNumber));
    }
}
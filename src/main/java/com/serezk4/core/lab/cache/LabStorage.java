package com.serezk4.core.lab.cache;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import com.serezk4.core.lab.model.Clazz;
import com.serezk4.core.lab.model.Lab;
import com.serezk4.core.lab.model.StoredClazz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;

public final class LabStorage {
    private static final Path CACHE_ROOT = Paths.get("lab_cache");
    private final Gson gson = new Gson();

    public LabStorage() throws IOException {
        Files.createDirectories(CACHE_ROOT);
    }

    public void save(Lab lab) {
        Path labPath = getLabPath(lab.isu(), lab.labNumber());

        try {
            Files.createDirectories(labPath);
        } catch (IOException e) {
            System.err.println("Error creating lab directory: " + e.getMessage());
            return;
        }

        lab.clazzes().forEach(clazz -> saveNode(labPath, clazz));
    }

    private void saveNode(Path path, Clazz clazz) {
        Path outputPath = path.resolve(clazz.name().concat(".json"));
        try (JsonWriter jsonWriter = gson.newJsonWriter(Files.newBufferedWriter(outputPath))) {
            gson.toJson(clazz.toStoredTree(), StoredClazz.class, jsonWriter);
        } catch (IOException e) {
            System.err.println("Error saving node: " + e.getMessage());
        }
    }

    public Lab loadLab(String isu, int labNumber) {
        Path labPath = getLabPath(isu, labNumber);
        if (Files.notExists(labPath)) return new Lab(isu, labNumber, null);

        try (Stream<Path> files = Files.list(labPath)) {
            return new Lab(isu, labNumber,
                    files.map(this::loadNode)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toList());
        } catch (IOException e) {
            System.err.println("Error loading lab: " + e.getMessage());
            return new Lab(isu, labNumber, new ArrayList<>());
        }
    }

    private Optional<Clazz> loadNode(Path file) {
        try (var reader = Files.newBufferedReader(file)) {
            StoredClazz storedClazz = gson.fromJson(reader, StoredClazz.class);
            return Optional.of(storedClazz.toClazz());
        } catch (IOException e) {
            System.err.println("Error loading node: " + e.getMessage());
            return Optional.empty();
        }
    }

    private Path getLabPath(String isu, int labNumber) {
        return CACHE_ROOT.resolve(isu).resolve(String.valueOf(labNumber));
    }
}
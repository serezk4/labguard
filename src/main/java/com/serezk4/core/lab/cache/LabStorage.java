package com.serezk4.core.lab.cache;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import com.serezk4.core.antlr4.JavaLexer;
import com.serezk4.core.antlr4.JavaParser;
import com.serezk4.core.lab.analyze.linter.CheckstyleAnalyzer;
import com.serezk4.core.lab.model.Clazz;
import com.serezk4.core.lab.model.Lab;
import com.serezk4.core.lab.model.StoredClazz;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DiagnosticErrorListener;
import org.antlr.v4.runtime.atn.PredictionMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public final class LabStorage {
    private static final Path CACHE_ROOT = Paths.get("/Users/serezk4/labguard/core/lab_cache");
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

    public List<Lab> loadAllByLabNumber(int labNumber) {
        try (Stream<Path> isuPaths = Files.list(CACHE_ROOT)) {
            return isuPaths
                    .filter(Files::isDirectory)
                    .map(isuPath -> loadLab(isuPath.getFileName().toString(), labNumber))
                    .sorted(Comparator.comparing(Lab::isu))
                    .peek(lab -> System.out.println("Loaded lab: " + lab.isu()))
                    .toList();
        } catch (IOException e) {
            System.err.println("Error loading all labs: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Lab load(String isu, int labNumber, Path path) {
        return new Lab(isu, labNumber, getFiles(path.toString()).stream()
                .map(this::parseFile)
                .filter(Objects::nonNull)
                .toList());
    }

    private Clazz parseFile(Path path) {
        try {
            String code = Files.readString(path);
            String normalizedCode = normalize(code);

            CharStream charStream = CharStreams.fromString(normalizedCode);
            JavaLexer lexer = new JavaLexer(charStream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            JavaParser parser = new JavaParser(tokens) {{
                getInterpreter().setPredictionMode(PredictionMode.SLL);
                addErrorListener(new DiagnosticErrorListener());
            }};

            List<String> pmdReport = CheckstyleAnalyzer.getInstance().analyzeCode(path);
            return new Clazz(path.getFileName().toString(), parser.compilationUnit(), code, pmdReport);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    private List<Path> getFiles(final String stringPath)  {
        try (Stream<Path> paths = Files.walk(Paths.get(stringPath))) {
            return paths.filter(Files::isRegularFile)
                    .filter(_path -> _path.toString().endsWith(".java"))
                    .toList();
        } catch (IOException e) {
            System.err.println("Error getting files: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private String normalize(String code) {
        return code
                .replaceAll("(?s)/\\*.*?\\*/|//.*", "")
                .replaceAll("\\b\\d+\\b", "0")
                .replaceAll("\".*?\"", "\"stringLiteral\"");
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
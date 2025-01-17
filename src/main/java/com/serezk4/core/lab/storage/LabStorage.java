package com.serezk4.core.lab.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.serezk4.core.antlr4.JavaLexer;
import com.serezk4.core.antlr4.JavaParser;
import com.serezk4.core.lab.analyze.checkstyle.CheckstyleAnalyzer;
import com.serezk4.core.lab.model.Clazz;
import com.serezk4.core.lab.model.Lab;
import com.serezk4.core.lab.model.StoredClazz;
import com.serezk4.core.lab.storage.adapter.ParseTreeAdapter;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DiagnosticErrorListener;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class LabStorage {
    private static final Path CACHE_ROOT = Paths.get("/Users/serezk4/labguard/core/lab_cache");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("(?s)/\\*.*?\\*/|//.*");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+\\b");
    private static final Pattern STRING_PATTERN = Pattern.compile("\".*?\"");

    private final Gson gson = new GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .registerTypeAdapter(ParseTree.class, new ParseTreeAdapter())
            .create();
    private final Map<Path, Clazz> parsedFileCache = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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

        lab.clazzes().parallelStream().forEach(clazz -> saveNode(labPath, clazz));
    }

    private void saveNode(Path path, Clazz clazz) {
        Path outputPath = path.resolve(clazz.name().concat(".json"));
        try (var writer = Files.newBufferedWriter(outputPath)) {
            gson.toJson(clazz.toStoredTree(), writer);
        } catch (IOException e) {
            System.err.println("Error saving node: " + e.getMessage());
        }
    }

    public List<Lab> loadAllByLabNumber(int labNumber) {
        try (Stream<Path> isuPaths = Files.list(CACHE_ROOT)) {
            return isuPaths
                    .parallel()
                    .filter(Files::isDirectory)
                    .map(isuPath -> loadLab(isuPath.getFileName().toString(), labNumber))
                    .filter(lab -> lab.clazzes() != null)
                    .sorted(Comparator.comparing(Lab::isu))
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
        return parsedFileCache.computeIfAbsent(path, p -> {
            try {
                String code = Files.readString(p);
                String normalizedCode = normalize(code);

                CharStream charStream = CharStreams.fromString(normalizedCode);
                JavaLexer lexer = new JavaLexer(charStream);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                JavaParser parser = new JavaParser(tokens) {{
                    getInterpreter().setPredictionMode(PredictionMode.SLL);
                }};

                List<String> pmdReport = CheckstyleAnalyzer.getInstance().analyzeCode(p);
                return new Clazz(p.getFileName().toString(), parser.compilationUnit(), code, pmdReport);
            } catch (IOException e) {
                System.err.println("Error parsing file: " + e.getMessage());
                return null;
            }
        });
    }

    private List<Path> getFiles(final String stringPath) {
        try (Stream<Path> paths = Files.walk(Paths.get(stringPath), Integer.MAX_VALUE)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        } catch (IOException e) {
            System.err.println("Error getting files: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private String normalize(String code) {
        return STRING_PATTERN.matcher(
                NUMBER_PATTERN.matcher(
                        COMMENT_PATTERN.matcher(code).replaceAll("")
                ).replaceAll("0")
        ).replaceAll("\"stringLiteral\"");
    }

    public Lab loadLab(String isu, int labNumber) {
        Path labPath = getLabPath(isu, labNumber);
        if (Files.notExists(labPath)) return new Lab(isu, labNumber, null);

        try (Stream<Path> files = Files.list(labPath)) {
            return new Lab(isu, labNumber,
                    new LinkedList<>(files.map(this::loadNode)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .sorted(Comparator.comparingInt(clazz -> clazz.source().length()))
                            .toList()));
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

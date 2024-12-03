package com.serezk4.core;

import com.google.gson.Gson;
import com.serezk4.core.antlr4.JavaLexer;
import com.serezk4.core.antlr4.JavaParser;
import com.serezk4.core.apted.costmodel.WeightedCostModel;
import com.serezk4.core.apted.distance.APTED;
import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;
import com.serezk4.core.apted.util.NodeUtil;
import com.serezk4.core.lab.cache.CustomParseTree;
import com.serezk4.core.lab.cache.LabCache;
import com.serezk4.core.lab.model.Lab;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class Main {
    public static final int TEST = 3;
    // todo temp
    public static final String CHECK_DIRECTORY_PATH = "/Users/serezk4/labguard/core/test/1/%d".formatted(TEST);

    private final Map<String, Double> similarityCache = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public static void main(String... args) throws IOException {
        new Main().run("5591", 6);
    }

    private void run(
            final String isu,
            final int labNumber
    ) throws IOException {
        LabCache cache = new LabCache();

        List<Path> source = getFiles(CHECK_DIRECTORY_PATH);

        System.out.printf("Source: %d Java files found.%n", source.size());

        List<ParseTree> sourceTrees = source.stream()
                .map(file -> {
                    try {
                        return parseFileAndSave(file, cache, isu, labNumber);
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        AtomicLong plagiarismCount = new AtomicLong();

        sourceTrees.parallelStream().forEach(sourceTree -> {
            sourceTrees.parallelStream().forEach(targetTree -> {
                if (sourceTree == targetTree) return;
                double similarity = calculateTreeSimilarity(sourceTree, targetTree);

                if (similarity > 0.7) {
                    plagiarismCount.incrementAndGet();
                }
            });
        });

        System.out.println("Plagiarism analysis completed.");
        System.out.println("Total plagiarism cases detected: " + plagiarismCount.get());
    }

    private ParseTree parseFileAndSave(Path filePath, LabCache cache, String isu, int labNumber) throws IOException {
        String fileName = filePath.getFileName().toString();
        Optional<StoredTree> storedResult = cache.loadLab(isu, labNumber, fileName);

        if (storedResult.isPresent()) {
            System.out.printf("File %s already processed for ISU %s, Lab %d.%n", fileName, isu, labNumber);
            return parseNodeToTree(storedResult.get().getTree());
        }

        System.out.printf("Processing new file: %s%n", fileName);
        ParseTree tree = parseFile(filePath);
        cache.save(Lab.builder().isu(isu).labNumber(labNumber).tree(tree).build());

        return tree;
    }

    private ParseTree parseFile(Path path) {
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

            return parser.compilationUnit();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    private List<Path> getFiles(String stringPath) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(stringPath))) {
            return paths.filter(Files::isRegularFile)
                    .filter(_path -> _path.toString().endsWith(".java"))
                    .toList();
        }
    }

    private String normalize(String code) {
        return code
                .replaceAll("(?s)/\\*.*?\\*/|//.*", "")
                .replaceAll("\\b\\d+\\b", "0")
                .replaceAll("\".*?\"", "\"stringLiteral\"");
    }

    // todo maybe use this method
    private boolean quickFilter(ParseTree tree1, ParseTree tree2) {
        return Math.abs(tree1.getText().length() - tree2.getText().length()) < 100;
    }

    private double calculateTreeSimilarity(ParseTree tree1, ParseTree tree2) {
        String cacheKey = tree1.getText() + "::" + tree2.getText();
        if (similarityCache.containsKey(cacheKey)) return similarityCache.get(cacheKey);

        Node<StringNodeData> node1 = NodeUtil.parseTreeToNode(tree1);
        Node<StringNodeData> node2 = NodeUtil.parseTreeToNode(tree2);

        APTED<WeightedCostModel, StringNodeData> apted = new APTED<>(new WeightedCostModel());
        double distance = apted.computeEditDistance(node1, node2);

        int maxSize = Math.max(calculateSubtreeSize(node1), calculateSubtreeSize(node2));
        double similarity = 1.0 - (distance / maxSize);

        similarityCache.put(cacheKey, similarity);
        return similarity;
    }

    private int calculateSubtreeSize(Node<StringNodeData> node) {
        if (node == null) return 0;
        return 1 + node.getChildren().stream().mapToInt(this::calculateSubtreeSize).sum();
    }

    private ParseTree parseNodeToTree(Node<StringNodeData> node) {
        if (node == null) return null;
        return new CustomParseTree(node);
    }
}
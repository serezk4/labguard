package com.serezk4.core;

import com.serezk4.core.antlr4.JavaLexer;
import com.serezk4.core.antlr4.JavaParser;
import com.serezk4.core.apted.costmodel.StringUnitCostModel;
import com.serezk4.core.apted.distance.APTED;
import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

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
    public static final int TEST = 1;
    // todo temp
    public static final String CHECK_DIRECTORY_PATH = "/Users/serezk4/labguard/core/test/%d/target".formatted(TEST);

    private final Map<String, Double> similarityCache = new ConcurrentHashMap<>();

    public static void main(String... args) throws IOException {
        new Main().run("2281337", 6);
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
        Node<StringNodeData> node = parseTreeToNode(tree);
        cache.saveLab(isu, labNumber, fileName, node);

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
        if (similarityCache.containsKey(cacheKey)) {
            return similarityCache.get(cacheKey);
        }

        Node<StringNodeData> node1 = parseTreeToNode(tree1);
        Node<StringNodeData> node2 = parseTreeToNode(tree2);

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

    private Node<StringNodeData> parseTreeToNode(ParseTree tree) {
        if (tree == null || tree.getChildCount() == 0) return null;

        String type = tree.getClass().getSimpleName();
        Node<StringNodeData> node = new Node<>(new StringNodeData(type));

        for (int i = 0; i < tree.getChildCount(); i++) {
            Node<StringNodeData> childNode = parseTreeToNode(tree.getChild(i));
            Optional.ofNullable(childNode).ifPresent(node::addChild);
        }

        return node;
    }

    private static class WeightedCostModel extends StringUnitCostModel {

        @Override
        public float del(Node<StringNodeData> n) {
            return n.getNodeData().getLabel().startsWith("Method") ? 2.0f : 1.0f;
        }

        @Override
        public float ins(Node<StringNodeData> n) {
            return n.getNodeData().getLabel().startsWith("Method") ? 2.0f : 1.0f;
        }

        @Override
        public float ren(Node<StringNodeData> n1, Node<StringNodeData> n2) {
            if (n1.getNodeData().getLabel().equals(n2.getNodeData().getLabel())) return 0f;
            if (n1.getNodeData().getLabel().startsWith("Method") && n2.getNodeData().getLabel().startsWith("Method"))
                return 0.5f;
            return 1.0f;
        }
    }

    private ParseTree parseNodeToTree(Node<StringNodeData> node) {
        if (node == null) return null;
        return new CustomParseTree(node);
    }

    public class CustomParseTree implements ParseTree {
        private final Node<StringNodeData> node;

        public CustomParseTree(Node<StringNodeData> node) {
            this.node = node;
        }

        @Override
        public ParseTree getParent() {
            return null;
        }

        @Override
        public ParseTree getChild(int i) {
            if (i < 0 || i >= node.getChildren().size()) return null;
            return new CustomParseTree(node.getChildren().get(i));
        }

        @Override
        public void setParent(RuleContext ruleContext) {
            throw new UnsupportedOperationException("CustomParseTree does not support parents.");
        }

        @Override
        public int getChildCount() {
            return node.getChildren().size();
        }

        @Override
        public String getText() {
            return node.getNodeData().getLabel();
        }

        @Override
        public String toStringTree(Parser parser) {
            return "";
        }

        @Override
        public String toStringTree() {
            StringBuilder sb = new StringBuilder();
            sb.append(getText());
            if (getChildCount() > 0) {
                sb.append(" (");
                for (int i = 0; i < getChildCount(); i++) {
                    sb.append(getChild(i).toStringTree());
                    if (i < getChildCount() - 1) sb.append(", ");
                }
                sb.append(")");
            }
            return sb.toString();
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            throw new UnsupportedOperationException("CustomParseTree does not support visitors.");
        }

        @Override
        public ParseTree getPayload() {
            return this;
        }

        @Override
        public Interval getSourceInterval() {
            return Interval.INVALID;
        }

        @Override
        public String toString() {
            return getText();
        }
    }
}
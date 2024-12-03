package com.serezk4.core;

import com.serezk4.core.antlr4.JavaLexer;
import com.serezk4.core.antlr4.JavaParser;
import com.serezk4.core.apted.costmodel.WeightedCostModel;
import com.serezk4.core.apted.distance.APTED;
import com.serezk4.core.apted.node.Node;
import com.serezk4.core.apted.node.StringNodeData;
import com.serezk4.core.apted.util.NodeUtil;
import com.serezk4.core.lab.cache.LabStorage;
import com.serezk4.core.lab.model.Clazz;
import com.serezk4.core.lab.model.Lab;
import com.serezk4.core.lab.linter.CheckstyleAnalyzer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DiagnosticErrorListener;
import org.antlr.v4.runtime.atn.PredictionMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class Main {
    public static final int TEST = 2;
    public static final String CHECK_DIRECTORY_PATH = "/Users/serezk4/labguard/core/test/1/%d".formatted(TEST);

    public static void main(String... args) throws IOException {
        new Main().run("412934", 6);
    }

    private void run(
            final String isu,
            final int labNumber
    ) throws IOException {
        LabStorage cache = new LabStorage();

        List<Path> source = getFiles(CHECK_DIRECTORY_PATH);

        System.out.printf("Source: %d Java files found.%n", source.size());

        Lab cached = cache.loadLab(isu, labNumber);
        if (cached.clazzes() == null) {
            List<Clazz> clazzes = source.stream().map(this::parseFile).toList();
            cached = new Lab(isu, labNumber, clazzes);
            cache.save(cached);
            System.out.println("Lab saved to cache.");
            return;
        }

        AtomicLong plagiarismCount = new AtomicLong();

        Lab finalCached = cached;
        cached.clazzes().forEach(sourceTree -> {
            finalCached.clazzes().forEach(targetTree -> {
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
            System.out.println("report: ".concat(pmdReport.toString()));
            return new Clazz(path.getFileName().toString(), parser.compilationUnit(), code, pmdReport);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    private List<Path> getFiles(final String stringPath) throws IOException {
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

    private double calculateTreeSimilarity(Clazz clazz1, Clazz clazz2) {
        Node<StringNodeData> node1 = NodeUtil.parseTreeToNode(clazz1.tree());
        Node<StringNodeData> node2 = NodeUtil.parseTreeToNode(clazz2.tree());

        APTED<WeightedCostModel, StringNodeData> apted = new APTED<>(new WeightedCostModel());
        double distance = apted.computeEditDistance(node1, node2);

        int maxSize = Math.max(calculateSubtreeSize(node1), calculateSubtreeSize(node2));
        return 1.0 - (distance / maxSize);
    }

    private int calculateSubtreeSize(Node<StringNodeData> node) {
        if (node == null) return 0;
        return 1 + node.getChildren().stream().mapToInt(this::calculateSubtreeSize).sum();
    }
}
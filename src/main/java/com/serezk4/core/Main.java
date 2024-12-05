package com.serezk4.core;

import com.serezk4.core.lab.cache.LabStorage;
import com.serezk4.core.lab.check.Detector;
import com.serezk4.core.lab.check.apted.AptedCheck;
import com.serezk4.core.lab.check.graph.DataFlowGraphDetector;
import com.serezk4.core.lab.check.metric.CodeMetricsDetector;
import com.serezk4.core.lab.check.pattern.PatternMatchingDetector;
import com.serezk4.core.lab.check.tokenization.TokenizationCheck;
import com.serezk4.core.lab.model.Lab;
import com.serezk4.core.lab.model.Plagiarist;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Main {
    public static final int TEST = 2;
    public static final String CHECK_DIRECTORY_PATH = "/Users/serezk4/labguard/core/test/1/%d".formatted(TEST);

    public static final List<Detector> DETECTORS = List.of(
            new AptedCheck(),
            new DataFlowGraphDetector(),
            new CodeMetricsDetector(),
            new PatternMatchingDetector(),
            new TokenizationCheck()
    );

    public static void main(String... args) throws IOException {
        new Main().run("412934", 6);
    }

    private void run(
            final String isu,
            final int labNumber
    ) throws IOException {
        LabStorage cache = new LabStorage();

        Path sourcePath = Path.of(CHECK_DIRECTORY_PATH);

        Lab cached = cache.loadLab(isu, labNumber);
        if (cached.clazzes() == null) {
            cached = cache.load(isu, labNumber, sourcePath);
            cache.save(cached);
            return;
        }

        Lab targetLab = cached;

        AtomicLong plagiarismCount = new AtomicLong();
        cache.loadAllByLabNumber(labNumber).parallelStream()
                .filter(lab -> !lab.isu().equals(isu))
                .forEach(lab -> {
//                    List<Plagiarist> plagiarists = lab.clazzes().parallelStream()
//                            .map(clazz -> targetLab.clazzes().parallelStream()
//                                    .map(sourceTree -> new AbstractMap.SimpleEntry<>(sourceTree, DETECTORS.getFirst().detect(sourceTree, clazz)))
//                                    .filter(entry -> entry.getValue() > 0.7)
//                                    .max(Comparator.comparingDouble(AbstractMap.SimpleEntry::getValue))
//                                    .map(entry -> new Plagiarist(clazz, entry.getKey(), entry.getValue()))
//                                    .orElse(null))
//                            .filter(Objects::nonNull)
//                            .toList();

                    List<String> result = new ArrayList<>();
                    List<Plagiarist> plagiarists = lab.clazzes().parallelStream()
                            .map(clazz -> targetLab.clazzes().parallelStream()
                                    .peek(sourceTree -> {
                                        result.add(sourceTree.name() + ":" + clazz.name() + " = " + DETECTORS.stream().map(detector -> {
                                            double similarity = detector.detect(sourceTree, clazz);
                                            return String.format("%-10s %.2f%s | ", detector.getClass().getSimpleName(), similarity, similarity > 0.7 ? " <---" : "");
                                        }).collect(Collectors.joining()));
                                    })
                                    .map(sourceTree -> new AbstractMap.SimpleEntry<>(sourceTree, DETECTORS.getFirst().detect(sourceTree, clazz)))
                                    .filter(entry -> entry.getValue() > 0.7)
                                    .max(Comparator.comparingDouble(AbstractMap.SimpleEntry::getValue))
                                    .map(entry -> new Plagiarist(clazz, entry.getKey(), entry.getValue()))
                                    .orElse(null))
                            .filter(Objects::nonNull)
                            .toList();

                    plagiarismCount.addAndGet(plagiarists.size());

                    result.forEach(System.out::println);

                    if (plagiarists.isEmpty()) return;

                    System.out.println("\nLab (isu) : " + lab.isu());
                    System.out.println("Detected plagiarism cases: " + plagiarists.size());
                    System.out.println("----------------------------------------------------------------------------------------------------------");
                    System.out.printf("%-40s %-40s %-10s%n", "Target Class", "Source Class", "Similarity");
                    System.out.println("----------------------------------------------------------------------------------------------------------");

                    plagiarists.forEach(plagiarist -> System.out.printf(
                            "%-40s %-40s %-10.2f%n",
                            plagiarist.targetClazz().name(),
                            plagiarist.plagiarizedClazz().name(),
                            plagiarist.similarity()
                    ));
                });

        System.out.println("Plagiarism analysis completed.");
        System.out.println("Total plagiarism cases detected: " + plagiarismCount.get());
    }


}
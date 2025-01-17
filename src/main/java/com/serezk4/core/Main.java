package com.serezk4.core;

import com.serezk4.core.lab.check.Checker;
import com.serezk4.core.lab.check.apted.AptedCheck;
import com.serezk4.core.lab.model.*;
import com.serezk4.core.lab.storage.LabStorage;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.serezk4.core.html.HtmlGenerator.generateHtmlReport;

public class Main {
    private static final List<Checker> CHECKERS = List.of(new AptedCheck());
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final BufferedWriter consoleWriter = new BufferedWriter(new OutputStreamWriter(System.out));

    public static void main(final String... args) throws IOException {
        if (args.length != 3
                || !args[0].matches("\\d{6}")
                || !args[1].matches("\\d+")
                || !Files.exists(Path.of(args[2]))
        ) {
            System.out.println("Use format: <isu> <labNumber> <path>");
            return;
        }

        new Main().run(args[0], Integer.parseInt(args[1]), args[2]);
    }

    private void run(
            final String isu,
            final int labNumber,
            final String path
    ) throws IOException {
        final long startOverall = System.nanoTime();

        final LabStorage cache = new LabStorage();
        final Path sourcePath = Path.of(path);

        final Lab targetLab = loadAndCacheLab(cache, isu, labNumber, sourcePath);
        final List<Lab> labs = cache.loadAllByLabNumber(labNumber).stream()
                .filter(lab -> !lab.isu().equals(isu))
                .toList();

        final Map<String, List<Plagiarist>> results = new ConcurrentHashMap<>();
        final Map<Integer, List<Clazz>> targetGroupedByLength = targetLab.clazzes().stream()
                .collect(Collectors.groupingBy(this::selectGroupKey));

        CompletableFuture.allOf(labs.stream().map(lab -> CompletableFuture.runAsync(() -> {
            List<Plagiarist> plagiarists = lab.clazzes().stream()
                    .map(clazz -> findPlagiarist(clazz, targetGroupedByLength))
                    .flatMap(Optional::stream)
                    .toList();

            results.put(lab.isu(), plagiarists);
        }, EXECUTOR)).toArray(CompletableFuture[]::new)).join();

        generateHtmlReport(isu, labNumber, labs, results);

        final long endOverall = System.nanoTime();
        consoleWriter
                .append("Total execution time: ")
                .append(String.format("%.2f seconds\n", (endOverall - startOverall) / 1e9))
                .flush();
        consoleWriter.close();
    }

    private Lab loadAndCacheLab(LabStorage cache, String isu, int labNumber, Path sourcePath) throws IOException {
        Lab targetLab = cache.loadLab(isu, labNumber);
        if (targetLab.clazzes() != null) return targetLab;

        System.out.println("Lab not found in cache, parsing files...");
        targetLab = cache.load(isu, labNumber, sourcePath);
        cache.save(targetLab);
        return targetLab;
    }

    private Optional<Plagiarist> findPlagiarist(Clazz clazz, Map<Integer, List<Clazz>> targetGroupedByLength) {
        int groupKey = selectGroupKey(clazz);

        System.out.println("analyze");
        List<PlagiaristMethod> plagiaristMethods = Stream.of(groupKey - 1, groupKey, groupKey + 1)
                .filter(targetGroupedByLength::containsKey)
                .flatMap(key -> targetGroupedByLength.get(key).stream())
                .flatMap(targetClazz -> compareMethods(clazz, targetClazz).stream())
                .toList();

        if (plagiaristMethods.isEmpty()) return Optional.empty();
        return Optional.of(new Plagiarist(clazz, targetGroupedByLength.get(groupKey).getFirst(), plagiaristMethods));
    }

    private List<PlagiaristMethod> compareMethods(Clazz sourceClazz, Clazz targetClazz) {
        return sourceClazz.methods().stream()
                .flatMap(sourceMethod -> targetClazz.methods().stream()
                        .map(targetMethod -> {
                            double similarity = detectCached(CHECKERS.getFirst(), sourceMethod, targetMethod);
                            return similarity > 0.7
                                    ? new PlagiaristMethod(sourceMethod, targetMethod, similarity)
                                    : null;
                        })
                        .filter(Objects::nonNull))
                .toList();
    }

    private final Map<String, Double> similarityCache = new ConcurrentHashMap<>();

    private double detectCached(Checker checker, Method sourceMethod, Method targetMethod) {
        String key = sourceMethod.hashCode() + ":" + targetMethod.hashCode();
        return similarityCache.computeIfAbsent(key, _ -> checker.detect(sourceMethod, targetMethod));
    }

    private int selectGroupKey(Clazz clazz) {
        int length = clazz.source().length();
        if (length < 500) return 0;
        else if (length < 1000) return 1;
        else return 2;
    }
}

package com.serezk4.core;

import com.serezk4.core.lab.check.Checker;
import com.serezk4.core.lab.check.apted.AptedCheck;
import com.serezk4.core.lab.model.*;
import com.serezk4.core.lab.storage.LabStorage;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.serezk4.core.html.HtmlGenerator.generateHtmlReport;

public class Main {
    private static final List<Checker> CHECKERS = List.of(new AptedCheck());
    private static final ExecutorService VIRTUAL_EXECUTOR =
            Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());

    private static final VarHandle RESULTS_UPDATER;

    static {
        try {
            RESULTS_UPDATER = MethodHandles.lookup()
                    .findStaticVarHandle(Main.class, "results", ConcurrentHashMap.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to initialize VarHandle for results", e);
        }
    }

    private static final ConcurrentHashMap<String, List<Plagiarist>> results = new ConcurrentHashMap<>();
    private final Map<String, Double> similarityCache = new ConcurrentHashMap<>();

    private final PrintWriter consoleWriter = new PrintWriter(new OutputStreamWriter(System.out), true);

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
        VIRTUAL_EXECUTOR.shutdown();
    }

    private void run(
            final String isu,
            final int labNumber,
            final String path
    ) throws IOException {
        final Instant startOverall = Instant.now();

        final LabStorage cache = new LabStorage();
        final Path sourcePath = Path.of(path);

        final Lab targetLab = loadAndCacheLab(cache, isu, labNumber, sourcePath);

        final List<Lab> labs = cache.loadAllByLabNumber(labNumber)
                .stream()
                .filter(lab -> !lab.isu().equals(isu))
                .toList();

        final Map<Integer, List<Clazz>> targetGroupedByLength = targetLab.clazzes()
                .stream()
                .collect(Collectors.groupingBy(this::selectGroupKey));

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            for (Lab lab : labs) {
                scope.fork(() -> processLab(lab, targetGroupedByLength));
            }
            scope.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            scope.fork(() -> {
                generateHtmlReport(isu, labNumber, labs, results);
                return null;
            });
            scope.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        final Instant endOverall = Instant.now();
        Duration duration = Duration.between(startOverall, endOverall);

        consoleWriter.println("Total execution time: " + duration.toMillis() + " ms");
    }

    private Void processLab(Lab lab, Map<Integer, List<Clazz>> targetGroupedByLength) {
        List<Plagiarist> foundPlagiarists = lab.clazzes()
                .stream()
                .map(clazz -> findPlagiarist(clazz, targetGroupedByLength))
                .flatMap(Optional::stream)
                .toList();

        if (!foundPlagiarists.isEmpty()) {
            RESULTS_UPDATER.setRelease(results, lab.isu(), foundPlagiarists);
        }
        return null;
    }

    private Lab loadAndCacheLab(LabStorage cache, String isu, int labNumber, Path sourcePath) throws IOException {
        Lab targetLab = cache.loadLab(isu, labNumber);
        if (targetLab.clazzes() != null) return targetLab;
        targetLab = cache.load(isu, labNumber, sourcePath);
        cache.save(targetLab);
        return targetLab;
    }

    private Optional<Plagiarist> findPlagiarist(Clazz clazz, Map<Integer, List<Clazz>> targetGroupedByLength) {
        final int groupKey = selectGroupKey(clazz);

        List<PlagiaristMethod> plagiaristMethods = Stream.of(groupKey - 1, groupKey, groupKey + 1)
                .filter(targetGroupedByLength::containsKey)
                .flatMap(key -> targetGroupedByLength.get(key).stream())
                .flatMap(targetClazz -> compareMethods(clazz, targetClazz).stream())
                .toList();

        System.out.println(plagiaristMethods.size());

        if (plagiaristMethods.isEmpty()) return Optional.empty();
        Clazz representative = targetGroupedByLength.getOrDefault(groupKey, List.of()).stream()
                .findFirst()
                .orElse(clazz);
        return Optional.of(new Plagiarist(clazz, representative, plagiaristMethods));
    }

    private List<PlagiaristMethod> compareMethods(Clazz sourceClazz, Clazz targetClazz) {
        Checker checker = CHECKERS.getFirst();
        List<Method> sourceMethods = new ArrayList<>(sourceClazz.methods());
        List<Method> targetMethods = new ArrayList<>(targetClazz.methods());

        sourceMethods.sort(Comparator.comparingInt(m -> m.source().length()));
        targetMethods.sort(Comparator.comparingInt(m -> m.source().length()));

        List<PlagiaristMethod> detectedPlagiarists = Collections.synchronizedList(new ArrayList<>());

        List<Method> suspectedMethods = Collections.synchronizedList(new ArrayList<>());

        sourceMethods.parallelStream().forEach(sourceMethod -> {
            targetMethods.parallelStream().forEach(targetMethod -> {
                if (sourceMethod.equals(targetMethod)) return;
                if (suspectedMethods.contains(targetMethod)) return;

                double similarity = detectCached(checker, sourceMethod, targetMethod);
                if (similarity > 0.7) {
                    detectedPlagiarists.add(new PlagiaristMethod(sourceMethod, targetMethod, similarity));
                    suspectedMethods.add(targetMethod);
                }
            });
        });

        targetMethods.removeAll(suspectedMethods);

        return detectedPlagiarists;
    }

    private double detectCached(Checker checker, Method sourceMethod, Method targetMethod) {
        String key = sourceMethod.hashCode() + ":" + targetMethod.hashCode();
        return similarityCache.computeIfAbsent(key, _ -> checker.detect(sourceMethod, targetMethod));
    }

    private int selectGroupKey(Clazz clazz) {
        int length = clazz.source().length();
        return length / 200;
    }
}

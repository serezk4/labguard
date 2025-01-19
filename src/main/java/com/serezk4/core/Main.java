package com.serezk4.core;

import com.serezk4.core.lab.check.Checker;
import com.serezk4.core.lab.check.apted.AptedCheck;
import com.serezk4.core.lab.model.Clazz;
import com.serezk4.core.lab.model.Lab;
import com.serezk4.core.lab.model.Plagiarist;
import com.serezk4.core.lab.storage.LabStorage;
import com.serezk4.core.lab.util.GroupKeySelector;

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

/**
 * Entry point for the application that performs lab plagiarism detection.
 *
 * <p>
 * The application loads lab data from a given path, compares the target lab against
 * a collection of stored labs, and generates an HTML report with the plagiarism results.
 * The comparison is based on similarity metrics provided by {@link Checker} implementations.
 * </p>
 *
 * <p>
 * Key responsibilities:
 * <ul>
 *     <li>Validate and parse input arguments.</li>
 *     <li>Load and cache lab data for efficiency.</li>
 *     <li>Perform plagiarism detection using a multi-threaded approach.</li>
 *     <li>Generate an HTML report summarizing the results.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Input arguments:
 * <ol>
 *     <li><b>isu:</b> ISU identifier of the student whose lab is being analyzed (6 digits).</li>
 *     <li><b>labNumber:</b> The number of the lab to analyze.</li>
 *     <li><b>path:</b> Path to the directory containing the lab source files.</li>
 * </ol>
 * </p>
 *
 * @author serezk4
 * @version 1.0
 * @since 1.0
 */
public class Main {
    private static final List<Checker> CHECKERS = List.of(new AptedCheck());
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final BufferedWriter consoleWriter = new BufferedWriter(new OutputStreamWriter(System.out));
    private final GroupKeySelector groupKeySelector = new GroupKeySelector(
            0,
            120 * 500,
            120 * 50
    );

    /**
     * Main method that initializes the application and validates input arguments.
     *
     * <p>
     * The method verifies that the provided arguments meet the required format:
     * <ul>
     *     <li><b>ISU</b>: A 6-digit unique student identifier.</li>
     *     <li><b>Lab Number</b>: A positive integer representing the lab to analyze.</li>
     *     <li><b>Path</b>: A valid file system path to the directory containing the lab's source files.</li>
     * </ul>
     * If the arguments are invalid, the application prints a usage message and terminates.
     * </p>
     *
     * @param args command-line arguments in the format: `<isu> <labNumber> <path>`
     * @throws IOException if an I/O error occurs during execution
     */
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

    /**
     * Core execution method that orchestrates the plagiarism detection process.
     *
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li>Loads and caches the target lab from the specified path.</li>
     *     <li>Loads all labs with the same lab number, excluding the target lab.</li>
     *     <li>Performs parallel plagiarism checks for each loaded lab against the target lab.</li>
     *     <li>Generates an HTML report summarizing the results.</li>
     * </ol>
     * </p>
     *
     * @param isu       ISU identifier of the target lab owner (must be 6 digits)
     * @param labNumber Number of the lab to analyze (must be positive)
     * @param path      Path to the lab files (must exist and be readable)
     * @throws IOException if an error occurs while reading or writing data
     */
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
                .collect(Collectors.groupingBy(groupKeySelector::selectGroupKey));

        CompletableFuture.allOf(labs.stream().map(lab -> CompletableFuture.runAsync(() -> {
            List<Plagiarist> plagiarists = lab.clazzes().stream()
                    .map(clazz -> findPlagiarist(clazz, targetGroupedByLength))
                    .flatMap(Optional::stream)
                    .toList();

            results.put(lab.isu(), plagiarists);
        }, EXECUTOR)).toArray(CompletableFuture[]::new)).join();

        generateHtmlReport(isu, labNumber, labs, targetLab, results);

        final long endOverall = System.nanoTime();
        consoleWriter
                .append("Total execution time: ")
                .append(String.format("%.2f seconds\n", (endOverall - startOverall) / 1e9))
                .flush();
        consoleWriter.close();
    }

    /**
     * Loads the target lab from the cache or the specified path if not already cached.
     *
     * <p>
     * This method first attempts to retrieve the lab from the {@link LabStorage} cache. If the lab is not found,
     * it reads the lab's source files from the provided path, processes them into a {@link Lab} object, and saves
     * the object back to the cache for future use.
     * </p>
     *
     * @param cache     The {@link LabStorage} instance managing cached lab data
     * @param isu       ISU identifier of the target lab owner (6 digits)
     * @param labNumber Number of the lab to load
     * @param sourcePath Path to the directory containing the lab's source files
     * @return the loaded {@link Lab} object representing the target lab
     */
    private Lab loadAndCacheLab(
            final LabStorage cache,
            final String isu,
            final int labNumber,
            final Path sourcePath
    ) {
        Lab targetLab = cache.loadLab(isu, labNumber);
        if (targetLab.clazzes() != null) return targetLab;

        targetLab = cache.load(isu, labNumber, sourcePath);
        cache.save(targetLab);
        return targetLab;
    }

    /**
     * Detects potential plagiarism between a single source class and a set of target classes grouped by length.
     *
     * <p>
     * The method first determines the group key of the source class based on its length. It then compares the source
     * class against all target classes within the same group, as well as adjacent groups, filtering out classes with
     * significant length differences. If a similarity score above 0.7 is detected, a {@link Plagiarist} object is
     * created to represent the plagiarism instance.
     * </p>
     *
     * @param clazz                 The source {@link Clazz} to analyze
     * @param targetGroupedByLength A map of target {@link Clazz} objects grouped by their length
     * @return an {@link Optional} containing a {@link Plagiarist} object if plagiarism is detected,
     *         or an empty {@link Optional} otherwise
     */
    private Optional<Plagiarist> findPlagiarist(
            final Clazz clazz,
            final Map<Integer, List<Clazz>> targetGroupedByLength
    ) {
        final int groupKey = groupKeySelector.selectGroupKey(clazz);
        final int sourceLength = clazz.source().length();

        final int lengthThreshold = 2000;
        final double similarityThreshold = 0.61;

        List<Clazz> potentialTargets = Stream.of(groupKey - 1, groupKey, groupKey + 1)
                .map(targetGroupedByLength::get)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(target -> Math.abs(sourceLength - target.source().length()) <= lengthThreshold)
                .toList();

        return potentialTargets.parallelStream()
                .map(target -> {
                    double similarity = detectCached(CHECKERS.getFirst(), clazz, target);
                    return similarity > similarityThreshold ? new Plagiarist(clazz, target, similarity) : null;
                })
                .filter(Objects::nonNull)
                .findFirst();
    }

    private final Map<String, Double> similarityCache = new ConcurrentHashMap<>();

    /**
     * Computes the similarity score between two classes using a {@link Checker}, with caching for optimization.
     *
     * <p>
     * The similarity score is calculated only once for a given pair of classes. Subsequent calls with the same
     * pair return the cached result. This ensures that repeated calculations are avoided, improving overall
     * performance during the plagiarism detection process.
     * </p>
     *
     * @param checker The {@link Checker} instance used to compute the similarity score
     * @param source  The source {@link Clazz}
     * @param target  The target {@link Clazz}
     * @return the similarity score between the source and target classes, ranging from 0.0 (completely different)
     *         to 1.0 (identical)
     */
    private double detectCached(
            final Checker checker,
            final Clazz source,
            final Clazz target
    ) {
        return similarityCache.computeIfAbsent(
                String.join(":", String.valueOf(source.hashCode()), String.valueOf(target.hashCode())), _ ->
                        checker.detect(source, target)
        );
    }
}

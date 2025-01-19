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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Manages the storage, loading, and caching of lab data.
 *
 * <p>
 * The {@code LabStorage} class provides methods to save labs to the file system, load labs from storage,
 * and parse Java source files into structured representations. It uses a JSON-based caching mechanism for
 * efficient data retrieval and normalization techniques to preprocess Java code for further analysis.
 * </p>
 *
 * Key features:
 * <ul>
 *     <li>Save and load labs from a predefined storage root.</li>
 *     <li>Parse Java files into {@link Clazz} objects, applying code normalization.</li>
 *     <li>Retrieve all labs for a specific lab number.</li>
 *     <li>Handle concurrent file access and caching of parsed files.</li>
 * </ul>
 *
 * <p>
 * This implementation is thread-safe and optimized for parallel processing using {@link Executors}.
 * </p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * LabStorage storage = new LabStorage();
 * Lab lab = storage.load("123456", 1, Path.of("/path/to/lab"));
 * storage.save(lab);
 * }</pre>
 *
 * @author serezk4
 * @version 1.0
 * @since 1.0
 */
public final class LabStorage {
    private static final Path CACHE_ROOT = Paths.get("/Users/serezk4/labguard/core/lab_cache");

    private static final Pattern COMMENT_PATTERN = Pattern.compile("(?s)/\\*.*?\\*/|//.*");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+\\b");
    private static final Pattern STRING_PATTERN = Pattern.compile("\".*?\"");

    private final Gson gson = new GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .create();

    private final Map<Path, Clazz> parsedFileCache = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code LabStorage} instance and ensures the storage root directory exists.
     *
     * @throws IOException if an error occurs while creating the storage root directory
     */
    public LabStorage() throws IOException {
        Files.createDirectories(CACHE_ROOT);
    }

    /**
     * Saves the specified lab to the storage.
     *
     * <p>
     * The lab is serialized into JSON files, where each {@link Clazz} is stored as a separate file.
     * The storage path is determined based on the lab's ISU and lab number.
     * </p>
     *
     * @param lab the lab to save
     */
    public void save(final Lab lab) {
        Path labPath = getLabPath(lab.isu(), lab.labNumber());

        try {
            Files.createDirectories(labPath);
        } catch (IOException e) {
            System.err.println("Error creating lab directory: " + e.getMessage());
            return;
        }

        lab.clazzes().parallelStream().forEach(clazz -> saveNode(labPath, clazz));
    }


    private void saveNode(
            final Path path,
            final Clazz clazz
    ) {
        Path outputPath = path.resolve(clazz.name().concat(".json"));
        try (var writer = Files.newBufferedWriter(outputPath)) {
            gson.toJson(clazz.toStoredTree(), writer);
        } catch (IOException e) {
            System.err.println("Error saving node: " + e.getMessage());
        }
    }

    /**
     * Loads all labs with the specified lab number from the storage.
     *
     * <p>
     * This method scans the storage root directory for all ISU identifiers and attempts to load
     * labs matching the given lab number. It skips labs without any associated classes.
     * </p>
     *
     * @param labNumber the lab number to load
     * @return a list of labs with the specified lab number, sorted by ISU
     */
    public List<Lab> loadAllByLabNumber(final int labNumber) {
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

    /**
     * Loads a lab from the specified path and parses its Java files.
     *
     * <p>
     * This method reads all Java files in the provided path and processes them into {@link Clazz} objects.
     * The resulting {@link Lab} contains the parsed classes and metadata.
     * </p>
     *
     * @param isu       the ISU identifier of the lab owner
     * @param labNumber the number of the lab to load
     * @param path      the path to the directory containing the lab's source files
     * @return a {@link Lab} object representing the loaded lab
     */
    public Lab load(
            final String isu,
            final int labNumber,
            final Path path
    ) {
        return new Lab(isu, labNumber, getFiles(path.toString()).stream()
                .map(this::parseFile)
                .filter(Objects::nonNull)
                .toList());
    }

    /**
     * Parses a Java source file into a {@link Clazz} object.
     *
     * <p>
     * The parsing process includes:
     * <ul>
     *     <li>Code normalization (removal of comments, numbers, and string literals).</li>
     *     <li>Lexical and syntactical analysis using ANTLR.</li>
     *     <li>Static code analysis using Checkstyle.</li>
     * </ul>
     * The parsed result is cached for efficiency.
     * </p>
     *
     * @param path the path to the Java source file
     * @return a {@link Clazz} object representing the parsed file, or {@code null} if an error occurs
     */
    private Clazz parseFile(final Path path) {
        return parsedFileCache.computeIfAbsent(path, p -> {
            try {
                String code = Files.readString(p);
                String normalizedCode = normalize(code);

                CharStream charStream = CharStreams.fromString(normalizedCode);
                JavaLexer lexer = new JavaLexer(charStream);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                JavaParser parser = new JavaParser(tokens) {{
                    getInterpreter().setPredictionMode(PredictionMode.SLL);
                    addErrorListener(new DiagnosticErrorListener());
                }};

                List<String> pmdReport = CheckstyleAnalyzer.getInstance().analyzeCode(p);
                return new Clazz(p.getFileName().toString(), parser.compilationUnit(), code, normalizedCode, pmdReport);
            } catch (IOException e) {
                System.err.println("Error parsing file: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Retrieves all Java files from the specified directory path.
     *
     * <p>
     * This method recursively scans the directory and returns all files with the `.java` extension.
     * </p>
     *
     * @param stringPath the directory path as a string
     * @return a list of paths to Java files in the directory
     */
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

    /**
     * Normalizes Java code by removing comments, replacing numbers with "0", and replacing strings with a placeholder.
     *
     * <p>
     * This method is used to simplify the code representation for more efficient analysis.
     * </p>
     *
     * @param code the raw Java code to normalize
     * @return the normalized Java code
     */
    public static String normalize(final String code) {
        return STRING_PATTERN.matcher(
                NUMBER_PATTERN.matcher(
                        COMMENT_PATTERN.matcher(code).replaceAll("")
                ).replaceAll("0")
        ).replaceAll("\"stringLiteral\"");
    }

    public Lab loadLab(
            final String isu,
            final int labNumber
    ) {
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

    private Optional<Clazz> loadNode(final Path file) {
        try (var reader = Files.newBufferedReader(file)) {
            StoredClazz storedClazz = gson.fromJson(reader, StoredClazz.class);
            return Optional.of(storedClazz.toClazz());
        } catch (IOException e) {
            System.err.println("Error loading node: " + e.getMessage());
            return Optional.empty();
        }
    }

    private Path getLabPath(
            final String isu,
            final int labNumber
    ) {
        return CACHE_ROOT.resolve(isu).resolve(String.valueOf(labNumber));
    }
}

package com.serezk4.core.lab.analyze.checkstyle;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Singleton class for analyzing Java code using Checkstyle.
 *
 * <p>
 * The {@code CheckstyleAnalyzer} provides a mechanism for running Checkstyle analysis
 * on Java source files. It leverages a configured {@link Checker} instance with a
 * predefined configuration file located at {@code config/checkstyle/checkstyle.xml}.
 * </p>
 *
 * <p>
 * The analyzer is implemented as a singleton to ensure that only one {@link Checker}
 * instance is initialized during the application lifecycle, improving performance and
 * avoiding redundant configurations.
 * </p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * CheckstyleAnalyzer analyzer = CheckstyleAnalyzer.getInstance();
 * List<String> issues = analyzer.analyzeCode(Path.of("src/main/java/MyClass.java"));
 * issues.forEach(System.out::println);
 * }</pre>
 *
 * @see Checker
 * @see com.puppycrawl.tools.checkstyle.api.AuditListener
 *
 * @author serez
 * @version 1.0
 * @since 1.0
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CheckstyleAnalyzer {

    private static CheckstyleAnalyzer instance;
    private Checker checker;

    /**
     * Initializes the {@code CheckstyleAnalyzer} with a configured {@link Checker} instance.
     *
     * <p>
     * The configuration file for Checkstyle is loaded from {@code config/checkstyle/checkstyle.xml},
     * and additional properties can be provided via {@link PropertiesExpander}.
     * </p>
     *
     * @throws CheckstyleException if an error occurs during the initialization of the {@link Checker}
     */
    private CheckstyleAnalyzer() throws CheckstyleException {
        this.checker = new Checker() {{
            setModuleClassLoader(Checker.class.getClassLoader());
            configure(ConfigurationLoader.loadConfiguration(
                    "config/checkstyle/checkstyle.xml",
                    new PropertiesExpander(new Properties() {{
                        setProperty("config_loc", "config/checkstyle");
                    }})
            ));
        }};
    }

    /**
     * Retrieves the singleton instance of {@code CheckstyleAnalyzer}.
     *
     * <p>
     * If the instance is not already initialized, it will be created. Subsequent calls
     * will return the same instance.
     * </p>
     *
     * @return the singleton instance of {@code CheckstyleAnalyzer}
     * @throws RuntimeException if an error occurs during initialization
     */
    public static CheckstyleAnalyzer getInstance() {
        try {
            return instance == null ? instance = new CheckstyleAnalyzer() : instance;
        } catch (CheckstyleException e) {
            throw new RuntimeException("Error initializing CheckstyleAnalyzer", e);
        }
    }

    /**
     * Analyzes the specified Java file using Checkstyle and returns the list of issues found.
     *
     * <p>
     * The method processes the provided {@link Path} to a Java source file using the configured
     * {@link Checker}. Any issues found during the analysis are returned as a list of strings.
     * </p>
     *
     * @param path the {@link Path} to the Java file to be analyzed
     * @return a list of strings representing Checkstyle issues, or an error message if analysis fails
     */
    public List<String> analyzeCode(Path path) {
        return new ArrayList<>() {{
            try (var _ = new CustomListener(this, checker)) {
                checker.process(Collections.singletonList(path.toFile()));
            } catch (Exception e) {
                add("Failed to analyze: %s".formatted(e.getMessage()));
            }
        }};
    }
}

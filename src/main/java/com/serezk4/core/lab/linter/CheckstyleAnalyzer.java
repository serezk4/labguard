package com.serezk4.core.lab.linter;

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

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CheckstyleAnalyzer {
    static CheckstyleAnalyzer instance;
    Checker checker;

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

    public static CheckstyleAnalyzer getInstance() {
        try {
            return instance == null ? instance = new CheckstyleAnalyzer() : instance;
        } catch (CheckstyleException e) {
            throw new RuntimeException("Error initializing CheckstyleAnalyzer", e);
        }
    }

    public List<String> analyzeCode(Path path) {
        return new ArrayList<>(){{
            try (var _ = new CustomListener(this, checker)) {
                checker.process(Collections.singletonList(path.toFile()));
            } catch (Exception e) {
                add("Failed to analyze: %s".formatted(e.getMessage()));
            }
        }};
    }
}
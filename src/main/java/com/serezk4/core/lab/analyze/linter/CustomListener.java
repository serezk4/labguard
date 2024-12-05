package com.serezk4.core.lab.analyze.linter;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class CustomListener implements AutoCloseable, AuditListener {
    List<String> results;
    Checker checker;

    public CustomListener(List<String> results, Checker checker) {
        this.results = results;
        this.checker = checker;

        this.checker.addListener(this);
    }

    @Override
    public void auditStarted(AuditEvent event) {
    }

    @Override
    public void auditFinished(AuditEvent event) {
    }

    @Override
    public void fileStarted(AuditEvent event) {
    }

    @Override
    public void fileFinished(AuditEvent event) {
    }

    @Override
    public void addError(AuditEvent event) {
        results.add(String.format("Violation: %s at line %d: %s",
                event.getSourceName(),
                event.getLine(),
                event.getMessage()));
    }

    @Override
    public void addException(AuditEvent event, Throwable throwable) {
        results.add("Error: " + throwable.getMessage());
    }

    @Override
    public void close() throws Exception {
        checker.removeListener(this);
    }
}

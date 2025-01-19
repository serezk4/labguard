package com.serezk4.core.lab.analyze.checkstyle;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomListener implements AutoCloseable, AuditListener {
    List<String> results;
    Checker checker;

    public CustomListener(List<String> results, Checker checker) {
        this.results = results;
        this.checker = checker;

        this.checker.addListener(this);
    }

    @Override
    public void auditStarted(AuditEvent ignored) {
    }

    @Override
    public void auditFinished(AuditEvent ignored) {
    }

    @Override
    public void fileStarted(AuditEvent ignored) {
    }

    @Override
    public void fileFinished(AuditEvent ignored) {

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
    public void close() {
        checker.removeListener(this);
    }
}

package com.serezk4.core.lab.linter;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class CustomListener implements AuditListener {
    List<String> results;

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
}

package io.ballerina.stdlib.websub;

import io.ballerina.tools.diagnostics.DiagnosticSeverity;

/**
 * {@code WebSubDiagnosticCodes} is used to hold websub related diagnostic codes.
 */
public enum WebSubDiagnosticCodes {
    WEBSUB_100("WEBSUB_100", "checkpanic detected, use check",
            DiagnosticSeverity.WARNING),
    WEBSUB_101("WEBSUB_101", "Could not find the required service annotations",
            DiagnosticSeverity.ERROR),
    WEBSUB_102("WEBSUB_102", "websub:SubscriberService should only implement remote methods",
            DiagnosticSeverity.ERROR),
    WEBSUB_103("WEBSUB_103", "websub:SubscriberService should implement 'onEventNotification'",
            DiagnosticSeverity.ERROR);

    private String code;
    private String description;
    private DiagnosticSeverity severity;

    WebSubDiagnosticCodes(String code,
                          String description,
                          DiagnosticSeverity severity) {
        this.code = code;
        this.description = description;
        this.severity = severity;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public DiagnosticSeverity getSeverity() {
        return severity;
    }
}

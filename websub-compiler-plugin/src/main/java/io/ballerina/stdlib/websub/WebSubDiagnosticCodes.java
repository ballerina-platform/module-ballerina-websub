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
    WEBSUB_103("WEBSUB_103", "websub:SubscriberService should implement onEventNotification method",
            DiagnosticSeverity.ERROR),
    WEBSUB_104("WEBSUB_104", "{0} method is not allowed in websub:SubscriberService declaration",
            DiagnosticSeverity.ERROR),
    WEBSUB_105("WEBSUB_105", "{0} type parameters not allowed for {1} method",
               DiagnosticSeverity.ERROR),
    WEBSUB_106("WEBSUB_106", "{0} method should have parameters of following {1} types",
            DiagnosticSeverity.ERROR),
    WEBSUB_107("WEBSUB_107", "{0} type is not allowed to be returned from {1} method",
            DiagnosticSeverity.ERROR),
    WEBSUB_108("WEBSUB_108", "{0} method should return {1} types", DiagnosticSeverity.ERROR);

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

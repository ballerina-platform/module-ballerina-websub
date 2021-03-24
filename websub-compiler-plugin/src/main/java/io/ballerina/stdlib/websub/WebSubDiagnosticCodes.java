package io.ballerina.stdlib.websub;

public enum WebSubDiagnosticCodes {
    WEBSUB_100("WEBSUB_100", "No methods declared"),
    WEBSUB_101("WEBSUB_101", "Could not find the required method onEventNotification");

    private String code;
    private String description;

    WebSubDiagnosticCodes(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

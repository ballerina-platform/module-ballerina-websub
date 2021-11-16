/*
 * Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.websub;

import io.ballerina.tools.diagnostics.DiagnosticSeverity;

/**
 * {@code WebSubDiagnosticCodes} is used to hold websub related diagnostic codes.
 */
public enum WebSubDiagnosticCodes {
    WEBSUB_100("WEBSUB_100", "checkpanic detected, use check",
            DiagnosticSeverity.WARNING),
    WEBSUB_101("WEBSUB_101",
            "Subscriber service should be annotated with websub:SubscriberServiceConfig",
            DiagnosticSeverity.ERROR),
    WEBSUB_102("WEBSUB_102", "{0} method should be declared as a remote method",
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
    WEBSUB_108("WEBSUB_108", "{0} method should return {1} types", DiagnosticSeverity.ERROR),
    WEBSUB_109("WEBSUB_109",
            "websub:Listener should only take either http:Listener or websub:ListenerConfiguration",
            DiagnosticSeverity.ERROR),
    WEBSUB_200("WEBSUB_200",
            "Error occurred while generating unique service path for websub:SubscriberService : {0}",
            DiagnosticSeverity.ERROR),
    WEBSUB_201("WEBSUB_201",
            "Error occurred while packaging generated resources : {0}",
            DiagnosticSeverity.ERROR);

    private final String code;
    private final String description;
    private final DiagnosticSeverity severity;

    WebSubDiagnosticCodes(String code, String description, DiagnosticSeverity severity) {
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

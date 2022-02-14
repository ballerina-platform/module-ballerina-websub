/*
 * Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.websub.task.service.path;

import io.ballerina.projects.Package;
import io.ballerina.projects.plugins.GeneratorTask;
import io.ballerina.projects.plugins.SourceGeneratorContext;
import io.ballerina.stdlib.websub.WebSubDiagnosticCodes;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;

import java.nio.charset.Charset;
import java.util.Optional;

import static io.ballerina.stdlib.websub.task.service.path.ServicePathContextHandler.getContextHandler;

/**
 * {@code ServiceInfoResourceGeneratorTask} add the service-info resource to compilation context.
 */
public class ServiceInfoResourceGeneratorTask implements GeneratorTask<SourceGeneratorContext> {
    private static final String SERVICE_INFO_FILE = "service-info.csv";
    private static final String SERVICE_INFO_ENTRY_FORMAT = "%d, %s%n";

    @Override
    public void generate(SourceGeneratorContext context) {
        boolean erroneousCompilation = context.compilation().diagnosticResult()
                .diagnostics().stream()
                .anyMatch(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()));
        // if the compilation already contains any error, do not proceed
        if (erroneousCompilation) {
            return;
        }

        try {
            Package currentPackage = context.currentPackage();
            Optional<ServicePathContext> servicePathContextOpt = getContextHandler()
                    .retrieveContext(currentPackage.packageId(), currentPackage.project().sourceRoot());
            // if the shared service-path generation context not found, do not proceed
            if (servicePathContextOpt.isEmpty()) {
                return;
            }

            ServicePathContext servicePathContext = servicePathContextOpt.get();
            // if generated service-path information is empty, do not proceed
            if (servicePathContext.getServicePathDetails().isEmpty()) {
                return;
            }
            String serviceInfo = servicePathContext.getServicePathDetails().stream()
                    .map(info -> String.format(SERVICE_INFO_ENTRY_FORMAT, info.getServiceId(), info.getServicePath()))
                    .reduce("", String::concat);
            context.addResourceFile(serviceInfo.getBytes(Charset.defaultCharset()), SERVICE_INFO_FILE);
        } catch (Exception e) {
            WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_201;
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                    errorCode.getCode(), errorCode.getDescription(), errorCode.getSeverity());
            Diagnostic diagnostic = DiagnosticFactory.createDiagnostic(
                    diagnosticInfo, new NullLocation(), e.getMessage());
            context.reportDiagnostic(diagnostic);
        }
    }

    private static class NullLocation implements Location {
        @Override
        public LineRange lineRange() {
            LinePosition from = LinePosition.from(0, 0);
            return LineRange.from("", from, from);
        }

        @Override
        public TextRange textRange() {
            return TextRange.from(0, 0);
        }
    }
}

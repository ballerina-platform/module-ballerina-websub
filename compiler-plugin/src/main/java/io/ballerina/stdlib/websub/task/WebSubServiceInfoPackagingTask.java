/*
 * Copyright (c) 2021, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ballerina.stdlib.websub.task;

import io.ballerina.projects.JBallerinaBackend;
import io.ballerina.projects.JarLibrary;
import io.ballerina.projects.JarResolver;
import io.ballerina.projects.JvmTarget;
import io.ballerina.projects.Package;
import io.ballerina.projects.plugins.CompilerLifecycleEventContext;
import io.ballerina.projects.plugins.CompilerLifecycleTask;
import io.ballerina.stdlib.websub.WebSubDiagnosticCodes;
import io.ballerina.stdlib.websub.task.service.path.ResourcePackagingService;
import io.ballerina.stdlib.websub.task.service.path.ServicePathContext;
import io.ballerina.stdlib.websub.task.service.path.ServicePathGeneratorException;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.stdlib.websub.task.service.path.ServicePathContextHandler.getContextHandler;

/**
 * {@code WebSubServiceInfoPackagingTask} handles post compile tasks related to unique service path generation.
 */
public class WebSubServiceInfoPackagingTask implements CompilerLifecycleTask<CompilerLifecycleEventContext> {
    private final ResourcePackagingService packagingService;

    public WebSubServiceInfoPackagingTask() {
        this.packagingService = new ResourcePackagingService();
    }

    @Override
    public void perform(CompilerLifecycleEventContext context) {
        boolean erroneousCompilation = context.compilation().diagnosticResult()
                .diagnostics().stream()
                .anyMatch(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()));
        // if the compilation already contains any error, do not proceed
        if (erroneousCompilation) {
            return;
        }

        Package currentPackage = context.currentPackage();
        Optional<ServicePathContext> servicePathContextOpt = getContextHandler()
                .retrieveContext(currentPackage.packageId(), currentPackage.project().sourceRoot());
        // if the shared service-path generation context not found, do not proceed
        if (servicePathContextOpt.isEmpty()) {
            return;
        }

        Optional<Path> executablePath = context.getGeneratedArtifactPath();
        executablePath.ifPresent(exec -> {
            ServicePathContext servicePathContext = servicePathContextOpt.get();
            updateResources(servicePathContext, exec, context);
        });
    }

    private void updateResources(ServicePathContext context, Path executablePath,
                                 CompilerLifecycleEventContext compilationContext) {
        Path executableJarAbsPath = executablePath.toAbsolutePath();
        // get the path for `target/bin`
        Path targetBinPath = executableJarAbsPath.getParent();
        if (null != targetBinPath && Files.exists(targetBinPath)) {
            // if generated service-path information is empty, do not proceed
            if (context.getServicePathDetails().isEmpty()) {
                return;
            }

            String executableJarFileName = executableJarAbsPath.toFile().getName();
            try {
                // update the executable jar
                this.packagingService.updateJarFile(targetBinPath, executableJarFileName, context);

                // update the thin-jar | this is required for dockerized applications
                Optional<JarLibrary> thinJarOpt = getThinJar(compilationContext, executablePath);
                if (thinJarOpt.isPresent()) {
                    JarLibrary thinJar = thinJarOpt.get();
                    Path thinJarLocation = thinJar.path().toAbsolutePath();
                    Path parenDirectory = thinJarLocation.getParent();
                    String thinJarName = thinJarLocation.toFile().getName();
                    this.packagingService.updateJarFile(parenDirectory, thinJarName, context);
                }
            } catch (IOException | ServicePathGeneratorException e) {
                WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_201;
                DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                        errorCode.getCode(), errorCode.getDescription(), errorCode.getSeverity());
                Diagnostic diagnostic = DiagnosticFactory.createDiagnostic(
                        diagnosticInfo, new NullLocation(), e.getMessage());
                compilationContext.reportDiagnostic(diagnostic);
            }
        }
    }

    private Optional<JarLibrary> getThinJar(CompilerLifecycleEventContext compilationContext, Path executablePath) {
        JBallerinaBackend jBallerinaBackend = JBallerinaBackend
                .from(compilationContext.compilation(), JvmTarget.JAVA_11);
        JarResolver jarResolver = jBallerinaBackend.jarResolver();

        Package currentPackage = compilationContext.currentPackage();
        String thinJarName = "$anon".equals(currentPackage.packageOrg().value())
                ? executablePath.getFileName().toString()
                : currentPackage.packageOrg().value() + "-" + currentPackage.packageName().value() +
                "-" + currentPackage.packageVersion().value() + ".jar";

        return jarResolver
                .getJarFilePathsRequiredForExecution().stream()
                .filter(jarLibrary -> isCorrectThinJar(jarLibrary, thinJarName))
                .findFirst();
    }

    private boolean isCorrectThinJar(JarLibrary jarLibrary, String expectedThinJar) {
        Path filePath = jarLibrary.path();
        if (Objects.nonNull(filePath)) {
            Path fileName = filePath.getFileName();
            if (Objects.nonNull(fileName)) {
                return fileName.toString().endsWith(expectedThinJar);
            }
        }
        return false;
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

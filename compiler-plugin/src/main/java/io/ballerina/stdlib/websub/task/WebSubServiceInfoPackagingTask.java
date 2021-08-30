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

import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.plugins.CompilerLifecycleEventContext;
import io.ballerina.projects.plugins.CompilerLifecycleTask;
import io.ballerina.stdlib.websub.Constants;
import io.ballerina.stdlib.websub.task.service.path.ResourcePackagingService;
import io.ballerina.stdlib.websub.task.service.path.ServicePathGenerationException;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * {@code WebSubServiceInfoPackagingTask} handles post compile tasks related to unique service path generation.
 */
public class WebSubServiceInfoPackagingTask implements CompilerLifecycleTask<CompilerLifecycleEventContext> {
    private static final PrintStream ERR = System.err;

    private final ResourcePackagingService packagingService;

    public WebSubServiceInfoPackagingTask() {
        this.packagingService = new ResourcePackagingService();
    }

    @Override
    public void perform(CompilerLifecycleEventContext context) {
        ProjectKind projectType = context.currentPackage().project().kind();
        Optional<Path> executablePath = context.getGeneratedArtifactPath();
        executablePath.ifPresent(exec ->
                updateResources(exec, ProjectKind.SINGLE_FILE_PROJECT.equals(projectType))
        );
    }

    private void updateResources(Path executablePath, boolean isSingleFile) {
        Path executableJarAbsPath = executablePath.toAbsolutePath();
        // get the path for `target/bin`
        Path targetBinPath = executableJarAbsPath.getParent();
        if (null != targetBinPath && Files.exists(targetBinPath)) {
            String executableJarFileName = executableJarAbsPath.toFile().getName();
            try {
                this.packagingService.updateExecutableJar(targetBinPath, executableJarFileName);
            } catch (IOException | ServicePathGenerationException e) {
                ERR.println("error [service-path-generation]: " + e.getLocalizedMessage());
            }

            // clean up created intermediate resources if current project is a single-ballerina-file project
            if (isSingleFile) {
                execCleanup(targetBinPath);
            }
        }
    }

    private void execCleanup(Path targetPath) {
        Path resourcesDirectory = targetPath.resolve(Constants.RESOURCES_DIR_NAME);
        try {
            if (Files.exists(resourcesDirectory)) {
                Files.delete(resourcesDirectory);
            }
        } catch (IOException e) {
            ERR.println("error [service-path-generation]: resource cleanup failed for single-file ballerina project"
                    + e.getLocalizedMessage());
        }
    }
}

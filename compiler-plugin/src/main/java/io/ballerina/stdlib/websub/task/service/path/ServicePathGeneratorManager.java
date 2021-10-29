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

package io.ballerina.stdlib.websub.task.service.path;

import io.ballerina.projects.Package;
import io.ballerina.projects.PackageId;
import io.ballerina.projects.Project;

import java.util.List;

/**
 * {@code ServicePathGeneratorManager} manages unique service-path generation for subscriber-services
 * depending on whether the current project is a ballerina-project or a single ballerina file.
 */
public final class ServicePathGeneratorManager {
    private final List<ServicePathGenerator> servicePathGenerators;

    public ServicePathGeneratorManager() {
        this.servicePathGenerators = List.of(
                new SingleFileServicePathGenerator(), new BalProjectServicePathGenerator());
    }

    public void generate(Package currentPackage, int serviceId) throws ServicePathGeneratorException {
        Project currentProject = currentPackage.project();
        for (ServicePathGenerator servicePathGenerator: servicePathGenerators) {
            if (servicePathGenerator.isSupported(currentProject.kind())) {
                PackageId packageId = currentPackage.packageId();
                servicePathGenerator.generate(packageId, currentProject.sourceRoot(), serviceId);
                return;
            }
        }

        // throw an error if we could not find a valid service-path generator
        // this has to be done because without service-path subscriber-service could not start
        throw new ServicePathGeneratorException("Valid service path generator not found");
    }
}

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

package io.ballerina.stdlib.websub.task.service.path;

import io.ballerina.projects.PackageId;
import io.ballerina.projects.ProjectKind;

import java.nio.file.Path;

/**
 * {@code ServicePathGenerator} generates unique service path for subscriber service and saves into a resource file.
 */
public interface ServicePathGenerator {
    /**
     * Generates the service-path for the provided `websub:SubscriberService`.
     *
     * @param packageId - PackageId of the current ballerina project
     * @param currentProjectRoot - Project source root path
     * @param serviceId - ServiceId of the curreng `websub:SubscriberService`
     * @throws ServicePathGeneratorException - If there is any runtime exception while generating the service-path
     */
    void generate(PackageId packageId, Path currentProjectRoot, int serviceId) throws ServicePathGeneratorException;

    /**
     * Checks whether current {@code ServicePathGenerator} instance supports the provided ballerina project-type.
     *
     * @param projectType - Ballerina project-type of the current project
     * @return - {@code true} if current {@code ServicePathGenerator} is compatible with the provided project-type
     *           or else {@code false}
     */
    boolean isSupported(ProjectKind projectType);
}

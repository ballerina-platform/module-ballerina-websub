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

import java.nio.file.Path;
import java.util.Random;

import static io.ballerina.stdlib.websub.task.service.path.ServicePathContextHandler.getContextHandler;

/**
 * {@code AbstractServicePathGenerator} contains the basic utilities required for unique service-path generation.
 */
public abstract class AbstractServicePathGenerator implements ServicePathGenerator {
    @Override
    public void generate(PackageId packageId, Path currentProjectRoot, int serviceId)
        throws ServicePathGeneratorException {
        try {
            String generatedServicePath = generateRandomAlphaNumericString(10);
            ServicePathContext.ServicePathInformation servicePathDetails = new ServicePathContext
                    .ServicePathInformation(serviceId, generatedServicePath);
            getContextHandler().updateServicePathContext(packageId, currentProjectRoot, servicePathDetails);
        } catch (Exception ex) {
            // throw an error if there is any error in service-info generation
            String errorMsg = String.format("service path generation failed due to %s", ex.getLocalizedMessage());
            throw new ServicePathGeneratorException(errorMsg, ex);
        }
    }

    /**
     * Generates a random alphanumeric string with the provided length.
     *
     * @param stringLength length of the generated string
     * @return random alphanumeric string
     */
    private String generateRandomAlphaNumericString(int stringLength) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();
        return random.ints(leftLimit, rightLimit + 1)
                // character literals from 48 - 57 are numbers | 65 - 90 are capital letters |
                // 97 - 122 are simple letters
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(stringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}

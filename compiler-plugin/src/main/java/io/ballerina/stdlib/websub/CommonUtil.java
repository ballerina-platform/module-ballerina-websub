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

import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;

import java.util.Optional;

import static io.ballerina.stdlib.websub.task.AnalyserUtils.getQualifiedType;

/**
 * {@code CommonUtil} contains common utility functions related to compiler-plugin.
 */
public final class CommonUtil {
    private CommonUtil() {}

    public static Optional<AnnotationSymbol> extractSubscriberServiceConfig(ServiceDeclarationSymbol service) {
        return service.annotations()
                .stream()
                .filter(annotationSymbol -> {
                    String moduleName = annotationSymbol.getModule()
                            .flatMap(ModuleSymbol::getName)
                            .orElse("");
                    String type = annotationSymbol.getName().orElse("");
                    String annotationName = getQualifiedType(type, moduleName);
                    return annotationName.equals(Constants.SERVICE_ANNOTATION_NAME);
                }).findFirst();
    }
}

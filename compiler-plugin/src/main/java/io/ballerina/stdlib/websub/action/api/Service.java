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

package io.ballerina.stdlib.websub.action.api;

import io.ballerina.stdlib.websub.Constants;

import java.util.List;

/**
 * {@code Service} code snippet for ballerina `websub:SubscriberService`.
 */
public class Service implements CodeSnippet {
    private final List<Annotation> annotations;
    private final List<Function> functions;

    public Service(List<Annotation> annotations, List<Function> functions) {
        this.annotations = annotations;
        this.functions = functions;
    }

    public String getAnnotationSnippet() {
        return annotations.stream()
                .map(Annotation::snippetAsAString)
                .reduce("", (a, b) -> String.format("%s%s%s", a, Constants.LS, b));
    }

    public String getFunctionSnippet() {
        return functions.stream()
                .map(Function::snippetAsAString)
                .reduce("", (a, b) -> String.format("%s%s%s", a, Constants.LS, b));
    }

    @Override
    // todo: implement this method properly
    public String snippetAsAString() {
        return null;
    }
}

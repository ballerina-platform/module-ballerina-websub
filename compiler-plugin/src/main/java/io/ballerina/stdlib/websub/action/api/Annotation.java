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

/**
 * {@code Annotation} code snippet for ballerina annotation.
 */
public class Annotation implements CodeSnippet {
    private final Type type;

    private Annotation(Type type) {
        this.type = type;
    }

    public static Annotation getEmptyAnnotation(String module, String name) {
        Type type = Type.from(module, name);
        return new Annotation(type);
    }

    // Generated annotation snippet template does look like following:
    //      "@websub:SubscriberServiceConfig { }"
    @Override
    public String snippetAsAString() {
        String annotationType = String.format("@%s", this.type.snippetAsAString());
        return String.format("%s { }", annotationType);
    }
}

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

import java.util.Collections;
import java.util.List;

/**
 * {@code Annotation} code snippet for ballerina annotation.
 */
public class Annotation implements CodeSnippet {
    private final Type type;
    private final List<AnnotationField> fields;

    private Annotation(Type type, List<AnnotationField> fields) {
        this.type = type;
        this.fields = fields;
    }

    public static Annotation getEmptyAnnotation(String module, String name) {
        Type type = Type.from(module, name);
        return new Annotation(type, Collections.emptyList());
    }

    // Generated function snippet template does look like following:
    //      "@websub:SubscriberServiceConfig {LS + LS}" +
    @Override
    public String snippetAsAString() {
        String fields = this.fields.stream()
                .map(field -> String.format("\t%s: %s", field.name, field.value))
                .reduce("", (a, b) -> String.format("%s%s%s", a, Constants.LS, b));
        String annotationType = String.format("@%s", this.type.snippetAsAString());
        return String.format("%s {%s%s%s}", annotationType, Constants.LS, fields, Constants.LS);
    }

    /**
     * {@code AnnotationField} which represents a field of a ballerina annotation.
     */
    public static class AnnotationField {
        private final String name;
        private final String value;

        public AnnotationField(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
}

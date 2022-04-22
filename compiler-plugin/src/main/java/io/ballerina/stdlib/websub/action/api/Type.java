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

/**
 * {@code Type} code snippet for ballerina type.
 */
public class Type implements CodeSnippet {
    private final String module;
    private final String name;

    private Type(String module, String name) {
        this.module = module;
        this.name = name;
    }

    public static Type from(String module, String name) {
        return new Type(module, name);
    }

    @Override
    public String snippetAsAString() {
        if (Constants.BALLERINA_ERROR_PACKAGE.equals(this.module)) {
            return this.name;
        }
        return String.format("%s:%s", module, name);
    }
}

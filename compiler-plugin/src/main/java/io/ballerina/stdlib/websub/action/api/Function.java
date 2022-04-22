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
 * {@code Function} code snippet for ballerina function.
 */
public class Function implements CodeSnippet {
    private String type = "remote";
    private final String  name;
    private final List<FunctionArg> args;
    private final List<Type> returnTypes;
    private boolean optionalReturnTypes = false;

    private Function(String name, List<FunctionArg> args, List<Type> returnTypes) {
        this.name = name;
        this.args = args;
        this.returnTypes = returnTypes;
    }

    public static Function remoteFunctionWithOptionalReturnTypes(String name, List<FunctionArg> args,
                                                                 List<Type> returnTypes) {
        Function function = new Function(name, args, returnTypes);
        function.optionalReturnTypes = true;
        return function;
    }

    // Generated function snippet template does look like following:
    //      "\tremote function onEventNotification(websub:ContentDistributionMessage message) returns " +
    //      "websub:Acknowledgement|websub:SubscriptionDeletedError|error? " +
    //      "{" + LS + LS + "\t}"
    @Override
    public String snippetAsAString() {
        String functionArgs = this.args.stream()
                .map(arg -> String.format("%s %s", arg.type.snippetAsAString(), arg.name))
                .reduce("", (a, b) -> String.format("%s ,%s", a, b));
        String returnTypes = constructReturnTypes();
        return String.format("\t%s %s %s(%s) returns %s {%s%s\t}",
                this.type, "function", this.name, functionArgs, returnTypes, Constants.LS, Constants.LS);
    }

    private String constructReturnTypes() {
        String providedReturnTypes = this.returnTypes.stream()
                .map(Type::snippetAsAString)
                .reduce("", (a, b) -> String.format("%s|%s", a, b));
        if (optionalReturnTypes) {
            return String.format("%s?", providedReturnTypes);
        }
        return providedReturnTypes;
    }

    /**
     * {@code FunctionArg} which represents an argument to a ballerina function.
     */
    public static class FunctionArg {
        private final Type type;
        private final String name;

        public FunctionArg(Type type, String name) {
            this.type = type;
            this.name = name;
        }
    }
}

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

package io.ballerina.stdlib.websub.action;

import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.stdlib.websub.Constants;
import io.ballerina.stdlib.websub.action.api.Function;
import io.ballerina.stdlib.websub.action.api.Type;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;

import java.util.List;

/**
 * {@code CodeActionUtil} contains utility functions related to code-actions.
 */
public final class CodeActionUtil {
    /**
     * Finds a node in syntax-tree by line-range.
     *
     * @param syntaxTree Syntax tree
     * @param lineRange Line range
     * @return node
     */
    public static NonTerminalNode findNode(SyntaxTree syntaxTree, LineRange lineRange) {
        TextDocument textDocument = syntaxTree.textDocument();
        int start = textDocument.textPositionFrom(lineRange.startLine());
        int end = textDocument.textPositionFrom(lineRange.endLine());
        return ((ModulePartNode) syntaxTree.rootNode())
                .findNode(TextRange.from(start, end - start), true);
    }

    public static List<Function> constructMandatoryFunctions() {
        // only mandatory function is `onEventNotification`
        List<Function.FunctionArg> functionArgs = constructFunctionArgs();
        List<Type> returnTypes = constructReturnTypes();
        Function onEventNotificationFunction = Function.remoteFunctionWithOptionalReturnTypes(
                Constants.ON_EVENT_NOTIFICATION, functionArgs, returnTypes);
        return List.of(onEventNotificationFunction);
    }

    private static List<Function.FunctionArg> constructFunctionArgs() {
        Type contentDistributionMessage = Type.from(
                Constants.PACKAGE_NAME, Constants.CONTENT_DISTRIBUTION_MESSAGE_TYPE);
        Function.FunctionArg message = new Function.FunctionArg(
                contentDistributionMessage, Constants.CONTENT_DISTRIBUTION_MESSAGE_PARAM_NAME);
        return List.of(message);
    }

    private static List<Type> constructReturnTypes() {
        Type acknowledgement = Type.from(Constants.PACKAGE_NAME, Constants.ACKNOWLEDGEMENT_TYPE);
        Type subscriptionDeletedError = Type.from(Constants.PACKAGE_NAME, Constants.SUBSCRIPTION_DELETED_ERROR_TYPE);
        Type error = Type.from(Constants.BALLERINA_ERROR_PACKAGE, Constants.BALLERINA_ERROR_TYPE);
        return List.of(acknowledgement, subscriptionDeletedError, error);
    }
}

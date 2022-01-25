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

package io.ballerina.stdlib.websub;

import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.plugins.codeaction.*;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Code action to generate `websub:SubscriberService` code snippet.
 */
public class WebSubTemplateGenerations implements CodeAction {
    private static final String NODE_LOCATION = "node.location";

    @Override
    public List<String> supportedDiagnosticCodes() {
        return List.of(WebSubDiagnosticCodes.WEBSUB_202.getCode());
    }

    @Override
    public Optional<CodeActionInfo> codeActionInfo(CodeActionContext context) {
        Diagnostic diagnostic = context.diagnostic();
        if (Objects.isNull(diagnostic.location())) {
            return Optional.empty();
        }
        CodeActionArgument locationArg = CodeActionArgument.from(NODE_LOCATION, diagnostic.location().lineRange());
        return Optional.of(CodeActionInfo.from("SUBSCRIBER_TEMPLATE", List.of(locationArg)));
    }

    @Override
    public List<DocumentEdit> execute(CodeActionExecutionContext executionContext) {
        Optional<LineRange> lineRangeOpt = executionContext.arguments().stream()
                .filter(arg -> NODE_LOCATION.endsWith(arg.key()))
                .map(arg -> arg.valueAs(LineRange.class)).findAny();
        if (lineRangeOpt.isEmpty()) {
            return Collections.emptyList();
        }

        LineRange lineRange = lineRangeOpt.get();
        SyntaxTree syntaxTree = executionContext.currentDocument().syntaxTree();

        NonTerminalNode node = findNode(syntaxTree, lineRange);
        if (!(node instanceof ServiceDeclarationNode)) {
            return Collections.emptyList();
        }

        return null;
    }

    public static NonTerminalNode findNode(SyntaxTree syntaxTree, LineRange lineRange) {
        TextDocument textDocument = syntaxTree.textDocument();
        int start = textDocument.textPositionFrom(lineRange.startLine());
        int end = textDocument.textPositionFrom(lineRange.endLine());
        return ((ModulePartNode) syntaxTree.rootNode())
                .findNode(TextRange.from(start, end - start), true);
    }

    @Override
    public String name() {
        return "ADD_SUBSCRIBER_CODE_SNIPPET";
    }
}

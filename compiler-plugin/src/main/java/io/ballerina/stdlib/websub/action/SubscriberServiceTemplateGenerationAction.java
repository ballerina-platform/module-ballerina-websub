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

import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.plugins.codeaction.CodeAction;
import io.ballerina.projects.plugins.codeaction.CodeActionArgument;
import io.ballerina.projects.plugins.codeaction.CodeActionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionExecutionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionInfo;
import io.ballerina.projects.plugins.codeaction.DocumentEdit;
import io.ballerina.stdlib.websub.Constants;
import io.ballerina.stdlib.websub.WebSubDiagnosticCodes;
import io.ballerina.stdlib.websub.action.api.Service;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextEdit;
import io.ballerina.tools.text.TextRange;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.stdlib.websub.action.CodeActionUtil.findNode;

/**
 * {@code EmptySubscriberServiceTemplateGenerateAction} generates empty `websub:SubscriberService` code-snippet.
 */
public class SubscriberServiceTemplateGenerationAction implements CodeAction {
    private static final String NODE_LOCATION = "node.location";
    private static final String CODE_ACTION_TITLE = "Add subscriber service";

    private final Service websubServiceSnippet;

    public SubscriberServiceTemplateGenerationAction() {
        this.websubServiceSnippet = CodeActionUtil.constructSubscriberService();
    }

    @Override
    public List<String> supportedDiagnosticCodes() {
        return List.of(WebSubDiagnosticCodes.WEBSUB_202.getCode());
    }

    @Override
    public Optional<CodeActionInfo> codeActionInfo(CodeActionContext context) {
        return Optional.ofNullable(context.diagnostic())
                .filter(d -> Objects.nonNull(d.location()))
                .map(this::constructCodeActionInfo);
    }

    private CodeActionInfo constructCodeActionInfo(Diagnostic diagnostic) {
        CodeActionArgument locationArg = CodeActionArgument.from(NODE_LOCATION, diagnostic.location().lineRange());
        return CodeActionInfo.from(CODE_ACTION_TITLE, List.of(locationArg));
    }

    @Override
    public List<DocumentEdit> execute(CodeActionExecutionContext executionContext) {
        Optional<LineRange> lineRangeOpt = executionContext.arguments().stream()
                .filter(arg -> NODE_LOCATION.equals(arg.key()))
                .map(arg -> arg.valueAs(LineRange.class))
                .findFirst();
        if (lineRangeOpt.isEmpty()) {
            return Collections.emptyList();
        }
        LineRange lineRange = lineRangeOpt.get();
        SyntaxTree syntaxTree = executionContext.currentDocument().syntaxTree();
        NonTerminalNode node = findNode(syntaxTree, lineRange);
        if (!(node instanceof ServiceDeclarationNode)) {
            return Collections.emptyList();
        }
        List<TextEdit> textEdits = retrieveRequiredTextEdits((ServiceDeclarationNode) node);
        TextDocumentChange change = TextDocumentChange.from(textEdits.toArray(new TextEdit[0]));
        return Collections.singletonList(
                new DocumentEdit(executionContext.fileUri(), SyntaxTree.from(syntaxTree, change)));
    }

    private List<TextEdit> retrieveRequiredTextEdits(ServiceDeclarationNode serviceDeclarationNode) {
        TextRange annotationTextRange = TextRange.from(
                serviceDeclarationNode.serviceKeyword().textRange().startOffset(), 0);
        TextEdit annotationsEdit = TextEdit.from(
                annotationTextRange, String.format("%s%s", websubServiceSnippet.getAnnotationSnippet(), Constants.LS));
        TextRange functionTextRange = TextRange.from(
                serviceDeclarationNode.openBraceToken().textRange().endOffset(), 0);
        TextEdit functionsEdit = TextEdit.from(
                functionTextRange, String.format("%s%s", websubServiceSnippet.getFunctionSnippet(), Constants.LS));
        return Arrays.asList(annotationsEdit, functionsEdit);
    }

    @Override
    public String name() {
        return "ADD_SUBSCRIBER_SERVICE_CODE_SNIPPET";
    }
}

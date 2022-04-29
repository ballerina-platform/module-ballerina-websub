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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
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
import io.ballerina.stdlib.websub.action.api.Annotation;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextEdit;
import io.ballerina.tools.text.TextRange;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.ballerina.stdlib.websub.CommonUtil.extractSubscriberServiceConfig;
import static io.ballerina.stdlib.websub.action.CodeActionUtil.findNode;

/**
 * {@code AnnotationConfigsGenerationAction} generates code-snippets related to annotation config on
 * `websub:SubscriberService`.
 */
public class AnnotationConfigsGenerationAction implements CodeAction {
    private final List<Annotation> serviceAnnotations;

    public AnnotationConfigsGenerationAction() {
        this.serviceAnnotations = List.of(
                Annotation.getEmptyAnnotation(Constants.PACKAGE_NAME, Constants.SERVICE_CONFIG_ANNOTATTION_TYPE));
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
        CodeActionArgument locationArg = CodeActionArgument.from(Constants.NODE_LOCATION,
                diagnostic.location().lineRange());
        return CodeActionInfo.from(Constants.ADD_SERVICE_ANNOTATION_CONFIGS_ACTION, List.of(locationArg));
    }

    @Override
    public List<DocumentEdit> execute(CodeActionExecutionContext executionContext) {
        Optional<LineRange> lineRangeOpt = executionContext.arguments().stream()
                .filter(arg -> Constants.NODE_LOCATION.equals(arg.key()))
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
        ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) node;
        Optional<AnnotationSymbol> serviceAnnotation = executionContext.currentSemanticModel().symbol(serviceNode)
                .flatMap(service -> extractSubscriberServiceConfig((ServiceDeclarationSymbol) service));
        if (serviceAnnotation.isPresent()) {
            return Collections.emptyList();
        }
        List<TextEdit> textEdits = retrieveRequiredTextEdits(
                serviceNode, executionContext.currentSemanticModel());
        if (textEdits.isEmpty()) {
            return Collections.emptyList();
        }
        TextDocumentChange change = TextDocumentChange.from(textEdits.toArray(new TextEdit[0]));
        return Collections.singletonList(
                new DocumentEdit(executionContext.fileUri(), SyntaxTree.from(syntaxTree, change)));
    }

    private List<TextEdit> retrieveRequiredTextEdits(ServiceDeclarationNode serviceNode, SemanticModel semanticModel) {
        TextRange annotationTextRange = TextRange.from(
                serviceNode.serviceKeyword().textRange().startOffset(), 0);
        String annotationCodeSnippet = serviceAnnotations.stream()
                .map(Annotation::snippetAsAString)
                .collect(Collectors.joining(Constants.LS));
        TextEdit annotationsEdit = TextEdit.from(
                annotationTextRange, String.format("%s%s", annotationCodeSnippet, Constants.LS));
        return List.of(annotationsEdit);
    }

    @Override
    public String name() {
        return "ADD_SERVICE_ANNOTATION_CODE_SNIPPET";
    }
}

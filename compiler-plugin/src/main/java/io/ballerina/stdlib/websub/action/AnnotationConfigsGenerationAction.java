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
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.NodeList;
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

import static io.ballerina.stdlib.websub.action.CodeActionUtil.findNode;
import static io.ballerina.stdlib.websub.task.AnalyserUtils.getQualifiedType;

/**
 * {@code AnnotationConfigsGenerationAction} generates code-snippets related to annotation config on
 * `websub:SubscriberService`.
 */
public class AnnotationConfigsGenerationAction implements CodeAction {
    private final List<Annotation> serviceAnnotations;

    public AnnotationConfigsGenerationAction() {
        this.serviceAnnotations = List.of(
                Annotation.getEmptyAnnotation(Constants.PACKAGE_NAME, Constants.SERVICE_CONFIG_ANNOTATTION));
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
        List<TextEdit> textEdits = retrieveRequiredTextEdits(
                (ServiceDeclarationNode) node, executionContext.currentSemanticModel());
        if (textEdits.isEmpty()) {
            return Collections.emptyList();
        }
        TextDocumentChange change = TextDocumentChange.from(textEdits.toArray(new TextEdit[0]));
        return Collections.singletonList(
                new DocumentEdit(executionContext.fileUri(), SyntaxTree.from(syntaxTree, change)));
    }

    private List<TextEdit> retrieveRequiredTextEdits(ServiceDeclarationNode serviceNode, SemanticModel semanticModel) {
        boolean serviceAnnotationExists = serviceAnnotationExists(serviceNode, semanticModel);
        if (serviceAnnotationExists) {
            return Collections.emptyList();
        }
        TextRange annotationTextRange = TextRange.from(
                serviceNode.serviceKeyword().textRange().startOffset(), 0);
        String annotationCodeSnippet = serviceAnnotations.stream()
                .map(Annotation::snippetAsAString)
                .collect(Collectors.joining(Constants.LS));
        TextEdit annotationsEdit = TextEdit.from(
                annotationTextRange, String.format("%s%s", annotationCodeSnippet, Constants.LS));
        return List.of(annotationsEdit);
    }

    private boolean serviceAnnotationExists(ServiceDeclarationNode serviceNode, SemanticModel semanticModel) {
        Optional<NodeList<AnnotationNode>> annotationsOpt = serviceNode.metadata().map(MetadataNode::annotations);
        if (annotationsOpt.isEmpty()) {
            return false;
        }
        for (AnnotationNode annotation: annotationsOpt.get()) {
            Optional<Symbol> symbolOpt = semanticModel.symbol(annotation);
            if (symbolOpt.isEmpty()) {
                continue;
            }
            Symbol symbol = symbolOpt.get();
            if (!(symbol instanceof AnnotationSymbol)) {
                continue;
            }
            AnnotationSymbol annotationSymbol = (AnnotationSymbol) symbol;
            String moduleName = annotationSymbol.getModule().flatMap(ModuleSymbol::getName).orElse("");
            String type = annotationSymbol.getName().orElse("");
            String annotationName = getQualifiedType(type, moduleName);
            return Constants.SERVICE_ANNOTATION_NAME.equals(annotationName);
        }
        return false;
    }

    @Override
    public String name() {
        return "ADD_SERVICE_ANNOTATION_CODE_SNIPPET";
    }
}

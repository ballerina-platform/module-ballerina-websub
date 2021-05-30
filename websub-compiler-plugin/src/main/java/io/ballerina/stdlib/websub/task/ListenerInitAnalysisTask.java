/*
 * Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.websub.task;

import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeLocation;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.RestArgumentNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.websub.Constants;
import io.ballerina.stdlib.websub.WebSubDiagnosticCodes;

import java.util.Optional;

import static io.ballerina.stdlib.websub.task.AnalyserUtils.isWebSubListener;
import static io.ballerina.stdlib.websub.task.AnalyserUtils.updateContext;

/**
 * {@code ListenerInitAnalysisTask} validates websub listener init declaration.
 */
public class ListenerInitAnalysisTask implements AnalysisTask<SyntaxNodeAnalysisContext> {
    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        Node node = context.node();
        SyntaxKind nodeSyntaxKind = node.kind();
        if (nodeSyntaxKind == SyntaxKind.EXPLICIT_NEW_EXPRESSION) {
            ExplicitNewExpressionNode expressionNode = (ExplicitNewExpressionNode) node;
            Optional<Symbol> symbolOpt = context.semanticModel().symbol(expressionNode.typeDescriptor());
            if (symbolOpt.isPresent() && symbolOpt.get() instanceof TypeReferenceTypeSymbol) {
                TypeSymbol typeDescriptor = ((TypeReferenceTypeSymbol) symbolOpt.get()).typeDescriptor();
                String identifier = typeDescriptor.getName().orElse("");
                if (Constants.LISTENER_IDENTIFIER.equals(identifier) && isWebSubListener(typeDescriptor)) {
                    SeparatedNodeList<FunctionArgumentNode> functionArgs = expressionNode
                            .parenthesizedArgList().arguments();
                    verifyListenerArgType(context, expressionNode.location(), functionArgs);
                }
            }
        } else if (nodeSyntaxKind == SyntaxKind.IMPLICIT_NEW_EXPRESSION) {
            ImplicitNewExpressionNode expressionNode = (ImplicitNewExpressionNode) node;
            if (expressionNode.parent() instanceof ListenerDeclarationNode) {
                ListenerDeclarationNode parentNode = (ListenerDeclarationNode) expressionNode.parent();
                Optional<TypeDescriptorNode> parentTypeOpt = parentNode.typeDescriptor();
                if (parentTypeOpt.isPresent()) {
                    Optional<Symbol> parentSymbolOpt = context.semanticModel().symbol(parentTypeOpt.get());
                    if (parentSymbolOpt.isPresent() && parentSymbolOpt.get() instanceof TypeReferenceTypeSymbol) {
                        TypeSymbol typeSymbol = ((TypeReferenceTypeSymbol) parentSymbolOpt.get()).typeDescriptor();
                        if (isWebSubListener(typeSymbol)) {
                            Optional<ParenthesizedArgList> argListOpt = expressionNode.parenthesizedArgList();
                            if (argListOpt.isPresent()) {
                                SeparatedNodeList<FunctionArgumentNode> functionArgs = argListOpt.get().arguments();
                                verifyListenerArgType(context, expressionNode.location(), functionArgs);
                            }
                        }
                    }
                }
            }
        }
    }

    private void verifyListenerArgType(SyntaxNodeAnalysisContext context, NodeLocation location,
                                       SeparatedNodeList<FunctionArgumentNode> functionArgs) {
        // two args are valid only if the first arg is numeric (i.e, port and config)
        if (functionArgs.size() == 2) {
            Optional<SyntaxKind> firstArgSyntaxKindOpt = getArgSyntaxKind(functionArgs.get(0));
            if (firstArgSyntaxKindOpt.isPresent() && firstArgSyntaxKindOpt.get() != SyntaxKind.NUMERIC_LITERAL) {
                WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_109;
                updateContext(context, errorCode, location);
            }
        }
    }

    private Optional<SyntaxKind> getArgSyntaxKind(FunctionArgumentNode argument) {
        SyntaxKind syntaxKind = null;
        if (argument instanceof PositionalArgumentNode) {
            syntaxKind = ((PositionalArgumentNode) argument).expression().kind();
        } else if (argument instanceof NamedArgumentNode) {
            syntaxKind = ((NamedArgumentNode) argument).expression().kind();
        } else if (argument instanceof RestArgumentNode) {
            syntaxKind = ((RestArgumentNode) argument).expression().kind();
        }
        return Optional.ofNullable(syntaxKind);
    }
}

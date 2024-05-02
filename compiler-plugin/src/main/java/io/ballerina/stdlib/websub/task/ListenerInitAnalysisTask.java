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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.RestArgumentNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
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
        SyntaxKind nodeSyntaxKind = context.node().kind();
        if (nodeSyntaxKind == SyntaxKind.EXPLICIT_NEW_EXPRESSION) {
            ExplicitNewExpressionNode expressionNode = (ExplicitNewExpressionNode) context.node();
            validateExplicitNewListener(context, expressionNode);
        } else if (nodeSyntaxKind == SyntaxKind.IMPLICIT_NEW_EXPRESSION) {
            ImplicitNewExpressionNode expressionNode = (ImplicitNewExpressionNode) context.node();
            validateImplicitNewListener(context, expressionNode);
        }
    }

    private void validateExplicitNewListener(SyntaxNodeAnalysisContext context, ExplicitNewExpressionNode node) {
        TypeDescriptorNode typeDescriptor = node.typeDescriptor();
        if (context.semanticModel().symbol(typeDescriptor).isEmpty()) {
            return;
        }
        Symbol listenerSymbol = context.semanticModel().symbol(typeDescriptor).get();
        if (listenerSymbol.kind() != SymbolKind.TYPE) {
            return;
        }
        TypeSymbol listenerTypeSymbol = ((TypeReferenceTypeSymbol) listenerSymbol).typeDescriptor();
        if (!isWebSubListener(listenerTypeSymbol)) {
            return;
        }
        SeparatedNodeList<FunctionArgumentNode> functionArgs = node.parenthesizedArgList().arguments();
        verifyListenerArgType(context, functionArgs);
    }

    private void validateImplicitNewListener(SyntaxNodeAnalysisContext context, ImplicitNewExpressionNode node) {
        Optional<ListenerDeclarationNode> listenerNodeOpt = getImplicitListenerNode(node);
        if (listenerNodeOpt.flatMap(ListenerDeclarationNode::typeDescriptor).isEmpty()) {
            return;
        }
        TypeDescriptorNode typeDescriptor = listenerNodeOpt.flatMap(ListenerDeclarationNode::typeDescriptor).get();
        if (context.semanticModel().symbol(typeDescriptor).isEmpty()) {
            return;
        }
        Symbol listenerSymbol = context.semanticModel().symbol(typeDescriptor).get();
        if (listenerSymbol.kind() != SymbolKind.TYPE) {
            return;
        }
        TypeSymbol listenerTypeSymbol = ((TypeReferenceTypeSymbol) listenerSymbol).typeDescriptor();
        if (!isWebSubListener(listenerTypeSymbol)) {
            return;
        }
        if (node.parenthesizedArgList().isPresent()) {
            SeparatedNodeList<FunctionArgumentNode> functionArgs = node.parenthesizedArgList().get().arguments();
            verifyListenerArgType(context, functionArgs);
        }
    }

    private Optional<ListenerDeclarationNode> getImplicitListenerNode(ImplicitNewExpressionNode node) {
        if (node.parent().kind() == SyntaxKind.LISTENER_DECLARATION) {
            return Optional.of((ListenerDeclarationNode) node.parent());
        } else if (node.parent().kind() == SyntaxKind.CHECK_EXPRESSION) {
            if (node.parent().parent().kind() == SyntaxKind.LISTENER_DECLARATION) {
                return Optional.of((ListenerDeclarationNode) node.parent().parent());
            }
        }
        return Optional.empty();
    }

    private void verifyListenerArgType(SyntaxNodeAnalysisContext context,
                                       SeparatedNodeList<FunctionArgumentNode> functionArgs) {
        // two args are valid only if the first arg is numeric (i.e, port and config)
        if (functionArgs.size() > 1) {
            Optional<Symbol> firstArgSymbolOpt = getFirstListenerArg(context.semanticModel(), functionArgs.get(0));
            if (firstArgSymbolOpt.isEmpty()) {
                return;
            }
            Symbol firstArgSymbol = firstArgSymbolOpt.get();
            if (SymbolKind.VARIABLE.equals(firstArgSymbol.kind())) {
                VariableSymbol variable = (VariableSymbol) firstArgSymbol;
                if (TypeDescKind.INT.equals(variable.typeDescriptor().typeKind())) {
                    return;
                }
            }
            FunctionArgumentNode secondArg = functionArgs.get(1);
            updateContext(context, WebSubDiagnosticCodes.WEBSUB_109, secondArg.location());
        }
    }

    private Optional<Symbol> getFirstListenerArg(SemanticModel semanticModel, FunctionArgumentNode firstArg) {
        if (SyntaxKind.POSITIONAL_ARG.equals(firstArg.kind())) {
            return semanticModel.symbol(((PositionalArgumentNode) firstArg).expression());
        } else if (SyntaxKind.NAMED_ARG.equals(firstArg.kind())) {
            return semanticModel.symbol(((NamedArgumentNode) firstArg).expression());
        } else {
            return semanticModel.symbol(((RestArgumentNode) firstArg).expression());
        }
    }
}

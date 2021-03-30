package io.ballerina.stdlib.websub.validator;

import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.Document;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.websub.Constants;
import io.ballerina.tools.text.LinePosition;

import java.util.Optional;

/**
 * {@code ValidatorUtils} contains the utility methods required for validation tasks.
 */
public final class ValidatorUtils {
    public static boolean isWebSubService(SyntaxNodeAnalysisContext context) {
        ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) context.node();
        SeparatedNodeList<ExpressionNode> expressions = serviceNode.expressions();
        Optional<TypeSymbol> moduleTypeDescriptor = expressions.stream()
                .filter(e -> e.kind() == SyntaxKind.EXPLICIT_NEW_EXPRESSION ||
                        e.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE)
                .map(e -> {
                    if (e.kind() == SyntaxKind.EXPLICIT_NEW_EXPRESSION) {
                        Document currentDocument = context.currentPackage().getDefaultModule()
                                .document(context.documentId());
                        LinePosition lineStart = e.lineRange().startLine();
                        Symbol currentSymbol = context.semanticModel().symbol(currentDocument, lineStart).get();
                        return ((TypeReferenceTypeSymbol) currentSymbol).typeDescriptor();
                    } else {
                        Document currentDocument = context.currentPackage().getDefaultModule()
                                .document(context.documentId());
                        LinePosition lineStart = e.lineRange().startLine();
                        return ((VariableSymbol) context.semanticModel().symbol(currentDocument, lineStart).get())
                                .typeDescriptor();
                    }
                }).findFirst();
        if (moduleTypeDescriptor.isEmpty()) {
            return false;
        } else {
            TypeSymbol moduleType = moduleTypeDescriptor.get();
            String signature = moduleType.signature();
            return signature.contains(Constants.MODULE_NAME);
        }
    }

//    public static boolean isWebSubServiceV2(SyntaxNodeAnalysisContext ctx) {
//        ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) ctx.node();
//        Optional<Symbol> symbol = ctx.semanticModel().symbol(serviceNode);
//        if (symbol.isPresent()) {
//            ServiceDeclarationSymbol serviceDeclarationSymbol = (ServiceDeclarationSymbol) symbol.get();
//            List<TypeSymbol> typeSymbols = serviceDeclarationSymbol.listenerTypes();
//        } else {
//            return false;
//        }
//        return false;
//    }
}

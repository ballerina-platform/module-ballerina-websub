package io.ballerina.stdlib.websub.validator;

import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

/**
 * {@code ServiceRequiredMethodValidator} validates whether all the
 * required method defined in service implementation.
 */
public class ServiceRequiredMethodValidator
        implements AnalysisTask<SyntaxNodeAnalysisContext> {
    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        ServiceDeclarationNode serviceNode =
                (ServiceDeclarationNode) context.node();
        SeparatedNodeList<ExpressionNode> expressions =
                serviceNode.expressions();
        TypeSymbol moduleType = null;
        for (ExpressionNode expressionNode : expressions) {
            if (expressionNode.kind() == SyntaxKind.EXPLICIT_NEW_EXPRESSION) {
                moduleType = ((TypeReferenceTypeSymbol) (
                        context.semanticModel()
                                .symbol(
                                        context.currentPackage()
                                                .getDefaultModule()
                                                .document(
                                                        context.documentId()),
                                        expressionNode
                                                .lineRange().startLine()).get())
                ).typeDescriptor();

            } else if (expressionNode.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
                VariableSymbol symbol = (VariableSymbol) context.semanticModel().symbol
                        (context.currentPackage().getDefaultModule().
                                        document(context.documentId()),
                                expressionNode.lineRange().startLine()).get();
                moduleType = symbol.typeDescriptor();
            } else {
                moduleType = null;
                // todo
            }
        }

        String moduleNameSignature = moduleType == null ? "" : moduleType.signature();
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo("NATS_101",
                "service declaration in type: "
                + moduleNameSignature, DiagnosticSeverity.INFO);
        Diagnostic diagnostic = DiagnosticFactory
                .createDiagnostic(diagnosticInfo, serviceNode.location());
        context.reportDiagnostic(diagnostic);
    }
}

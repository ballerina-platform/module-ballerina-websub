package io.ballerina.stdlib.websub.validator;

import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

/**
 * {@code ServiceAnnotationValidator} validates whether required annotation config is available or not.
 */
public class ServiceAnnotationValidator implements AnalysisTask<SyntaxNodeAnalysisContext> {
    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) context.node();
        DiagnosticInfo info = new DiagnosticInfo(
                "WEBSUB_101", "Service declaration found", DiagnosticSeverity.WARNING);
        Diagnostic diagnostic = DiagnosticFactory.createDiagnostic(info, serviceDeclarationNode.location());
        context.reportDiagnostic(diagnostic);
//        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) context.node();
//        NonTerminalNode parentNode = serviceDeclarationNode.parent();
//        if (Objects.isNull(parentNode) || !(parentNode instanceof AnnotationNode)) {
//            DiagnosticInfo info = new DiagnosticInfo(
//                    "WEBSUB_101", "Could not find the service annotation configurations",
//                    DiagnosticSeverity.ERROR);
//            Diagnostic diagnostic = DiagnosticFactory.createDiagnostic(info, serviceDeclarationNode.location());
//            context.reportDiagnostic(diagnostic);
//        } else {
//            parentNode = (AnnotationNode) parentNode;
//            Token token = ((AnnotationNode) parentNode).atToken();
//            ((AnnotationNode) parentNode).annotReference()
//        }
    }
}

package io.ballerina.stdlib.websub.validator;

import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

/**
 * {@code CheckPanicValidator} performs validations related to usage of `checkpanic` expression.
 */
public class CheckPanicValidator implements AnalysisTask<SyntaxNodeAnalysisContext> {
    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        CheckExpressionNode checkExpressionNode = (CheckExpressionNode) context.node();
        Token token = checkExpressionNode.checkKeyword();
        if (token.kind() == SyntaxKind.CHECKPANIC_KEYWORD) {
            DiagnosticInfo info = new DiagnosticInfo(
                    "WEBSUB_101", "checkpanic detected, use check", DiagnosticSeverity.WARNING);
            Diagnostic diagnostic = DiagnosticFactory.createDiagnostic(info, checkExpressionNode.location());
            context.reportDiagnostic(diagnostic);
        }
    }
}

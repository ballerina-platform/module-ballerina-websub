package io.ballerina.stdlib.websub.validator;

import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.websub.WebSubDiagnosticCodes;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;

/**
 * {@code CheckPanicValidator} performs validations related to usage of `checkpanic` expression.
 */
public class CheckPanicValidator implements AnalysisTask<SyntaxNodeAnalysisContext> {
    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        CheckExpressionNode checkExpressionNode = (CheckExpressionNode) context.node();
        Token token = checkExpressionNode.checkKeyword();
        if (token.kind() == SyntaxKind.CHECKPANIC_KEYWORD) {
            WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_100;
            DiagnosticInfo info = new DiagnosticInfo(
                    errorCode.getCode(), errorCode.getDescription(), errorCode.getSeverity());
            Diagnostic diagnostic = DiagnosticFactory.createDiagnostic(info, checkExpressionNode.location());
            context.reportDiagnostic(diagnostic);
        }
    }
}

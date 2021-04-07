package io.ballerina.stdlib.websub.validator;

import io.ballerina.compiler.syntax.tree.NodeLocation;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.websub.WebSubDiagnosticCodes;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;

public final class ValidatorUtils {
    public static void updateContext(SyntaxNodeAnalysisContext context, WebSubDiagnosticCodes errorCode,
                                     NodeLocation location, Object... args) {
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                errorCode.getCode(), errorCode.getDescription(), errorCode.getSeverity());
        Diagnostic diagnostic = DiagnosticFactory.createDiagnostic(diagnosticInfo, location, args);
        context.reportDiagnostic(diagnostic);
    }
}

package io.ballerina.stdlib.websub.validator;

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.websub.WebSubDiagnosticCodes;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;

/**
 * {@code ServiceRemoteMethodsPresentValidator} validates whether all the methods in websub service declaration
 * are remote methods.
 */
public class ServiceRemoteMethodsPresentValidator implements AnalysisTask<SyntaxNodeAnalysisContext> {
    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        if (ValidatorUtils.isWebSubService(context)) {
            ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) context.node();
            serviceNode.members().stream()
                    .filter(member -> member.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION)
                    .map(member -> (FunctionDefinitionNode) member)
                    .filter(fd -> fd.qualifierList().stream().noneMatch(q -> q.kind() == SyntaxKind.REMOTE_KEYWORD))
                    .forEach(fd -> {
                        WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_102;
                        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                                errorCode.getCode(), errorCode.getDescription(), errorCode.getSeverity());
                        Diagnostic diagnostic = DiagnosticFactory
                                .createDiagnostic(diagnosticInfo, fd.location());
                        context.reportDiagnostic(diagnostic);
                    });
        }
    }
}

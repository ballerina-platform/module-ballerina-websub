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
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.List;
import java.util.stream.Collectors;

import static io.ballerina.stdlib.websub.Constants.*;

/**
 * {@code ServiceRequiredMethodValidator} validates whether all the
 * required method defined in service implementation.
 */
public class ServiceRequiredMethodValidator implements AnalysisTask<SyntaxNodeAnalysisContext> {
    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) context.node();
        if (ValidatorUtils.isWebSubService(context)) {
            List<FunctionDefinitionNode> declaredWebSubRelatedFunctions = serviceNode.members().stream()
                    .filter(member -> member.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION)
                    .map(member -> (FunctionDefinitionNode) member)
                    .filter(e -> {
                        String functionName = e.functionName().toString();
                        return functionName.equalsIgnoreCase(ON_SUBSCRIPTION_VALIDATION_DENIED)
                                || functionName.equalsIgnoreCase(ON_SUBSCRIPTION_VERIFICATION)
                                || functionName.equalsIgnoreCase(ON_EVENT_NOTIFICATION);
                    }).collect(Collectors.toList());
            if (declaredWebSubRelatedFunctions.isEmpty()) {
                WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_100;
                DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                        errorCode.getCode(), errorCode.getDescription(), DiagnosticSeverity.INFO);
                Diagnostic diagnostic = DiagnosticFactory
                        .createDiagnostic(diagnosticInfo, serviceNode.location());
                context.reportDiagnostic(diagnostic);

            }
        }
    }
}

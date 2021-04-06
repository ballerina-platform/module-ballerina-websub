package io.ballerina.stdlib.websub.validator;

import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeLocation;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.Document;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.websub.Constants;
import io.ballerina.stdlib.websub.WebSubDiagnosticCodes;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.text.LinePosition;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@code ServiceDeclarationValidator} validates whether websub service declaration is complying to current websub
 * package implementation.
 */
public class ServiceDeclarationValidator implements AnalysisTask<SyntaxNodeAnalysisContext> {
    private static final List<String> allowedMethods;
    private static final Map<String, List<String>> allowedParameterTypes;
    private static final Map<String, List<String>> allowedReturnTypes;

    static {
        allowedMethods = Arrays.asList(
                Constants.ON_SUBSCRIPTION_VALIDATION_DENIED,
                Constants.ON_SUBSCRIPTION_VERIFICATION,
                Constants.ON_EVENT_NOTIFICATION);
        allowedParameterTypes = Map.of(
                Constants.ON_SUBSCRIPTION_VALIDATION_DENIED,
                Collections.singletonList(Constants.SUBSCRIPTION_DENIED_ERROR),
                Constants.ON_SUBSCRIPTION_VERIFICATION,
                Collections.singletonList(Constants.SUBSCRIPTION_VERIFICATION),
                Constants.ON_EVENT_NOTIFICATION,
                Collections.singletonList(Constants.CONTENT_DISTRIBUTION_MESSAGE)
        );
        allowedReturnTypes = Map.of(
                Constants.ON_SUBSCRIPTION_VALIDATION_DENIED, Arrays.asList(""),
                Constants.ON_SUBSCRIPTION_VERIFICATION, Arrays.asList(""),
                Constants.ON_EVENT_NOTIFICATION, Arrays.asList("")
        );
    }

    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) context.node();
        if (isWebSubService(context, serviceNode)) {

            List<FunctionDefinitionNode> availableFunctionDeclarations = serviceNode.members().stream()
                    .filter(member -> member.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION)
                    .map(member -> (FunctionDefinitionNode) member).collect(Collectors.toList());

            validateRequiredMethodsImplemented(context, availableFunctionDeclarations, serviceNode.location());
            availableFunctionDeclarations.forEach(fd -> {
                validateRemoteQualifier(context, fd);
                validateAdditionalMethodImplemented(context, fd);
                validateMethodParameters(context, fd);
            });
//            List<ServiceDeclarationSymbol> serviceSymbols = context.semanticModel()
//                    .moduleSymbols().stream()
//                    .filter(e -> e.kind() == SymbolKind.SERVICE_DECLARATION)
//                    .map(s -> (ServiceDeclarationSymbol) s)
//                    .collect(Collectors.toList());

        }
    }

    private void validateRequiredMethodsImplemented(SyntaxNodeAnalysisContext context,
                                                    List<FunctionDefinitionNode> availableFunctionDeclarations,
                                                    NodeLocation location) {
        boolean isRequiredMethodNotAvailable = availableFunctionDeclarations.stream()
                .noneMatch(fd -> Constants.ON_EVENT_NOTIFICATION.equalsIgnoreCase(fd.functionName().toString()));
        if (isRequiredMethodNotAvailable) {
            WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_103;
            updateContext(context, errorCode, location);
        }
    }

    private void validateRemoteQualifier(SyntaxNodeAnalysisContext context,
                                         FunctionDefinitionNode functionDefinition) {
        NodeList<Token> qualifiers = functionDefinition.qualifierList();
        if (qualifiers.stream().noneMatch(q -> q.kind() == SyntaxKind.REMOTE_KEYWORD)) {
            WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_102;
            updateContext(context, errorCode, functionDefinition.location());
        }
    }

    private void validateAdditionalMethodImplemented(SyntaxNodeAnalysisContext context,
                                                     FunctionDefinitionNode functionDefinition) {
        String functionName = functionDefinition.functionName().toString();
        if (!allowedMethods.contains(functionName)) {
            WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_104;
            updateContext(context, errorCode, functionDefinition.location(), functionName);
        }
    }

    private void validateMethodParameters(SyntaxNodeAnalysisContext context,
                                          FunctionDefinitionNode functionDefinition) {
        String functionName = functionDefinition.functionName().toString();
        if (allowedMethods.contains(functionName)) {
            List<List<String>> allowedParams = allowedParameterTypes
                    .get(functionName)
                    .stream()
                    .map(param -> Arrays.stream(param.trim().split("\\|"))
                            .map(String::trim).collect(Collectors.toList())
                    ).collect(Collectors.toList());
            functionDefinition
                    .functionSignature().parameters()
                    .stream()
                    .map(param -> (RequiredParameterNode) param)
                    .filter(param -> isParamsNotAllowed(allowedParams, param))
                    .forEach(param -> {
                        String paramType = param.typeName().toString();
                        WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_105;
                        updateContext(
                                context, errorCode, functionDefinition.location(), paramType.trim(), functionName);
                    });
        }
    }

    private boolean isParamsNotAllowed(List<List<String>> allowedParams, RequiredParameterNode param) {
        String paramType = param.typeName().toString();
        return Arrays
                .stream(paramType.trim().split("\\|"))
                .map(String::trim)
                .anyMatch(paramsTypes ->
                        allowedParams.stream().noneMatch(p -> p.contains(paramsTypes))
                );
    }

    private void updateContext(SyntaxNodeAnalysisContext context, WebSubDiagnosticCodes errorCode,
                               NodeLocation location, Object... args) {
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                errorCode.getCode(), errorCode.getDescription(), errorCode.getSeverity());
        Diagnostic diagnostic = DiagnosticFactory.createDiagnostic(diagnosticInfo, location, args);
        context.reportDiagnostic(diagnostic);
    }

    private boolean isWebSubService(SyntaxNodeAnalysisContext context, ServiceDeclarationNode serviceNode) {
        SeparatedNodeList<ExpressionNode> expressions = serviceNode.expressions();
        Optional<TypeSymbol> moduleTypeDescriptor = expressions.stream()
                .filter(this::isListenerAttachingExpression)
                .map(e -> getTypeSymbol(context, e))
                .findFirst();
        if (moduleTypeDescriptor.isEmpty()) {
            return false;
        } else {
            TypeSymbol moduleType = moduleTypeDescriptor.get();
            String signature = moduleType.signature();
            return signature.contains(Constants.MODULE_NAME);
        }
    }

    private boolean isListenerAttachingExpression(ExpressionNode e) {
        return e.kind() == SyntaxKind.EXPLICIT_NEW_EXPRESSION || e.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE;
    }

    private TypeSymbol getTypeSymbol(SyntaxNodeAnalysisContext context, ExpressionNode e) {
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
    }
}

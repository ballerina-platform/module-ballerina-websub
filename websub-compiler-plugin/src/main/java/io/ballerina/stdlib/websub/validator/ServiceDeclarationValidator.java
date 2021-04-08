package io.ballerina.stdlib.websub.validator;

import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeLocation;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.Document;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.websub.Constants;
import io.ballerina.stdlib.websub.WebSubDiagnosticCodes;
import io.ballerina.tools.text.LinePosition;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.ballerina.stdlib.websub.validator.ValidatorUtils.updateContext;

/**
 * {@code ServiceDeclarationValidator} validates whether websub service declaration is complying to current websub
 * package implementation.
 */
public class ServiceDeclarationValidator implements AnalysisTask<SyntaxNodeAnalysisContext> {
    private static final List<String> allowedMethods;
    private static final Map<String, List<String>> allowedParameterTypes;
    private static final Map<String, List<String>> allowedReturnTypes;
    private static final List<String> methodsWithOptionalReturnTypes;

    static {
        allowedMethods = List.of(
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
                Constants.ON_SUBSCRIPTION_VALIDATION_DENIED,
                Collections.singletonList(Constants.ACKNOWLEDGEMENT),
                Constants.ON_SUBSCRIPTION_VERIFICATION,
                List.of(Constants.SUBSCRIPTION_VERIFICATION_SUCCESS, Constants.SUBSCRIPTION_VERIFICATION_ERROR),
                Constants.ON_EVENT_NOTIFICATION,
                List.of(Constants.ACKNOWLEDGEMENT, Constants.SUBSCRIPTION_DELETED_ERROR)
        );
        methodsWithOptionalReturnTypes = List.of(
                Constants.ON_SUBSCRIPTION_VALIDATION_DENIED,
                Constants.ON_EVENT_NOTIFICATION
        );
    }

    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) context.node();
        if (isWebSubService(context, serviceNode)) {
            validateServiceAnnotation(context, serviceNode);
            List<FunctionDefinitionNode> availableFunctionDeclarations = serviceNode.members().stream()
                    .filter(member -> member.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION)
                    .map(member -> (FunctionDefinitionNode) member).collect(Collectors.toList());
            validateRequiredMethodsImplemented(context, availableFunctionDeclarations, serviceNode.location());
            availableFunctionDeclarations.forEach(fd -> {
                validateRemoteQualifier(context, fd);
                validateAdditionalMethodImplemented(context, fd);
                validateMethodParameters(context, fd);
                validateMethodReturnTypes(context, fd);
            });
        }
    }

    private void validateServiceAnnotation(SyntaxNodeAnalysisContext context, ServiceDeclarationNode serviceNode) {
        Optional<ServiceDeclarationSymbol> serviceDeclarationOptional = context.semanticModel().moduleSymbols()
                .stream()
                .filter(e -> e.kind() == SymbolKind.SERVICE_DECLARATION)
                .map(s -> (ServiceDeclarationSymbol) s)
                .findFirst();
        if (serviceDeclarationOptional.isPresent()) {
            ServiceDeclarationSymbol serviceDeclarationSymbol = serviceDeclarationOptional.get();
            Optional<AnnotationSymbol> subscriberServiceAnnotationOptional = serviceDeclarationSymbol.annotations()
                    .stream()
                    .filter(annotationSymbol ->
                            annotationSymbol.getName().orElse("").equals(Constants.SERVICE_ANNOTATION_NAME)
                    ).findFirst();
            if (subscriberServiceAnnotationOptional.isEmpty()) {
                WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_101;
                updateContext(context, errorCode, serviceNode.location());
            }
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
            List<String> allowedParameters = allowedParameterTypes.get(functionName);
            List<List<String>> allowedParams = allowedParameters.stream()
                    .map(param -> Arrays.stream(param.trim().split("\\|"))
                            .map(String::trim).collect(Collectors.toList())
                    ).collect(Collectors.toList());
            SeparatedNodeList<ParameterNode> availableParameters = functionDefinition.functionSignature().parameters();
            if (availableParameters.size() >= 1) {
                availableParameters
                        .stream()
                        .map(param -> (RequiredParameterNode) param)
                        .filter(param -> isParamNotAllowed(allowedParams, param))
                        .forEach(param -> {
                            String paramType = param.typeName().toString();
                            WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_105;
                            updateContext(
                                    context, errorCode, functionDefinition.location(), paramType.trim(), functionName);
                        });
            } else {
                if (!allowedParameters.isEmpty()) {
                    WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_106;
                    updateContext(
                            context, errorCode, functionDefinition.location(), functionName,
                            String.join(",", allowedParameters));
                }
            }
        }
    }

    private boolean isParamNotAllowed(List<List<String>> allowedParams, RequiredParameterNode param) {
        String paramType = param.typeName().toString();
        return Arrays
                .stream(paramType.trim().split("\\|"))
                .map(String::trim)
                .anyMatch(paramsTypes ->
                        allowedParams.stream().noneMatch(p -> p.contains(paramsTypes))
                );
    }

    private void validateMethodReturnTypes(SyntaxNodeAnalysisContext context,
                                           FunctionDefinitionNode functionDefinition) {
        String functionName = functionDefinition.functionName().toString();
        List<String> predefinedReturnTypes = allowedReturnTypes.get(functionName);
        if (allowedMethods.contains(functionName)) {
            Optional<ReturnTypeDescriptorNode> returnTypesOpt = functionDefinition.functionSignature().returnTypeDesc();
            if (returnTypesOpt.isPresent()) {
                ReturnTypeDescriptorNode returnTypeDescription = returnTypesOpt.get();
                Node returnType = returnTypeDescription.type();
                String returnTypeName = returnType.toString().trim();
                List<String> availableReturnTypes = Arrays
                        .stream(returnTypeName.replace(Constants.OPTIONAL, "").split("\\|"))
                        .map(String::trim)
                        .collect(Collectors.toList());
                if (!predefinedReturnTypes.containsAll(availableReturnTypes)) {
                    WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_107;
                    updateContext(
                            context, errorCode, functionDefinition.location(),
                            returnTypeName, functionName);
                }
            } else {
                boolean isReturnTypeOptional = methodsWithOptionalReturnTypes.contains(functionName);
                if (!isReturnTypeOptional) {
                    WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_108;
                    updateContext(
                            context, errorCode, functionDefinition.location(), functionName,
                            String.join("|", predefinedReturnTypes));
                }
            }
        }
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
        Document currentDocument = context.currentPackage().getDefaultModule().document(context.documentId());
        LinePosition lineStart = e.lineRange().startLine();
        if (e.kind() == SyntaxKind.EXPLICIT_NEW_EXPRESSION) {
            return ((TypeReferenceTypeSymbol) context.semanticModel().symbol(currentDocument, lineStart).get())
                    .typeDescriptor();
        } else {
            return ((VariableSymbol) context.semanticModel().symbol(currentDocument, lineStart).get())
                    .typeDescriptor();
        }
    }
}

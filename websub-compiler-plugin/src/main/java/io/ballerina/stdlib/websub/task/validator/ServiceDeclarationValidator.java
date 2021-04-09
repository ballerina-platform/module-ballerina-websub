package io.ballerina.stdlib.websub.task.validator;

import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeLocation;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.websub.Constants;
import io.ballerina.stdlib.websub.WebSubDiagnosticCodes;
import io.ballerina.stdlib.websub.task.visitor.ListenerInitiationExpressionVisitor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.ballerina.stdlib.websub.task.AnalyserUtils.updateContext;

/**
 * {@code ServiceDeclarationValidator} validates whether websub service declaration is complying to current websub
 * package implementation.
 */
public class ServiceDeclarationValidator {
    private static final ServiceDeclarationValidator INSTANCE = new ServiceDeclarationValidator();
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

    public static ServiceDeclarationValidator getInstance() {
        return INSTANCE;
    }

    public void validateListenerArguments(SyntaxNodeAnalysisContext context,
                                          ListenerInitiationExpressionVisitor visitor) {
        visitor.getExplicitNewExpressionNodes()
                .forEach(explicitNewExpressionNode -> {
                    SeparatedNodeList<FunctionArgumentNode> functionArgs = explicitNewExpressionNode
                            .parenthesizedArgList().arguments();
                    verifyListenerArgType(context, explicitNewExpressionNode.location(), functionArgs);
                });

        visitor.getImplicitNewExpressionNodes()
                .forEach(implicitNewExpressionNode -> {
                    Optional<ParenthesizedArgList> argListOpt = implicitNewExpressionNode.parenthesizedArgList();
                    if (argListOpt.isPresent()) {
                        SeparatedNodeList<FunctionArgumentNode> functionArgs = argListOpt.get().arguments();
                        verifyListenerArgType(context, implicitNewExpressionNode.location(), functionArgs);
                    }
                });
    }

    private void verifyListenerArgType(SyntaxNodeAnalysisContext context, NodeLocation location,
                                       SeparatedNodeList<FunctionArgumentNode> functionArgs) {
        if (functionArgs.size() >= 2) {
            PositionalArgumentNode firstArg = (PositionalArgumentNode) functionArgs.get(0);
            PositionalArgumentNode secondArg = (PositionalArgumentNode) functionArgs.get(1);
            SyntaxKind firstArgSyntaxKind = firstArg.expression().kind();
            SyntaxKind secondArgSyntaxKind = secondArg.expression().kind();
            if ((firstArgSyntaxKind == SyntaxKind.SIMPLE_NAME_REFERENCE
                    || firstArgSyntaxKind == SyntaxKind.MAPPING_CONSTRUCTOR)
                    && (secondArgSyntaxKind == SyntaxKind.SIMPLE_NAME_REFERENCE
                    || secondArgSyntaxKind == SyntaxKind.MAPPING_CONSTRUCTOR)) {
                WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_109;
                updateContext(context, errorCode, location);
            }
        }
    }

    public void validateServiceAnnotation(SyntaxNodeAnalysisContext context, ServiceDeclarationNode serviceNode,
                                          ServiceDeclarationSymbol serviceDeclarationSymbol) {
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

    public void validateRequiredMethodsImplemented(SyntaxNodeAnalysisContext context,
                                                    List<FunctionDefinitionNode> availableFunctionDeclarations,
                                                    NodeLocation location) {
        boolean isRequiredMethodNotAvailable = availableFunctionDeclarations.stream()
                .noneMatch(fd -> Constants.ON_EVENT_NOTIFICATION.equalsIgnoreCase(fd.functionName().toString()));
        if (isRequiredMethodNotAvailable) {
            WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_103;
            updateContext(context, errorCode, location);
        }
    }

    public void validateRemoteQualifier(SyntaxNodeAnalysisContext context,
                                         FunctionDefinitionNode functionDefinition) {
        NodeList<Token> qualifiers = functionDefinition.qualifierList();
        if (qualifiers.stream().noneMatch(q -> q.kind() == SyntaxKind.REMOTE_KEYWORD)) {
            WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_102;
            updateContext(context, errorCode, functionDefinition.location());
        }
    }

    public void validateAdditionalMethodImplemented(SyntaxNodeAnalysisContext context,
                                                     FunctionDefinitionNode functionDefinition) {
        String functionName = functionDefinition.functionName().toString();
        if (!allowedMethods.contains(functionName)) {
            WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_104;
            updateContext(context, errorCode, functionDefinition.location(), functionName);
        }
    }

    public void validateMethodParameters(SyntaxNodeAnalysisContext context,
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
                        .filter(param -> {
                            Node paramTypeName = param.typeName();
                            Optional<Symbol> typeSymbolOpt = context.semanticModel().symbol(paramTypeName);
                            if (typeSymbolOpt.isPresent()) {
                                Symbol typeSymbol = typeSymbolOpt.get();
                                return typeSymbol instanceof TypeReferenceTypeSymbol
                                        && isParamNotAllowed(allowedParams, (TypeReferenceTypeSymbol) typeSymbol);
                            } else {
                                return true;
                            }
                        }).forEach(param -> {
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

    private boolean isParamNotAllowed(List<List<String>> allowedParams, TypeReferenceTypeSymbol param) {
        String moduleName = param.getModule().flatMap(ModuleSymbol::getName).orElse("");
        String paramType = param.getName().orElse("");
        return Arrays
                .stream(paramType.trim().split("\\|"))
                .map(String::trim)
                .anyMatch(paramsTypes -> {
                    String qualifiedParamType = moduleName.isBlank() ? paramType :
                            String.format("%s:%s", moduleName, paramType);
                    return allowedParams.stream().noneMatch(p -> p.contains(qualifiedParamType));
                });
    }

    public void validateMethodReturnTypes(SyntaxNodeAnalysisContext context,
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
}

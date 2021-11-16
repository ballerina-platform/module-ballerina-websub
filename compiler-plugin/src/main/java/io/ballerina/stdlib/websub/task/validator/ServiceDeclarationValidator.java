/*
 * Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.websub.task.validator;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ErrorTypeSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.NodeLocation;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.websub.Constants;
import io.ballerina.stdlib.websub.WebSubDiagnosticCodes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.ballerina.stdlib.websub.task.AnalyserUtils.getQualifiedType;
import static io.ballerina.stdlib.websub.task.AnalyserUtils.getTypeDescription;
import static io.ballerina.stdlib.websub.task.AnalyserUtils.isRemoteMethod;
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
                Constants.ON_UNSUBSCRIPTION_VERIFICATION,
                Constants.ON_EVENT_NOTIFICATION);
        allowedParameterTypes = Map.of(
                Constants.ON_SUBSCRIPTION_VALIDATION_DENIED,
                Collections.singletonList(Constants.SUBSCRIPTION_DENIED_ERROR),
                Constants.ON_SUBSCRIPTION_VERIFICATION,
                Collections.singletonList(Constants.SUBSCRIPTION_VERIFICATION),
                Constants.ON_UNSUBSCRIPTION_VERIFICATION,
                Collections.singletonList(Constants.UNSUBSCRIPTION_VERIFICATION),
                Constants.ON_EVENT_NOTIFICATION,
                Collections.singletonList(Constants.CONTENT_DISTRIBUTION_MESSAGE)
        );
        allowedReturnTypes = Map.of(
                Constants.ON_SUBSCRIPTION_VALIDATION_DENIED,
                Collections.singletonList(Constants.ACKNOWLEDGEMENT),
                Constants.ON_SUBSCRIPTION_VERIFICATION,
                List.of(Constants.SUBSCRIPTION_VERIFICATION_SUCCESS, Constants.SUBSCRIPTION_VERIFICATION_ERROR),
                Constants.ON_UNSUBSCRIPTION_VERIFICATION,
                List.of(Constants.UNSUBSCRIPTION_VERIFICATION_SUCCESS, Constants.UNSUBSCRIPTION_VERIFICATION_ERROR),
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

    public void validate(SyntaxNodeAnalysisContext context, ServiceDeclarationNode serviceNode,
                         ServiceDeclarationSymbol serviceDeclarationSymbol) {
        executeServiceAnnotationValidation(context, serviceNode, serviceDeclarationSymbol);
        List<FunctionDefinitionNode> availableFunctionDeclarations = serviceNode.members().stream()
                .filter(member -> member.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION)
                .map(member -> (FunctionDefinitionNode) member).collect(Collectors.toList());
        executeRequiredMethodValidation(context, availableFunctionDeclarations, serviceNode.location());
        availableFunctionDeclarations.forEach(fd -> {
            context.semanticModel().symbol(fd).ifPresent(fs -> {
                NodeLocation location = fd.location();
                executeRemoteMethodValidation(context, (FunctionSymbol) fs, location);
                executeMethodParameterValidation(context, fd, ((FunctionSymbol) fs).typeDescriptor());
                executeMethodReturnTypeValidation(context, fd, ((FunctionSymbol) fs).typeDescriptor());
            });
        });
    }

    private void executeServiceAnnotationValidation(SyntaxNodeAnalysisContext context,
                                                    ServiceDeclarationNode serviceNode,
                                                    ServiceDeclarationSymbol serviceDeclarationSymbol) {
        Optional<AnnotationSymbol> subscriberServiceAnnotationOptional = serviceDeclarationSymbol.annotations()
                .stream()
                .filter(annotationSymbol -> {
                            String moduleName = annotationSymbol.getModule()
                                    .flatMap(ModuleSymbol::getName)
                                    .orElse("");
                            String type = annotationSymbol.getName().orElse("");
                            String annotationName = getQualifiedType(type, moduleName);
                            return annotationName.equals(Constants.SERVICE_ANNOTATION_NAME);
                }).findFirst();
        if (subscriberServiceAnnotationOptional.isEmpty()) {
            WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_101;
            updateContext(context, errorCode, serviceNode.location());
        }
    }

    private void executeRequiredMethodValidation(SyntaxNodeAnalysisContext context,
                                                 List<FunctionDefinitionNode> availableFunctionDeclarations,
                                                 NodeLocation location) {
        boolean isRequiredMethodNotAvailable = availableFunctionDeclarations.stream()
                .noneMatch(fd -> Constants.ON_EVENT_NOTIFICATION.equalsIgnoreCase(fd.functionName().toString()));
        if (isRequiredMethodNotAvailable) {
            WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_103;
            updateContext(context, errorCode, location);
        }
    }

    private void executeRemoteMethodValidation(SyntaxNodeAnalysisContext context,
                                               FunctionSymbol functionSymbol, NodeLocation location) {
        Optional<String> functionNameOpt = functionSymbol.getName();
        if (functionNameOpt.isPresent()) {
            String functionName = functionNameOpt.get();
            boolean isRemoteMethod = isRemoteMethod(functionSymbol);
            boolean isAllowedMethod = allowedMethods.contains(functionName);
            // if its a `remote` method it should be an allowed-method
            if (isRemoteMethod && !isAllowedMethod) {
                WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_104;
                updateContext(context, errorCode, location, functionName);
            }
            // if its an allowed method it should be marked `remote`
            if (!isRemoteMethod && isAllowedMethod) {
                WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_102;
                updateContext(context, errorCode, location, functionName);
            }
        }
    }

    private void executeMethodParameterValidation(SyntaxNodeAnalysisContext context,
                                                  FunctionDefinitionNode functionDefinition,
                                                  FunctionTypeSymbol typeSymbol) {
        String functionName = functionDefinition.functionName().toString();
        if (allowedMethods.contains(functionName)) {
            List<String> allowedParameters = allowedParameterTypes.get(functionName);
            Optional<List<ParameterSymbol>> paramOpt = typeSymbol.params();
            if (paramOpt.isPresent()) {
                List<ParameterSymbol> params = paramOpt.get();
                if (params.isEmpty()) {
                    if (!allowedParameters.isEmpty()) {
                        WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_106;
                        updateContext(context, errorCode, functionDefinition.location(), functionName,
                                String.join("", allowedParameters));
                    }
                } else {
                    List<String> availableParamNames = params.stream()
                            .map(e -> getTypeDescription(e.typeDescriptor()))
                            .collect(Collectors.toList());
                    if (!allowedParameters.containsAll(availableParamNames)) {
                        List<String> notAllowedParams = availableParamNames.stream()
                                .filter(e -> !allowedParameters.contains(e))
                                .collect(Collectors.toList());
                        String message = String.join(",", notAllowedParams);
                        WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_105;
                        updateContext(context, errorCode, functionDefinition.location(), message, functionName);
                    }
                }
            } else {
                if (!allowedParameters.isEmpty()) {
                    WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_106;
                    updateContext(context, errorCode, functionDefinition.location(), functionName,
                            String.join("", allowedParameters));
                }
            }
        }
    }

    private void executeMethodReturnTypeValidation(SyntaxNodeAnalysisContext context,
                                                   FunctionDefinitionNode functionDefinition,
                                                   FunctionTypeSymbol typeSymbol) {
        String functionName = functionDefinition.functionName().toString();
        List<String> predefinedReturnTypes = allowedReturnTypes.get(functionName);
        boolean nilableReturnTypeAllowed = methodsWithOptionalReturnTypes.contains(functionName);
        if (allowedMethods.contains(functionName)) {
            Optional<TypeSymbol> returnTypesOpt = typeSymbol.returnTypeDescriptor();
            if (returnTypesOpt.isPresent()) {
                TypeSymbol returnTypeDescription = returnTypesOpt.get();
                if (returnTypeDescription.typeKind().equals(TypeDescKind.NIL) && !nilableReturnTypeAllowed) {
                    WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_108;
                    updateContext(context, errorCode, functionDefinition.location(), functionName,
                            String.join("|", predefinedReturnTypes));
                    return;
                }

                boolean invalidReturnTypePresent = isReturnTypeNotAllowed(
                        predefinedReturnTypes, returnTypeDescription, nilableReturnTypeAllowed);
                if (invalidReturnTypePresent) {
                    String returnTypeName = getTypeDescription(returnTypeDescription);
                    WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_107;
                    updateContext(context, errorCode, functionDefinition.location(), returnTypeName, functionName);
                }
            } else {
                if (!nilableReturnTypeAllowed) {
                    WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_108;
                    updateContext(context, errorCode, functionDefinition.location(), functionName,
                            String.join("|", predefinedReturnTypes));
                }
            }
        }
    }

    private boolean isReturnTypeNotAllowed(List<String> allowedReturnTypes, TypeSymbol returnTypeDescriptor,
                                           boolean nilableReturnTypeAllowed) {
        TypeDescKind typeKind = returnTypeDescriptor.typeKind();
        if (TypeDescKind.UNION.equals(typeKind)) {
            return ((UnionTypeSymbol) returnTypeDescriptor)
                    .memberTypeDescriptors().stream()
                    .map(e -> isReturnTypeNotAllowed(allowedReturnTypes, e, nilableReturnTypeAllowed))
                    .reduce(false, (a , b) -> a || b);
        } else if (TypeDescKind.TYPE_REFERENCE.equals(typeKind)) {
            TypeSymbol internalType = ((TypeReferenceTypeSymbol) returnTypeDescriptor).typeDescriptor();
            if (internalType instanceof ErrorTypeSymbol) {
                return isInvalidErrorReturn(allowedReturnTypes, returnTypeDescriptor);
            } else {
                String moduleName = returnTypeDescriptor.getModule().flatMap(ModuleSymbol::getName).orElse("");
                String paramType = returnTypeDescriptor.getName().orElse("");
                String qualifiedParamType = getQualifiedType(paramType, moduleName);
                return !allowedReturnTypes.contains(qualifiedParamType);
            }
        } else if (TypeDescKind.ERROR.equals(typeKind)) {
            return isInvalidErrorReturn(allowedReturnTypes, returnTypeDescriptor);
        } else if (TypeDescKind.NIL.equals(typeKind)) {
            return !nilableReturnTypeAllowed;
        } else {
            return true;
        }
    }

    private boolean isInvalidErrorReturn(List<String> allowedReturnTypes,
                                         TypeSymbol internalType) {
        String signature = internalType.signature();
        Optional<ModuleID> moduleIdOpt = internalType.getModule().map(ModuleSymbol::id);
        String moduleId = moduleIdOpt.map(ModuleID::toString).orElse("");
        String paramType = signature.replace(moduleId, "").replace(":", "");
        String moduleName = moduleIdOpt.map(ModuleID::modulePrefix).orElse("");
        String qualifiedParamType = getQualifiedType(paramType, moduleName);
        if (Constants.ERROR.equals(qualifiedParamType)) {
            return false;
        } else {
            return !allowedReturnTypes.contains(qualifiedParamType);
        }
    }
}

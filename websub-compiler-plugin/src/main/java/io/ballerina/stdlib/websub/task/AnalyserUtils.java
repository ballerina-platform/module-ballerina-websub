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

package io.ballerina.stdlib.websub.task;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.NodeLocation;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.websub.Constants;
import io.ballerina.stdlib.websub.WebSubDiagnosticCodes;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@code ValidatorUtils} contains utility functions required for {@code websub:SubscriberService} validation.
 */
public final class AnalyserUtils {
    public static void updateContext(SyntaxNodeAnalysisContext context, WebSubDiagnosticCodes errorCode,
                                     NodeLocation location, Object... args) {
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                errorCode.getCode(), errorCode.getDescription(), errorCode.getSeverity());
        Diagnostic diagnostic = DiagnosticFactory.createDiagnostic(diagnosticInfo, location, args);
        context.reportDiagnostic(diagnostic);
    }

    public static boolean isWebSubListener(TypeSymbol listenerType) {
        if (listenerType.typeKind() == TypeDescKind.UNION) {
            return ((UnionTypeSymbol) listenerType).memberTypeDescriptors().stream()
                    .filter(typeDescriptor -> typeDescriptor instanceof TypeReferenceTypeSymbol)
                    .map(typeReferenceTypeSymbol -> (TypeReferenceTypeSymbol) typeReferenceTypeSymbol)
                    .anyMatch(typeReferenceTypeSymbol ->
                            typeReferenceTypeSymbol.getModule().isPresent()
                                    && isWebSub(typeReferenceTypeSymbol.getModule().get()
                            ));
        }

        if (listenerType.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            Optional<ModuleSymbol> moduleOpt = ((TypeReferenceTypeSymbol) listenerType).typeDescriptor().getModule();
            return moduleOpt.isPresent() && isWebSub(moduleOpt.get());
        }

        if (listenerType.typeKind() == TypeDescKind.OBJECT) {
            Optional<ModuleSymbol> moduleOpt = ((ObjectTypeSymbol) listenerType).getModule();
            return moduleOpt.isPresent() && isWebSub(moduleOpt.get());
        }

        return false;
    }

    public static boolean isWebSub(ModuleSymbol moduleSymbol) {
        Optional<String> moduleNameOpt = moduleSymbol.getName();
        return moduleNameOpt.isPresent() && Constants.PACKAGE_NAME.equals(moduleNameOpt.get())
                && Constants.PACKAGE_ORG.equals(moduleSymbol.id().orgName());
    }

    public static String getParamTypeDescription(TypeSymbol paramType) {
        TypeDescKind paramKind = paramType.typeKind();
        if (TypeDescKind.TYPE_REFERENCE.equals(paramKind)) {
            String moduleName = paramType.getModule().flatMap(ModuleSymbol::getName).orElse("");
            String type = paramType.getName().orElse("");
            return getQualifiedType(type, moduleName);
        } else if (TypeDescKind.UNION.equals(paramKind)) {
            return ((UnionTypeSymbol) paramType)
                    .memberTypeDescriptors().stream()
                    .map(AnalyserUtils::getParamTypeDescription)
                    .filter(e -> !e.isEmpty() && !e.isBlank())
                    .reduce((a, b) -> String.join("|", a, b)).orElse("");
        } else if (TypeDescKind.ERROR.equals(paramKind)) {
            String signature = paramType.signature();
            Optional<ModuleID> moduleIdOpt = paramType.getModule().map(ModuleSymbol::id);
            String moduleId = moduleIdOpt.map(ModuleID::toString).orElse("");
            String type = signature.replace(moduleId, "").replace(":", "");
            String moduleName = moduleIdOpt.map(ModuleID::modulePrefix).orElse("");
            return getQualifiedType(type, moduleName);
        } else {
            return paramType.getName().orElse("");
        }
    }

    public static String getReturnTypeDescription(TypeSymbol paramType) {
        TypeDescKind typeKind = paramType.typeKind();
        if (TypeDescKind.TYPE_REFERENCE.equals(typeKind)) {
            String moduleName = paramType.getModule().flatMap(ModuleSymbol::getName).orElse("");
            String type = paramType.getName().orElse("");
            return getQualifiedType(type, moduleName);
        } else if (TypeDescKind.UNION.equals(typeKind)) {
            List<TypeSymbol> availableTypes = ((UnionTypeSymbol) paramType).memberTypeDescriptors();
            boolean optionalSymbolAvailable = availableTypes.stream()
                    .anyMatch(t -> TypeDescKind.NIL.equals(t.typeKind()));
            List<String> typeDescriptions = availableTypes.stream()
                    .map(AnalyserUtils::getReturnTypeDescription)
                    .filter(e -> !e.isEmpty() && !e.isBlank())
                    .collect(Collectors.toList());
            String concatenatedReturnTypes = String.join("|", typeDescriptions);
            return optionalSymbolAvailable ? concatenatedReturnTypes + Constants.OPTIONAL : concatenatedReturnTypes;
        } else if (TypeDescKind.ERROR.equals(typeKind)) {
            String signature = paramType.signature();
            Optional<ModuleID> moduleIdOpt = paramType.getModule().map(ModuleSymbol::id);
            String moduleId = moduleIdOpt.map(ModuleID::toString).orElse("");
            String type = signature.replace(moduleId, "").replace(":", "");
            String moduleName = moduleIdOpt.map(ModuleID::modulePrefix).orElse("");
            return getQualifiedType(type, moduleName);
        } else {
            return "";
        }
    }

    public static String getQualifiedType(String paramType, String moduleName) {
        return moduleName.isBlank() ? paramType : String.format("%s:%s", moduleName, paramType);
    }
}

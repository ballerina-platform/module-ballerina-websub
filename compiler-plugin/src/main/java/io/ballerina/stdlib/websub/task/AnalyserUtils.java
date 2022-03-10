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
import io.ballerina.compiler.api.symbols.ErrorTypeSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

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

    public static boolean isWebSubService(ServiceDeclarationSymbol serviceDeclarationSymbol) {
        return serviceDeclarationSymbol.listenerTypes().stream().anyMatch(AnalyserUtils::isWebSubListener);
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

    public static String getTypeDescription(TypeSymbol paramType) {
        TypeDescKind paramKind = paramType.typeKind();
        if (TypeDescKind.TYPE_REFERENCE.equals(paramKind)) {
            TypeSymbol internalType = ((TypeReferenceTypeSymbol) paramType).typeDescriptor();
            if (internalType instanceof ErrorTypeSymbol) {
                return getErrorTypeDescription(paramType);
            } else {
                String moduleName = paramType.getModule().flatMap(ModuleSymbol::getName).orElse("");
                String type = paramType.getName().orElse("");
                return getQualifiedType(type, moduleName);
            }
        } else if (TypeDescKind.UNION.equals(paramKind)) {
            List<TypeSymbol> availableTypes = ((UnionTypeSymbol) paramType).memberTypeDescriptors();
            boolean optionalSymbolAvailable = availableTypes.stream()
                    .anyMatch(t -> TypeDescKind.NIL.equals(t.typeKind()));
            String concatenatedTypeDesc = availableTypes.stream()
                    .map(AnalyserUtils::getTypeDescription)
                    .filter(e -> !e.isEmpty() && !e.isBlank())
                    .reduce((a, b) -> String.join("|", a, b)).orElse("");
            return optionalSymbolAvailable ? concatenatedTypeDesc + Constants.OPTIONAL : concatenatedTypeDesc;
        } else if (TypeDescKind.INTERSECTION.equals(paramKind)) {
            List<TypeSymbol> availableTypes = ((IntersectionTypeSymbol) paramType).memberTypeDescriptors();
            return availableTypes.stream()
                    .filter(e -> TypeDescKind.TYPE_REFERENCE.equals(e.typeKind()))
                    .map(AnalyserUtils::getTypeDescription)
                    .filter(e -> !e.isEmpty() && !e.isBlank())
                    .reduce((a, b) -> String.join("&", a, b)).orElse("");
        } else if (TypeDescKind.ERROR.equals(paramKind)) {
            return getErrorTypeDescription(paramType);
        } else {
            return paramType.getName().orElse("");
        }
    }

    private static String getErrorTypeDescription(TypeSymbol internalType) {
        String signature = internalType.signature();
        Optional<ModuleID> moduleIdOpt = internalType.getModule().map(ModuleSymbol::id);
        String moduleId = moduleIdOpt.map(ModuleID::toString).orElse("");
        String type = signature.replace(moduleId, "").replace(":", "");
        String moduleName = moduleIdOpt.map(ModuleID::modulePrefix).orElse("");
        return getQualifiedType(type, moduleName);
    }

    public static String getQualifiedType(String paramType, String moduleName) {
        return moduleName.isBlank() ? paramType : String.format("%s:%s", moduleName, paramType);
    }

    public static boolean isRemoteMethod(FunctionSymbol functionSymbol) {
        return functionSymbol.qualifiers().contains(Qualifier.REMOTE);
    }

    /**
     * Copy content of a file/directory into another location.
     *
     * @param inputStream stream from which the data is read
     * @param outStream stream to which the data is written
     * @throws IOException if there is any error while reading from a file or writing to a file
     */
    public static <T extends InputStream, E extends OutputStream> void copyContent(T inputStream, E outStream)
            throws IOException {
        byte[] data = new byte[1024];
        int bytesRead = inputStream.read(data);
        while (bytesRead != -1) {
            outStream.write(data, 0, bytesRead);
            bytesRead = inputStream.read(data);
        }
    }
}

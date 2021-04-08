package io.ballerina.stdlib.websub.validator;

import io.ballerina.compiler.api.symbols.ModuleSymbol;
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

import java.util.Optional;

/**
 * {@code ValidatorUtils} contains utility functions required for {@code websub:SubscriberService} validation.
 */
public final class ValidatorUtils {
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

        return false;
    }

    public static boolean isWebSub(ModuleSymbol moduleSymbol) {
        Optional<String> moduleNameOpt = moduleSymbol.getName();
        return moduleNameOpt.isPresent() && Constants.MODULE_NAME.equals(moduleNameOpt.get())
                && Constants.ORG_NAME.equals(moduleSymbol.id().orgName());
    }
}

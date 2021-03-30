package io.ballerina.stdlib.websub;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.CodeAnalysisContext;
import io.ballerina.projects.plugins.CodeAnalyzer;
import io.ballerina.stdlib.websub.validator.CheckPanicValidator;
import io.ballerina.stdlib.websub.validator.ServiceRemoteMethodsPresentValidator;
import io.ballerina.stdlib.websub.validator.ServiceRequiredMethodValidator;

/**
 * {@code WebSubCodeAnalyzer} handles syntax analysis for WebSub Services.
 */
public class WebSubCodeAnalyzer extends CodeAnalyzer {
    @Override
    public void init(CodeAnalysisContext codeAnalysisContext) {
        codeAnalysisContext.addSyntaxNodeAnalysisTask(
                new CheckPanicValidator(), SyntaxKind.CHECK_EXPRESSION);
//        codeAnalysisContext.addSyntaxNodeAnalysisTask(
//                new ServiceAnnotationValidator(),
//                SyntaxKind.SERVICE_DECLARATION);
        codeAnalysisContext.addSyntaxNodeAnalysisTask(
                new ServiceRemoteMethodsPresentValidator(),
                SyntaxKind.SERVICE_DECLARATION);
        codeAnalysisContext.addSyntaxNodeAnalysisTask(
                new ServiceRequiredMethodValidator(),
                SyntaxKind.SERVICE_DECLARATION);
//        codeAnalysisContext.addSyntaxNodeAnalysisTask(
//                new ServiceAdditionalMethodsNotAllowedValidator(),
//                SyntaxKind.SERVICE_DECLARATION);
    }
}

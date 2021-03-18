package io.ballerina.stdlib.websub;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.CodeAnalysisContext;
import io.ballerina.projects.plugins.CodeAnalyzer;
import io.ballerina.stdlib.websub.validator.CheckPanicValidator;

/**
 * {@code WebSubCodeAnalyzer} handles syntax analysis for WebSub Services.
 */
public class WebSubCodeAnalyzer extends CodeAnalyzer {
    @Override
    public void init(CodeAnalysisContext codeAnalysisContext) {
        codeAnalysisContext.addSyntaxNodeAnalysisTask(new CheckPanicValidator(), SyntaxKind.CHECK_EXPRESSION);
    }
}

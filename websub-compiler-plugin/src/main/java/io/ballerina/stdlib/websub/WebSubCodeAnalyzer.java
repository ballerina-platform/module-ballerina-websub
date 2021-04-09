package io.ballerina.stdlib.websub;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.CodeAnalysisContext;
import io.ballerina.projects.plugins.CodeAnalyzer;
import io.ballerina.stdlib.websub.task.CheckExpAnalysisTask;
import io.ballerina.stdlib.websub.task.ServiceAnalysisTask;

/**
 * {@code WebSubCodeAnalyzer} handles syntax analysis for WebSub Services.
 */
public class WebSubCodeAnalyzer extends CodeAnalyzer {
    @Override
    public void init(CodeAnalysisContext codeAnalysisContext) {
        codeAnalysisContext.addSyntaxNodeAnalysisTask(new CheckExpAnalysisTask(), SyntaxKind.CHECK_EXPRESSION);
        codeAnalysisContext.addSyntaxNodeAnalysisTask(new ServiceAnalysisTask(),
                SyntaxKind.SERVICE_DECLARATION);
    }
}

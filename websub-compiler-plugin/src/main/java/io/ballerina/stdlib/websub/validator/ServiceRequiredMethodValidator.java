package io.ballerina.stdlib.websub.validator;

import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;

/**
 * {@code ServiceRequiredMethodValidator} validates whether all the
 * required method defined in service-type is implemented.
 */
public class ServiceRequiredMethodValidator implements AnalysisTask<SyntaxNodeAnalysisContext> {
    @Override
    public void perform(SyntaxNodeAnalysisContext context) {

    }
}

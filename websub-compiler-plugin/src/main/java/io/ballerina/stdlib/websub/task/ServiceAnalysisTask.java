package io.ballerina.stdlib.websub.task;

import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.websub.task.validator.ServiceDeclarationValidator;
import io.ballerina.stdlib.websub.task.visitor.ListenerInitiationExpressionVisitor;

import java.util.Optional;

/**
 * {@code ServiceDeclarationValidator} validates whether websub service declaration is complying to current websub
 * package implementation.
 */
public class ServiceAnalysisTask implements AnalysisTask<SyntaxNodeAnalysisContext> {
    private final ServiceDeclarationValidator validator;

    public ServiceAnalysisTask() {
        this.validator = ServiceDeclarationValidator.getInstance();
    }

    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) context.node();
        Optional<Symbol> serviceDeclarationOpt = context.semanticModel().symbol(serviceNode);
        if (serviceDeclarationOpt.isPresent()) {
            ListenerInitiationExpressionVisitor visitor = new ListenerInitiationExpressionVisitor(context);
            serviceNode.syntaxTree().rootNode().accept(visitor);
            ServiceDeclarationSymbol serviceDeclarationSymbol = (ServiceDeclarationSymbol) serviceDeclarationOpt.get();
            if (isWebSubService(serviceDeclarationSymbol)) {
                this.validator.validate(context, serviceNode, visitor, serviceDeclarationSymbol);
            }
        }
    }

    private boolean isWebSubService(ServiceDeclarationSymbol serviceDeclarationSymbol) {
        return serviceDeclarationSymbol.listenerTypes().stream().anyMatch(AnalyserUtils::isWebSubListener);
    }
}

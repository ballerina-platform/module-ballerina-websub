package io.ballerina.stdlib.websub.task;

import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.websub.task.validator.ServiceDeclarationValidator;
import io.ballerina.stdlib.websub.task.visitor.ListenerInitiationExpressionVisitor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
                this.validator.validateListenerArguments(context, visitor);
                this.validator.validateServiceAnnotation(context, serviceNode, serviceDeclarationSymbol);
                List<FunctionDefinitionNode> availableFunctionDeclarations = serviceNode.members().stream()
                        .filter(member -> member.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION)
                        .map(member -> (FunctionDefinitionNode) member).collect(Collectors.toList());
                this.validator.validateRequiredMethodsImplemented(
                        context, availableFunctionDeclarations, serviceNode.location());
                availableFunctionDeclarations.forEach(fd -> {
                    this.validator.validateRemoteQualifier(context, fd);
                    this.validator.validateAdditionalMethodImplemented(context, fd);
                    this.validator.validateMethodParameters(context, fd);
                    this.validator.validateMethodReturnTypes(context, fd);
                });
            }
        }
    }

    private boolean isWebSubService(ServiceDeclarationSymbol serviceDeclarationSymbol) {
        return serviceDeclarationSymbol.listenerTypes().stream().anyMatch(AnalyserUtils::isWebSubListener);
    }
}

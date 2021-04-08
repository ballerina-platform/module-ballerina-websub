package io.ballerina.stdlib.websub.validator.visitor;

import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.stdlib.websub.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code ListenerDeclarationVisitor} find the available listener declarations in the current document.
 */
public class ListenerInitiationExpressionVisitor extends NodeVisitor {
    private final List<ImplicitNewExpressionNode> implicitNewExpressionNodes = new ArrayList<>();
    private final List<ExplicitNewExpressionNode> explicitNewExpressionNodes = new ArrayList<>();


    @Override
    public void visit(ImplicitNewExpressionNode node) {
        if (node.parent() instanceof ListenerDeclarationNode) {
            ListenerDeclarationNode parentNode = (ListenerDeclarationNode) node.parent();
            Optional<TypeDescriptorNode> parentTypeOpt = parentNode.typeDescriptor();
            if (parentTypeOpt.isPresent()) {
                QualifiedNameReferenceNode parentType = (QualifiedNameReferenceNode) parentTypeOpt.get();
                String module = parentType.modulePrefix().toString().trim();
                String identifier = parentType.identifier().toString().trim();
                if (isWebSubListener(module, identifier)) {
                    implicitNewExpressionNodes.add(node);
                }
            }
        }
    }

    @Override
    public void visit(ExplicitNewExpressionNode node) {
        QualifiedNameReferenceNode nameRef = (QualifiedNameReferenceNode) node.typeDescriptor();
        String module = nameRef.modulePrefix().toString().trim();
        String identifier = nameRef.identifier().toString().trim();
        if (isWebSubListener(module, identifier)) {
            explicitNewExpressionNodes.add(node);
        }
    }

    private boolean isWebSubListener(String module, String identifier) {
        return Constants.MODULE_NAME.equals(module) && Constants.LISTENER_IDENTIFIER.equals(identifier);
    }

    public List<ImplicitNewExpressionNode> getImplicitNewExpressionNodes() {
        return implicitNewExpressionNodes;
    }

    public List<ExplicitNewExpressionNode> getExplicitNewExpressionNodes() {
        return explicitNewExpressionNodes;
    }
}

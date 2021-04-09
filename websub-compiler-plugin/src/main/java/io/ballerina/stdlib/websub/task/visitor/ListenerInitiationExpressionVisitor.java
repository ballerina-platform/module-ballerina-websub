package io.ballerina.stdlib.websub.task.visitor;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
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

    private final SyntaxNodeAnalysisContext context;

    public ListenerInitiationExpressionVisitor(SyntaxNodeAnalysisContext context) {
        this.context = context;
    }

    @Override
    public void visit(ImplicitNewExpressionNode node) {
        if (node.parent() instanceof ListenerDeclarationNode) {
            ListenerDeclarationNode parentNode = (ListenerDeclarationNode) node.parent();
            Optional<TypeDescriptorNode> parentTypeOpt = parentNode.typeDescriptor();
            if (parentTypeOpt.isPresent()) {
                QualifiedNameReferenceNode parentType = (QualifiedNameReferenceNode) parentTypeOpt.get();
                Optional<Symbol> parentSymbolOpt = context.semanticModel().symbol(parentType);
                if (parentSymbolOpt.isPresent() && parentSymbolOpt.get() instanceof TypeReferenceTypeSymbol) {
                    TypeSymbol typeSymbol = ((TypeReferenceTypeSymbol) parentSymbolOpt.get()).typeDescriptor();
                    if (typeSymbol.typeKind() == TypeDescKind.UNION) {
                        Optional<TypeSymbol> refSymbolOpt = ((UnionTypeSymbol) typeSymbol).memberTypeDescriptors()
                                .stream().filter(e -> e.typeKind() == TypeDescKind.TYPE_REFERENCE).findFirst();
                        if (refSymbolOpt.isPresent()) {
                            TypeReferenceTypeSymbol refSymbol = (TypeReferenceTypeSymbol) refSymbolOpt.get();
                            TypeSymbol typeDescriptor = refSymbol.typeDescriptor();
                            Optional<ModuleID> moduleId = typeDescriptor.getModule().map(ModuleSymbol::id);
                            String identifier = typeDescriptor.getName().orElse("");
                            if (moduleId.isPresent() && isWebSubListener(moduleId.get(), identifier)) {
                                implicitNewExpressionNodes.add(node);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void visit(ExplicitNewExpressionNode node) {
        QualifiedNameReferenceNode nameRef = (QualifiedNameReferenceNode) node.typeDescriptor();
        Optional<Symbol> symbolOpt = context.semanticModel().symbol(nameRef);
        if (symbolOpt.isPresent() && symbolOpt.get() instanceof TypeReferenceTypeSymbol) {
            TypeSymbol typeSymbol = ((TypeReferenceTypeSymbol) symbolOpt.get()).typeDescriptor();
            if (typeSymbol.typeKind() == TypeDescKind.UNION) {
                Optional<TypeSymbol> refSymbolOpt = ((UnionTypeSymbol) typeSymbol).memberTypeDescriptors()
                        .stream().filter(e -> e.typeKind() == TypeDescKind.TYPE_REFERENCE).findFirst();
                if (refSymbolOpt.isPresent()) {
                    TypeReferenceTypeSymbol refSymbol = (TypeReferenceTypeSymbol) refSymbolOpt.get();
                    TypeSymbol typeDescriptor = refSymbol.typeDescriptor();
                    Optional<ModuleID> moduleId = typeDescriptor.getModule().map(ModuleSymbol::id);
                    String identifier = typeDescriptor.getName().orElse("");
                    if (moduleId.isPresent() && isWebSubListener(moduleId.get(), identifier)) {
                        explicitNewExpressionNodes.add(node);
                    }
                }
            }
        }
    }

    private boolean isWebSubListener(ModuleID moduleID, String identifier) {
        String orgName = moduleID.orgName();
        String packagePrefix = moduleID.modulePrefix();
        return Constants.PACKAGE_ORG.equals(orgName) && Constants.PACKAGE_NAME.equals(packagePrefix)
                && Constants.LISTENER_IDENTIFIER.equals(identifier);
    }

    public List<ImplicitNewExpressionNode> getImplicitNewExpressionNodes() {
        return implicitNewExpressionNodes;
    }

    public List<ExplicitNewExpressionNode> getExplicitNewExpressionNodes() {
        return explicitNewExpressionNodes;
    }
}

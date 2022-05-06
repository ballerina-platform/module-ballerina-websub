/*
 * Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.plugins.ModifierTask;
import io.ballerina.projects.plugins.SourceModifierContext;
import io.ballerina.stdlib.websub.Constants;
import io.ballerina.stdlib.websub.task.service.path.ServicePathContext;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static io.ballerina.stdlib.websub.task.service.path.ServicePathContextHandler.getContextHandler;

/**
 * {@code ServiceMetaInfoUpdaterTask} modifies the source by adding required meta-info for the websub service
 * declarations.
 */
public class ServiceMetaInfoUpdaterTask implements ModifierTask<SourceModifierContext> {
    @Override
    public void modify(SourceModifierContext context) {
        boolean erroneousCompilation = context.compilation().diagnosticResult()
                .diagnostics().stream()
                .anyMatch(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()));
        // if the compilation already contains any error, do not proceed
        if (erroneousCompilation) {
            return;
        }

        for (ModuleId modId : context.currentPackage().moduleIds()) {
            Module currentModule = context.currentPackage().module(modId);
            SemanticModel semanticModel = context.compilation().getSemanticModel(modId);
            for (DocumentId docId : currentModule.documentIds()) {
                Optional<ServicePathContext> servicePathContextOpt = getContextHandler().retrieveContext(modId, docId);
                // if the shared service-path generation context not found, do not proceed
                if (servicePathContextOpt.isEmpty()) {
                    continue;
                }
                List<ServicePathContext.ServicePathInformation> servicePathDetails = servicePathContextOpt.get()
                        .getServicePathDetails();
                if (servicePathDetails.isEmpty()) {
                    continue;
                }

                Document currentDoc = currentModule.document(docId);
                ModulePartNode rootNode = currentDoc.syntaxTree().rootNode();
                NodeList<ModuleMemberDeclarationNode> newMembers = updateMemberNodes(
                        rootNode.members(), servicePathDetails, semanticModel);
                ModulePartNode newModulePart =
                        rootNode.modify(rootNode.imports(), newMembers, rootNode.eofToken());
                SyntaxTree updatedSyntaxTree = currentDoc.syntaxTree().modifyWith(newModulePart);
                context.modifySourceFile(updatedSyntaxTree.textDocument(), docId);
            }
        }

        // for test files
        for (ModuleId modId : context.currentPackage().moduleIds()) {
            Module currentModule = context.currentPackage().module(modId);
            SemanticModel semanticModel = context.compilation().getSemanticModel(modId);
            for (DocumentId docId : currentModule.testDocumentIds()) {
                Optional<ServicePathContext> servicePathContextOpt = getContextHandler().retrieveContext(modId, docId);
                // if the shared service-path generation context not found, do not proceed
                if (servicePathContextOpt.isEmpty()) {
                    continue;
                }
                List<ServicePathContext.ServicePathInformation> servicePathDetails = servicePathContextOpt.get()
                        .getServicePathDetails();
                if (servicePathDetails.isEmpty()) {
                    continue;
                }

                Document currentDoc = currentModule.document(docId);
                ModulePartNode rootNode = currentDoc.syntaxTree().rootNode();
                NodeList<ModuleMemberDeclarationNode> newMembers = updateMemberNodes(
                        rootNode.members(), servicePathDetails, semanticModel);
                ModulePartNode newModulePart =
                        rootNode.modify(rootNode.imports(), newMembers, rootNode.eofToken());
                SyntaxTree updatedSyntaxTree = currentDoc.syntaxTree().modifyWith(newModulePart);
                context.modifyTestSourceFile(updatedSyntaxTree.textDocument(), docId);
            }
        }
    }

    private NodeList<ModuleMemberDeclarationNode> updateMemberNodes(NodeList<ModuleMemberDeclarationNode> oldMembers,
                                                                    List<ServicePathContext.ServicePathInformation> sd,
                                                                    SemanticModel semanticModel) {
        List<ModuleMemberDeclarationNode> updatedMembers = new LinkedList<>();
        for (ModuleMemberDeclarationNode memberNode : oldMembers) {
            if (memberNode.kind() != SyntaxKind.SERVICE_DECLARATION) {
                updatedMembers.add(memberNode);
                continue;
            }
            ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) memberNode;
            Optional<ServicePathContext.ServicePathInformation> servicePathInfoOpt = semanticModel.symbol(serviceNode)
                    .flatMap(symbol ->
                            sd.stream()
                                    .filter(service -> service.getServiceId() == symbol.hashCode())
                                    .findFirst());
            if (servicePathInfoOpt.isEmpty()) {
                updatedMembers.add(memberNode);
                continue;
            }
            ServicePathContext.ServicePathInformation servicePathInfo = servicePathInfoOpt.get();
            Optional<MetadataNode> metadata = serviceNode.metadata();
            if (metadata.isPresent()) {
                MetadataNode metadataNode = metadata.get();
                MetadataNode.MetadataNodeModifier modifier = metadataNode.modify();
                NodeList<AnnotationNode> updatedAnnotations = updateAnnotations(
                        metadataNode.annotations(), servicePathInfo.getServicePath());
                modifier.withAnnotations(updatedAnnotations);
                MetadataNode updatedMetadataNode = modifier.apply();
                ServiceDeclarationNode.ServiceDeclarationNodeModifier serviceDecModifier = serviceNode.modify();
                serviceDecModifier.withMetadata(updatedMetadataNode);
                ServiceDeclarationNode updatedServiceDecNode = serviceDecModifier.apply();
                updatedMembers.add(updatedServiceDecNode);
            }
        }
        return AbstractNodeFactory.createNodeList(updatedMembers);
    }

    private NodeList<AnnotationNode> updateAnnotations(NodeList<AnnotationNode> currentAnnotations,
                                                       String servicePath) {
        NodeList<AnnotationNode> updatedAnnotations = NodeFactory.createNodeList();
        for (AnnotationNode annotation: currentAnnotations) {
            if (isSubscriberServiceConfig(annotation)) {
                SeparatedNodeList<MappingFieldNode> updatedFields = getUpdatedFields(annotation, servicePath);
                MappingConstructorExpressionNode annotationValue =
                        NodeFactory.createMappingConstructorExpressionNode(
                                NodeFactory.createToken(SyntaxKind.OPEN_BRACE_TOKEN), updatedFields,
                                NodeFactory.createToken(SyntaxKind.CLOSE_BRACE_TOKEN));
                annotation = annotation.modify().withAnnotValue(annotationValue).apply();
            }
            updatedAnnotations = updatedAnnotations.add(annotation);
        }
        return updatedAnnotations;
    }

    private SeparatedNodeList<MappingFieldNode> getUpdatedFields(AnnotationNode annotation, String servicePath) {
        Optional<MappingConstructorExpressionNode> annotationValueOpt = annotation.annotValue();
        if (annotationValueOpt.isEmpty()) {
            return NodeFactory.createSeparatedNodeList(createServicePathField(servicePath));
        }
        List<Node> fields = new ArrayList<>();
        MappingConstructorExpressionNode annotationValue = annotationValueOpt.get();
        SeparatedNodeList<MappingFieldNode> existingFields = annotationValue.fields();
        Token separator = NodeFactory.createToken(SyntaxKind.COMMA_TOKEN);
        for (MappingFieldNode field : existingFields) {
            fields.add(field);
            fields.add(separator);
        }
        fields.add(createServicePathField(servicePath));
        return NodeFactory.createSeparatedNodeList(fields);
    }

    private static SpecificFieldNode createServicePathField(String value) {
        IdentifierToken fieldName = AbstractNodeFactory.createIdentifierToken(Constants.SERVICE_PATH_FIELD);
        Token colonToken = AbstractNodeFactory.createToken(SyntaxKind.COLON_TOKEN);
        String encodedValue = Base64.getEncoder().encodeToString(value.getBytes(Charset.defaultCharset()));
        ExpressionNode expressionNode = NodeParser.parseExpression(
                String.format("base64 `%s`.cloneReadOnly()", encodedValue));
        return NodeFactory.createSpecificFieldNode(null, fieldName, colonToken, expressionNode);
    }

    private boolean isSubscriberServiceConfig(AnnotationNode annotationNode) {
        QualifiedNameReferenceNode referenceNode = ((QualifiedNameReferenceNode) annotationNode.annotReference());
        if (!Constants.PACKAGE_NAME.equals(referenceNode.modulePrefix().text())) {
            return false;
        }
        return Constants.SERVICE_CONFIG_ANNOTATTION_TYPE.equals(referenceNode.identifier().text());
    }
}

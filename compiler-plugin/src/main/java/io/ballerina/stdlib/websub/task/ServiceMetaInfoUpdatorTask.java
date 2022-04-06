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

import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.plugins.ModifierTask;
import io.ballerina.projects.plugins.SourceModifierContext;
import io.ballerina.stdlib.websub.task.service.path.ServicePathContext;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.List;
import java.util.Optional;

import static io.ballerina.stdlib.websub.task.service.path.ServicePathContextHandler.getContextHandler;

/**
 * {@code serviceMetaInfoUpdatorTask} modifies the source by adding required meta-info for the websub service
 * declarations.
 */
public class ServiceMetaInfoUpdatorTask implements ModifierTask<SourceModifierContext> {
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
                NodeList<ModuleMemberDeclarationNode> members = rootNode.members();
                for (ModuleMemberDeclarationNode memberNode : members) {
                    if (memberNode.kind() != SyntaxKind.SERVICE_DECLARATION) {
                        return;
                    }
                    ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) memberNode;
                    Optional<MetadataNode> metadata = serviceNode.metadata();
                    if (metadata.isEmpty()) {
                        return;
                    }
                    MetadataNode metadataNode = metadata.get();
                    NodeList<AnnotationNode> oldAnnotations = metadataNode.annotations();
                    MetadataNode.MetadataNodeModifier modifier = metadataNode.modify();
                    // todo: implement annotation generation logic here
                    AnnotationNode newAnnotation = NodeFactory.createAnnotationNode(null, null, null);
                    modifier.withAnnotations(oldAnnotations.add(newAnnotation));
                    MetadataNode updatedMetadataNode = modifier.apply();
                    ServiceDeclarationNode.ServiceDeclarationNodeModifier serviceDecModifier = serviceNode.modify();
                    serviceDecModifier.withMetadata(updatedMetadataNode);
                    ServiceDeclarationNode updatedServiceDecNode = serviceDecModifier.apply();
                    if (updatedServiceDecNode != null) {
                        return;
                    }
                }
            }
        }
    }
}

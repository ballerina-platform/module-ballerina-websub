/*
 * Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.projects.Package;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.websub.Constants;
import io.ballerina.stdlib.websub.WebSubDiagnosticCodes;
import io.ballerina.stdlib.websub.task.service.path.ServicePathGeneratorException;
import io.ballerina.stdlib.websub.task.service.path.ServicePathGeneratorManager;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.Optional;

import static io.ballerina.stdlib.websub.task.AnalyserUtils.getQualifiedType;
import static io.ballerina.stdlib.websub.task.AnalyserUtils.isWebSubService;
import static io.ballerina.stdlib.websub.task.AnalyserUtils.updateContext;

/**
 * {@code WebSubServiceInfoGeneratorTask} generates service-path information for websub subscriber services.
 */
public class WebSubServiceInfoGeneratorTask implements AnalysisTask<SyntaxNodeAnalysisContext> {
    private final ServicePathGeneratorManager servicePathGenerator;

    public WebSubServiceInfoGeneratorTask() {
        this.servicePathGenerator = new ServicePathGeneratorManager();
    }

    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        SemanticModel semanticModel = context.semanticModel();
        boolean erroneousCompilation = semanticModel.diagnostics().stream()
                .anyMatch(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()));
        // if the compilation already contains any error, do not proceed
        if (erroneousCompilation) {
            return;
        }

        Package currentPackage = context.currentPackage();
        ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) context.node();
        Optional<Symbol> serviceDeclarationOpt = semanticModel.symbol(serviceNode);
        if (serviceDeclarationOpt.isPresent()) {
            ServiceDeclarationSymbol serviceDeclarationSymbol = (ServiceDeclarationSymbol) serviceDeclarationOpt.get();
            if (!isWebSubService(serviceDeclarationSymbol)) {
                return;
            }
            if (!shouldGenerateServicePath(serviceNode, semanticModel)) {
                return;
            }
            generateUniqueServicePath(context, currentPackage, serviceDeclarationSymbol.hashCode(), serviceNode);
        }
    }

    private boolean shouldGenerateServicePath(ServiceDeclarationNode serviceNode, SemanticModel semanticModel) {
        Optional<AnnotationNode> subscriberConfigOpt = getSubscriberConfigAnnotation(serviceNode, semanticModel);
        if (subscriberConfigOpt.isEmpty()) {
            return false;
        }
        AnnotationNode subscriberConfig = subscriberConfigOpt.get();

        // - scenario 1
        //  1. developer has not configured callback URL
        //  2. developer has not configured a service path
        Optional<String> callbackUrl = retrieveValueForAnnotationFields(subscriberConfig, Constants.CALLBACK);
        boolean servicePathAvailable = serviceNode.absoluteResourcePath().size() >= 1;
        if (callbackUrl.isEmpty() && !servicePathAvailable) {
            return true;
        }

        // - scenario 2
        //  1. developer has configured the callback URL
        //  2. developer has enabled `appendServicePath` configuration
        //  3. developer has not configured a service path
        Optional<String> appendServicePath = retrieveValueForAnnotationFields(
                subscriberConfig, Constants.APPEND_SERVICE_PATH);
        return callbackUrl.isPresent()
                && (appendServicePath.isPresent() && Boolean.parseBoolean(appendServicePath.get()))
                && !servicePathAvailable;
    }

    private Optional<AnnotationNode> getSubscriberConfigAnnotation(ServiceDeclarationNode serviceNode,
                                                                   SemanticModel semanticModel) {
        Optional<MetadataNode> metadata = serviceNode.metadata();
        if (metadata.isEmpty()) {
            return Optional.empty();
        }
        MetadataNode metaData = metadata.get();
        NodeList<AnnotationNode> annotations = metaData.annotations();
        return annotations.stream()
                .filter(ann -> isSubscriberConfigAnnotation(ann, semanticModel))
                .findFirst();
    }

    private boolean isSubscriberConfigAnnotation(AnnotationNode annotation, SemanticModel semanticModel) {
        Optional<Symbol> symbolOpt = semanticModel.symbol(annotation);
        if (symbolOpt.isEmpty()) {
            return false;
        }

        Symbol symbol = symbolOpt.get();
        if (!(symbol instanceof AnnotationSymbol)) {
            return false;
        }

        AnnotationSymbol annotationSymbol = (AnnotationSymbol) symbol;
        String moduleName = annotationSymbol.getModule().flatMap(ModuleSymbol::getName).orElse("");
        String type = annotationSymbol.getName().orElse("");
        String annotationName = getQualifiedType(type, moduleName);
        return Constants.SERVICE_ANNOTATION_NAME.equals(annotationName);
    }

    private Optional<String> retrieveValueForAnnotationFields(AnnotationNode serviceInfoAnnotation, String fieldName) {
        return serviceInfoAnnotation
                .annotValue()
                .map(MappingConstructorExpressionNode::fields)
                .flatMap(fields ->
                        fields.stream()
                                .filter(fld -> fld instanceof SpecificFieldNode)
                                .map(fld -> (SpecificFieldNode) fld)
                                .filter(fld -> fieldName.equals(fld.fieldName().toString().trim()))
                                .findFirst()
                ).flatMap(SpecificFieldNode::valueExpr)
                .map(en -> en.toString().trim());
    }

    private void generateUniqueServicePath(SyntaxNodeAnalysisContext context, Package currentPackage,
                                           int serviceId, ServiceDeclarationNode serviceNode) {
        try {
            this.servicePathGenerator.generate(currentPackage, serviceId);
        } catch (ServicePathGeneratorException ex) {
            String errorMsg = ex.getLocalizedMessage();
            WebSubDiagnosticCodes errorCode = WebSubDiagnosticCodes.WEBSUB_200;
            updateContext(context, errorCode, serviceNode.location(), errorMsg);
        }
    }
}

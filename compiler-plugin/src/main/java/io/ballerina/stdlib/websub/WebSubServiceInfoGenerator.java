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

package io.ballerina.stdlib.websub;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.CodeGenerator;
import io.ballerina.projects.plugins.CodeGeneratorContext;
import io.ballerina.stdlib.websub.task.WebSubServiceInfoGeneratorTask;
import io.ballerina.stdlib.websub.task.service.path.ServiceInfoResourceGeneratorTask;

/**
 * {@code WebSubServiceInfoGenerator} service-info generation for `websub:SubscriberService`.
 */
public class WebSubServiceInfoGenerator extends CodeGenerator {
    @Override
    public void init(CodeGeneratorContext codeGeneratorContext) {
        codeGeneratorContext.addSyntaxNodeAnalysisTask(new WebSubServiceInfoGeneratorTask(),
                SyntaxKind.SERVICE_DECLARATION);
        codeGeneratorContext.addSourceGeneratorTask(new ServiceInfoResourceGeneratorTask());
    }
}

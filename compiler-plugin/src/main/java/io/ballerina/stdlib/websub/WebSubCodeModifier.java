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
import io.ballerina.projects.plugins.CodeModifier;
import io.ballerina.projects.plugins.CodeModifierContext;
import io.ballerina.stdlib.websub.task.ServiceMetaInfoUpdaterTask;
import io.ballerina.stdlib.websub.task.WebSubServiceInfoGeneratorTask;

/**
 * {@code WebSubCodeModifier} handles required code-modification for WebSub Services.
 */
public class WebSubCodeModifier extends CodeModifier {
    @Override
    public void init(CodeModifierContext codeModifierContext) {
        codeModifierContext.addSyntaxNodeAnalysisTask(
                new WebSubServiceInfoGeneratorTask(), SyntaxKind.SERVICE_DECLARATION);
        codeModifierContext.addSourceModifierTask(new ServiceMetaInfoUpdaterTask());
    }
}

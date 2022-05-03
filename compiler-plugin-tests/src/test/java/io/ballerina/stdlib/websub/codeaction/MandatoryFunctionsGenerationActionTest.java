/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.websub.codeaction;

import io.ballerina.projects.plugins.codeaction.CodeActionArgument;
import io.ballerina.projects.plugins.codeaction.CodeActionInfo;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.testng.annotations.DataProvider;

import java.util.List;

import static io.ballerina.stdlib.websub.Constants.ADD_MANDATORY_FUNCTION_ACTION;
import static io.ballerina.stdlib.websub.Constants.NODE_LOCATION;

/**
 * {@code MandatoryFunctionsGenerationActionTest} contains the test cases related to mandatory function generation for
 * service declaration.
 */
public class MandatoryFunctionsGenerationActionTest extends AbstractCodeActionTest {
    @DataProvider
    protected Object[][] testDataProvider() {
        return new Object[][]{
                {"service.bal", 19, 8, "result.bal"}
        };
    }

    @Override
    protected CodeActionInfo getExpectedCodeAction() {
        LineRange lineRange = LineRange.from("service.bal", LinePosition.from(18, 0),
                LinePosition.from(19, 44));
        CodeActionArgument locationArg = CodeActionArgument.from(NODE_LOCATION, lineRange);
        CodeActionInfo codeAction = CodeActionInfo.from(ADD_MANDATORY_FUNCTION_ACTION, List.of(locationArg));
        codeAction.setProviderName("WEBSUB_202/ballerina/websub/ADD_MANDATORY_FUNCTION_CODE_SNIPPET");
        return codeAction;
    }

    @Override
    protected String getTestPackage() {
        return "sample_code_action_1";
    }

    @Override
    protected String getConfigDir() {
        return "mandatory_function_generation";
    }
}

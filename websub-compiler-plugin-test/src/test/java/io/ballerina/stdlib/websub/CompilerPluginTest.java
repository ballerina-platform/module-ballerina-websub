/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.websub;

import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.environment.Environment;
import io.ballerina.projects.environment.EnvironmentBuilder;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

/**
 * This class includes tests for Ballerina Graphql compiler plugin.
 */
public class CompilerPluginTest {
    private static final Path RESOURCE_DIRECTORY = Paths
            .get("src", "test", "resources", "ballerina_sources").toAbsolutePath();
    private static final PrintStream OUT = System.out;
    private static final Path DISTRIBUTION_PATH = Paths
            .get("build", "target", "ballerina-distribution").toAbsolutePath();

    @Test
    public void testCompilerPluginForRemoteMethodValidation() {
        Package currentPackage = loadPackage("sample_2");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.diagnostics().toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), "WEBSUB_102");
        String expectedMessage = "websub:SubscriberService should only implement remote methods";
        Assert.assertEquals(diagnostic.message(), expectedMessage);
    }

    @Test
    public void testCompilerPluginForRequiredMethodValidation() {
        Package currentPackage = loadPackage("sample_3");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.diagnostics().toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), "WEBSUB_103");
        String expectedMessage = "websub:SubscriberService should implement onEventNotification method";
        Assert.assertEquals(diagnostic.message(), expectedMessage);
    }

    @Test
    public void testCompilerPluginForNotAllowedMethods() {
        Package currentPackage = loadPackage("sample_4");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.diagnostics().toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), "WEBSUB_104");
        String expectedMessage = "onNewEvent method is not allowed in websub:SubscriberService declaration";
        Assert.assertEquals(diagnostic.message(), expectedMessage);
    }

    @Test
    public void testCompilerPluginForInvalidParameterTypesWithUnions() {
        Package currentPackage = loadPackage("sample_5");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        diagnosticResult.diagnostics().forEach(e -> System.out.println(e));
//        Assert.assertEquals(diagnosticResult.diagnostics().size(), 1);
//        Diagnostic diagnostic = (Diagnostic) diagnosticResult.diagnostics().toArray()[0];
//        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
//        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
//        Assert.assertEquals(diagnosticInfo.code(), "WEBSUB_105");
//        String expectedMsg = MessageFormat.format("{0} type parameters not allowed for {1} method",
//                "websub:ContentDistributionMessage|SecondaryMsgType", "onEventNotification");
//        Assert.assertEquals(diagnostic.message(), expectedMsg);
    }

    @Test
    public void testCompilerPluginForInvalidParameterTypes() {
        Package currentPackage = loadPackage("sample_6");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.diagnostics().toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), "WEBSUB_105");
        String expectedMsg = MessageFormat.format("{0} type parameters not allowed for {1} method",
                "sample_6:SimpleObj", "onEventNotification");
        Assert.assertEquals(diagnostic.message(), expectedMsg);
    }

    @Test
    public void testCompilerPluginForNoParameterAvailable() {
        Package currentPackage = loadPackage("sample_7");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.diagnostics().toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), "WEBSUB_106");
        String expectedMsg = MessageFormat.format("{0} method should have parameters of following {1} types",
                "onEventNotification", "websub:ContentDistributionMessage");
        Assert.assertEquals(diagnostic.message(), expectedMsg);
    }

    @Test
    public void testCompilerPluginForInvalidReturnTypes() {
        Package currentPackage = loadPackage("sample_8");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.diagnostics().toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), "WEBSUB_107");
        String expectedMsg = MessageFormat.format("{0} type is not allowed to be returned from {1} method",
                "websub:Acknowledgement|websub:SubscriptionDeletedError|SpecialReturnType?",
                "onEventNotification");
        Assert.assertEquals(diagnostic.message(), expectedMsg);
    }

    @Test
    public void testCompilerPluginForNoReturnType() {
        Package currentPackage = loadPackage("sample_9");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.diagnostics().toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), "WEBSUB_108");
        String expectedMsg = MessageFormat.format("{0} method should return {1} types",
                "onSubscriptionVerification",
                "websub:SubscriptionVerificationSuccess|websub:SubscriptionVerificationError");
        Assert.assertEquals(diagnostic.message(), expectedMsg);
    }

    @Test
    public void testCompilerPluginForInvalidAnnotation() {
        Package currentPackage = loadPackage("sample_10");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.diagnostics().toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), "WEBSUB_101");
        String expectedMsg = "Subscriber service should be annotated with websub:SubscriberServiceConfig";
        Assert.assertEquals(diagnostic.message(), expectedMsg);
    }

    @Test
    public void testCompilerPluginForNoAnnotation() {
        Package currentPackage = loadPackage("sample_11");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.diagnostics().toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), "WEBSUB_101");
        String expectedMsg = "Subscriber service should be annotated with websub:SubscriberServiceConfig";
        Assert.assertEquals(diagnostic.message(), expectedMsg);
    }

    @Test
    public void testCompilerPluginForListenerImplicitInit() {
        Package currentPackage = loadPackage("sample_12");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.diagnostics().toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), "WEBSUB_109");
        String expectedMsg = "websub:Listener should only take either http:Listener or websub:ListenerConfiguration";
        Assert.assertEquals(diagnostic.message(), expectedMsg);
    }

    @Test
    public void testCompilerPluginForListenerExplicitInit() {
        Package currentPackage = loadPackage("sample_13");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.diagnostics().toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), "WEBSUB_109");
        String expectedMsg = "websub:Listener should only take either http:Listener or websub:ListenerConfiguration";
        Assert.assertEquals(diagnostic.message(), expectedMsg);
    }

    private Package loadPackage(String path) {
        Path projectDirPath = RESOURCE_DIRECTORY.resolve(path);
        BuildProject project = BuildProject.load(getEnvironmentBuilder(), projectDirPath);
        return project.currentPackage();
    }

    private static ProjectEnvironmentBuilder getEnvironmentBuilder() {
        Environment environment = EnvironmentBuilder.getBuilder().setBallerinaHome(DISTRIBUTION_PATH).build();
        return ProjectEnvironmentBuilder.getBuilder(environment);
    }
}

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
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class includes tests for Ballerina WebSub compiler plugin.
 */
public class CompilerPluginTest {
    private static final Path RESOURCE_DIRECTORY = Paths
            .get("src", "test", "resources", "ballerina_sources").toAbsolutePath();
    private static final PrintStream OUT = System.out;
    private static final Path DISTRIBUTION_PATH = Paths
            .get("../", "target", "ballerina-runtime").toAbsolutePath();

    @Test
    public void testValidServiceDeclaration() {
        Package currentPackage = loadPackage("sample_1");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 0);
    }

    @Test
    public void testCompilerPluginForRemoteMethodValidation() {
        Package currentPackage = loadPackage("sample_2");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 1);
        Diagnostic diagnostic = (Diagnostic) errorDiagnostics.toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        WebSubDiagnosticCodes expectedCode = WebSubDiagnosticCodes.WEBSUB_102;
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        String expectedMsg = MessageFormat.format(expectedCode.getDescription(), "onEventNotification");
        Assert.assertEquals(diagnosticInfo.code(), expectedCode.getCode());
        Assert.assertEquals(diagnostic.message(), expectedMsg);
    }

    @Test
    public void testCompilerPluginForRequiredMethodValidation() {
        Package currentPackage = loadPackage("sample_3");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 1);
        Diagnostic diagnostic = (Diagnostic) errorDiagnostics.toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        WebSubDiagnosticCodes expectedCode = WebSubDiagnosticCodes.WEBSUB_103;
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), expectedCode.getCode());
        Assert.assertEquals(diagnostic.message(), expectedCode.getDescription());
    }

    @Test
    public void testCompilerPluginForNotAllowedMethods() {
        Package currentPackage = loadPackage("sample_4");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 1);
        Diagnostic diagnostic = (Diagnostic) errorDiagnostics.toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        WebSubDiagnosticCodes expectedCode = WebSubDiagnosticCodes.WEBSUB_104;
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), expectedCode.getCode());
        String expectedMessage = MessageFormat.format(expectedCode.getDescription(), "onNewEvent");
        Assert.assertEquals(diagnostic.message(), expectedMessage);
    }

    @Test
    public void testCompilerPluginForInvalidParameterTypesWithUnions() {
        Package currentPackage = loadPackage("sample_5");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 1);
        Diagnostic diagnostic = (Diagnostic) errorDiagnostics.toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        WebSubDiagnosticCodes expectedCode = WebSubDiagnosticCodes.WEBSUB_105;
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), expectedCode.getCode());
        String expectedMsg = MessageFormat.format(expectedCode.getDescription(),
                "websub:ContentDistributionMessage|sample_5:SecondaryMsgType", "onEventNotification");
        Assert.assertEquals(diagnostic.message(), expectedMsg);
    }

    @Test
    public void testCompilerPluginForInvalidParameterTypes() {
        Package currentPackage = loadPackage("sample_6");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 1);
        Diagnostic diagnostic = (Diagnostic) errorDiagnostics.toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        WebSubDiagnosticCodes expectedCode = WebSubDiagnosticCodes.WEBSUB_105;
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), expectedCode.getCode());
        String expectedMsg = MessageFormat.format(expectedCode.getDescription(),
                "sample_6:SimpleObj", "onEventNotification");
        Assert.assertEquals(diagnostic.message(), expectedMsg);
    }

    @Test
    public void testCompilerPluginForNoParameterAvailable() {
        Package currentPackage = loadPackage("sample_7");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 1);
        Diagnostic diagnostic = (Diagnostic) errorDiagnostics.toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        WebSubDiagnosticCodes expectedCode = WebSubDiagnosticCodes.WEBSUB_106;
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), expectedCode.getCode());
        String expectedMsg = MessageFormat.format(expectedCode.getDescription(),
                "onEventNotification", "websub:ContentDistributionMessage");
        Assert.assertEquals(diagnostic.message(), expectedMsg);
    }

    @Test
    public void testCompilerPluginForInvalidReturnTypes() {
        Package currentPackage = loadPackage("sample_8");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 1);
        Diagnostic diagnostic = (Diagnostic) errorDiagnostics.toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        WebSubDiagnosticCodes expectedCode = WebSubDiagnosticCodes.WEBSUB_107;
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), expectedCode.getCode());
        String expectedMsg = MessageFormat.format(expectedCode.getDescription(),
                "websub:Acknowledgement|websub:SubscriptionDeletedError|sample_8:SpecialReturnType?",
                "onEventNotification");
        Assert.assertEquals(diagnostic.message(), expectedMsg);
    }

    @Test
    public void testCompilerPluginForNoReturnType() {
        Package currentPackage = loadPackage("sample_9");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 1);
        Diagnostic diagnostic = (Diagnostic) errorDiagnostics.toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        WebSubDiagnosticCodes expectedCode = WebSubDiagnosticCodes.WEBSUB_108;
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), expectedCode.getCode());
        String expectedMsg = MessageFormat.format(expectedCode.getDescription(),
                "onSubscriptionVerification",
                "websub:SubscriptionVerificationSuccess|websub:SubscriptionVerificationError");
        Assert.assertEquals(diagnostic.message(), expectedMsg);
    }

    @Test
    public void testCompilerPluginForInvalidAnnotation() {
        Package currentPackage = loadPackage("sample_10");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 1);
        Diagnostic diagnostic = (Diagnostic) errorDiagnostics.toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        WebSubDiagnosticCodes expectedCode = WebSubDiagnosticCodes.WEBSUB_101;
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), expectedCode.getCode());
        Assert.assertEquals(diagnostic.message(), expectedCode.getDescription());
    }

    @Test
    public void testCompilerPluginForNoAnnotation() {
        Package currentPackage = loadPackage("sample_11");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 1);
        Diagnostic diagnostic = (Diagnostic) errorDiagnostics.toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        WebSubDiagnosticCodes expectedCode = WebSubDiagnosticCodes.WEBSUB_101;
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), expectedCode.getCode());
        Assert.assertEquals(diagnostic.message(), expectedCode.getDescription());
    }

    @Test
    public void testCompilerPluginForListenerImplicitInit() {
        Package currentPackage = loadPackage("sample_12");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 1);
        Diagnostic diagnostic = (Diagnostic) errorDiagnostics.toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        WebSubDiagnosticCodes expectedCode = WebSubDiagnosticCodes.WEBSUB_109;
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), expectedCode.getCode());
        Assert.assertEquals(diagnostic.message(), expectedCode.getDescription());
    }

    @Test
    public void testCompilerPluginForListenerExplicitInit() {
        Package currentPackage = loadPackage("sample_13");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 1);
        Diagnostic diagnostic = (Diagnostic) errorDiagnostics.toArray()[0];
        DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
        WebSubDiagnosticCodes expectedCode = WebSubDiagnosticCodes.WEBSUB_109;
        Assert.assertNotNull(diagnosticInfo, "DiagnosticInfo is null for erroneous service definition");
        Assert.assertEquals(diagnosticInfo.code(), expectedCode.getCode());
        Assert.assertEquals(diagnostic.message(), expectedCode.getDescription());
    }

    @Test
    public void testValidServiceDeclarationWithAliases() {
        Package currentPackage = loadPackage("sample_14");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 0);
    }

    @Test
    public void testValidServiceDeclarationWithIncludedRecordParams() {
        Package currentPackage = loadPackage("sample_15");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 0);
    }

    @Test
    public void testAttachingMultipleServicesToOneListener() {
        Package currentPackage = loadPackage("sample_16");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 0);
    }

    @Test
    public void testErrorReturnTypesWithEventNotification() {
        Package currentPackage = loadPackage("sample_17");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 0);
    }

    @Test
    public void testErrorReturnTypesWithAllMethods() {
        Package currentPackage = loadPackage("sample_18");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 0);
    }
    
    public void testValidWebsubServiceClassDeclaration() {
        Package currentPackage = loadPackage("sample_19");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 0);
    }

    @Test
    public void testValidWebsubServiceClassDeclarationWithAdditionalMethods() {
        Package currentPackage = loadPackage("sample_20");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 0);
    }

    @Test
    public void testValidWebsubServiceClassDeclarationWithAdditionalExternFunctions() {
        Package currentPackage = loadPackage("sample_21");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 0);
    }

    @Test
    public void testValidWebsubServiceDeclarationWithUnsubVerification() {
        Package currentPackage = loadPackage("sample_22");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 0);
    }

    @Test
    public void testValidWebsubServiceDeclarationWithIntersectionTypes() {
        Package currentPackage = loadPackage("sample_23");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        List<Diagnostic> errorDiagnostics = diagnosticResult.diagnostics().stream()
                .filter(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()))
                .collect(Collectors.toList());
        Assert.assertEquals(errorDiagnostics.size(), 0);
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

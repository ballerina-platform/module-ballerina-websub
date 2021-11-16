/*
 * Copyright (c) 2021, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ballerina.stdlib.websub.task.service.path;

import io.ballerina.stdlib.websub.Constants;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static io.ballerina.stdlib.websub.task.AnalyserUtils.copyContent;

/**
 * {@code ResourcePackagingService} updates generated ballerina executable jar.
 */
public class ResourcePackagingService {
    private static final String TARGET_FILE_NAME = "target_exec.jar";
    private static final String SERVICE_INFO_FILE = "service-info.csv";
    private static final String SERVICE_INFO_ENTRY_FORMAT = "%d, %s";

    public void updateJarFile(Path targetBinPath, String srcFileName, ServicePathContext context)
            throws IOException, ServicePathGeneratorException {
        Path srcFile = targetBinPath.resolve(srcFileName);
        Path targetFile = targetBinPath.resolve(TARGET_FILE_NAME);
        generateUpdatedJar(srcFile, targetFile, context);
        boolean successful = deleteOldFile(srcFile);
        if (successful) {
            renameGeneratedFile(targetFile, srcFile);
        }
    }

    private void generateUpdatedJar(Path srcFile, Path targetFile, ServicePathContext context) throws IOException {
        try (JarFile srcJar = new JarFile(srcFile.toString())) {
            try (ZipOutputStream outStream = new ZipOutputStream(new BufferedOutputStream(
                    new FileOutputStream(targetFile.toString())))) {
                // add generated open-api doc related resources to target jar
                addServicePathResource(outStream, context);

                // copy all the content in the src-jar to the new jar
                for (Enumeration<JarEntry> entries = srcJar.entries(); entries.hasMoreElements();) {
                    JarEntry element = entries.nextElement();
                    outStream.putNextEntry(element);
                    try (InputStream inputStream = srcJar.getInputStream(element)) {
                        copyContent(inputStream, outStream);
                        outStream.closeEntry();
                    }
                }
            }
        }
    }

    private void addServicePathResource(ZipOutputStream outStream, ServicePathContext context) throws IOException {
        Path resourcesDirectory = Paths.get(
                Constants.RESOURCES_DIR_NAME, Constants.PACKAGE_ORG, Constants.PACKAGE_NAME);
        Path targetFile = resourcesDirectory.resolve(SERVICE_INFO_FILE);
        outStream.putNextEntry(new ZipEntry(targetFile.toString()));
        for (ServicePathContext.ServicePathInformation servicePathInfo: context.getServicePathDetails()) {
            String entry = String.format(SERVICE_INFO_ENTRY_FORMAT,
                    servicePathInfo.getServiceId(), servicePathInfo.getServicePath());
            try (InputStream inputStream = new ByteArrayInputStream(entry.getBytes(StandardCharsets.UTF_8))) {
                copyContent(inputStream, outStream);
            }
        }
        outStream.closeEntry();
    }

    private boolean deleteOldFile(Path sourceFilePath) {
        // delete old jar file
        File sourceFile = sourceFilePath.toFile();
        if (sourceFile.exists()) {
            return sourceFile.delete();
        }
        return true;
    }

    private void renameGeneratedFile(Path targetFilePath, Path sourceFilePath) throws ServicePathGeneratorException {
        File targetFile = targetFilePath.toFile();
        boolean success = targetFile.renameTo(sourceFilePath.toFile());
        if (!success) {
            throw new ServicePathGeneratorException("could not rename the generated executable jar");
        }
    }
}

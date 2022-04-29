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

import io.ballerina.projects.DocumentId;
import io.ballerina.projects.ModuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * {@code ServicePathContextHandler} will manage the shared context among compiler plugin tasks.
 */
public final class ServicePathContextHandler {
    private static ServicePathContextHandler contextHandlerInstance;

    private final List<ServicePathContext> contexts;

    private ServicePathContextHandler() {
        this.contexts = new ArrayList<>();
    }

    public static ServicePathContextHandler getContextHandler() {
        synchronized (ServicePathContextHandler.class) {
            if (Objects.isNull(contextHandlerInstance)) {
                contextHandlerInstance = new ServicePathContextHandler();
            }
        }
        return contextHandlerInstance;
    }

    private void addContext(ServicePathContext context) {
        synchronized (this.contexts) {
            this.contexts.add(context);
        }
    }

    public void updateServicePathContext(ModuleId moduleId, DocumentId documentId,
                                         ServicePathContext.ServicePathInformation servicePathInformation) {
        Optional<ServicePathContext> contextOpt = retrieveContext(moduleId, documentId);
        if (contextOpt.isPresent()) {
            ServicePathContext context = contextOpt.get();
            synchronized (context) {
                context.updateServicePathDetails(servicePathInformation);
            }
            return;
        }
        ServicePathContext context = new ServicePathContext(moduleId, documentId);
        context.updateServicePathDetails(servicePathInformation);
        addContext(context);
    }

    public Optional<ServicePathContext> retrieveContext(ModuleId moduleId, DocumentId documentId) {
        return this.contexts.stream()
                .filter(ctx -> equals(ctx, moduleId, documentId))
                .findFirst();
    }

    private boolean equals(ServicePathContext context, ModuleId moduleId, DocumentId documentId) {
        int hashCodeForCurrentContext = Objects.hash(context.getModuleId(), context.getDocumentId());
        return hashCodeForCurrentContext == Objects.hash(moduleId, documentId);
    }
}

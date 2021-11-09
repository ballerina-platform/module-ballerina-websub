/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.websub;

import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.stdlib.websub.Constants.SERVICE_INFO_REGISTRY;
import static io.ballerina.stdlib.websub.Constants.SERVICE_PATH;
import static io.ballerina.stdlib.websub.Constants.SERVICE_REGISTRY;
import static io.ballerina.stdlib.websub.Constants.SUBSCRIBER_CONFIG;

/**
 * {@code NativeWebSubListenerAdaptor} is a wrapper object used to save/retrieve native data related to WebSub Listener.
 */
public class NativeWebSubListenerAdaptor {
    private static final String SERVICE_INFO_RESOURCE = "service-info.csv";

    public static Object externInit(BObject websubListener) {
        try {
            Map<String, String> serviceInfoRegistry = ServiceInfoRetriever.retrieve(SERVICE_INFO_RESOURCE);
            websubListener.addNativeData(SERVICE_INFO_REGISTRY, serviceInfoRegistry);
        } catch (IOException ex) {
            Module module = ModuleUtils.getModule();
            BString errorMessage = StringUtils.fromString(
                    String.format("Error retrieving Service Information: %s", ex.getMessage()));
            throw ErrorCreator.createError(module, "Error", errorMessage, null, null);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Object retrieveGeneratedServicePath(BObject websubListener, BObject subscriberService) {
        Object serviceInfoRegistryObj = websubListener.getNativeData(SERVICE_INFO_REGISTRY);
        if (Objects.isNull(serviceInfoRegistryObj)) {
            Module module = ModuleUtils.getModule();
            BString errorMessage = StringUtils
                    .fromString("Error retrieving Service Information: service path not found");
            throw ErrorCreator.createError(module, "Error", errorMessage, null, null);
        }

        Map<String, String> serviceInfoRegistry = (Map<String, String>) serviceInfoRegistryObj;
        Optional<String> serviceId = getServiceId(subscriberService);
        if (serviceId.isEmpty()) {
            Module module = ModuleUtils.getModule();
            BString errorMessage = StringUtils
                    .fromString("Error retrieving Service Information: service path not found");
            throw ErrorCreator.createError(module, "Error", errorMessage, null, null);
        }

        String generatedServicePath = serviceInfoRegistry.get(serviceId.get());
        if (Objects.isNull(generatedServicePath)) {
            Module module = ModuleUtils.getModule();
            BString errorMessage = StringUtils
                    .fromString("Error retrieving Service Information: service path not found");
            throw ErrorCreator.createError(module, "Error", errorMessage, null, null);
        }

        return StringUtils.fromString(generatedServicePath);
    }

    private static Optional<String> getServiceId(BObject subscriberService) {
        ObjectType objType = subscriberService.getType();
        if (Objects.nonNull(objType)) {
            return objType.getAnnotations().entrySet().stream()
                    .filter(e -> e.getKey().toString().contains(Constants.ANN_NAME_HTTP_INTROSPECTION_DOC_CONFIG))
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .filter(e -> e instanceof BMap)
                    .map(e -> ((BMap) e).getStringValue(Constants.ANN_FIELD_DOC_NAME).getValue().trim());
        }
        return Optional.empty();
    }

    public static void externAttach(BObject websubListener, BString servicePath,
                                    BObject subscriberService, BObject httpService,
                                    BMap<BString, Object> subscriberConfig) {
        // add service-path to subscriber-obj as native data
        // this information is useful to execute subscriber-service detach from websub-listener
        subscriberService.addNativeData(SERVICE_PATH, servicePath);

        // add subscriber-config to native-data. this information is useful when initiating subscription/unsubscription
        httpService.addNativeData(SUBSCRIBER_CONFIG, subscriberConfig);

        // add http-service into listener service-registry
        ServiceRegistry serviceRegistry = getServiceRegistry(websubListener);
        serviceRegistry.addHttpService(servicePath, httpService);
    }

    private static ServiceRegistry getServiceRegistry(BObject websubListener) {
        Object serviceRegistry = websubListener.getNativeData(SERVICE_REGISTRY);
        if (Objects.nonNull(serviceRegistry)) {
            return (ServiceRegistry) serviceRegistry;
        }
        // if the service-registry not available, create an instance and add it to the listener native-data
        ServiceRegistry serviceRegistryInstance = ServiceRegistry.getInstance();
        websubListener.addNativeData(SERVICE_REGISTRY, serviceRegistryInstance);
        return serviceRegistryInstance;
    }

    public static BObject detachHttpService(BObject websubListener, BObject subscriberService) {
        ServiceRegistry serviceRegistry = (ServiceRegistry) websubListener.getNativeData(SERVICE_REGISTRY);
        return serviceRegistry.detachHttpService(subscriberService);
    }

    public static BArray retrieveAttachedServices(BObject websubListener) {
        Object serviceRegistryObj = websubListener.getNativeData(SERVICE_REGISTRY);
        if (Objects.nonNull(serviceRegistryObj)) {
            ServiceRegistry serviceRegistry = (ServiceRegistry) serviceRegistryObj;
            BObject[] attachedServices = serviceRegistry.getAttachedServices();
            if (attachedServices.length > 0) {
                ArrayType arrType = TypeCreator.createArrayType(attachedServices[0].getType());
                return ValueCreator.createArrayValue(attachedServices, arrType);
            }
            return null;
        }
        return null;
    }
}

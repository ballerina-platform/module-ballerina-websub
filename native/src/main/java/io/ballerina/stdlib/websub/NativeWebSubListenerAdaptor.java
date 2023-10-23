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

import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.util.Objects;

import static io.ballerina.stdlib.websub.Constants.SERVICE_PATH;
import static io.ballerina.stdlib.websub.Constants.SERVICE_REGISTRY;
import static io.ballerina.stdlib.websub.Constants.SUBSCRIBER_CONFIG;

/**
 * {@code NativeWebSubListenerAdaptor} is a wrapper object used to save/retrieve native data related to WebSub Listener.
 */
public final class NativeWebSubListenerAdaptor {
    private NativeWebSubListenerAdaptor() {}

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
                ArrayType arrType = TypeCreator.createArrayType(TypeUtils.getType(attachedServices[0]));
                return ValueCreator.createArrayValue(attachedServices, arrType);
            }
            return null;
        }
        return null;
    }
}

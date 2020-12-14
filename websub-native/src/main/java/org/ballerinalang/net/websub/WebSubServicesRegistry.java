/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.net.websub;

import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.net.http.HTTPServicesRegistry;
import org.ballerinalang.net.http.HttpResource;
import org.ballerinalang.net.http.HttpService;
import org.ballerinalang.net.http.websocket.server.WebSocketServicesRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.ballerinalang.net.websub.WebSubSubscriberConstants.BALLERINA;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.RESOURCE_NAME_ON_INTENT_VERIFICATION;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.WEBSUB;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.WEBSUB_INTENT_VERIFICATION_REQUEST;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.WEBSUB_NOTIFICATION_REQUEST;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.WEBSUB_PACKAGE;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.WEBSUB_SERVICE_CALLER;

/**
 * The WebSub service registry which uses an {@link HTTPServicesRegistry} to maintain WebSub Subscriber HTTP services.
 *
 * @since 0.965.0
 */
public class WebSubServicesRegistry extends HTTPServicesRegistry {

    private static final Logger logger = LoggerFactory.getLogger(WebSubServicesRegistry.class);
    private static final String message = "Invalid parameter type '%s' in resource '%s'. Requires '%s:%s'";

    private String topicIdentifier;
    private String topicHeader;

    private BMap<BString, Object> headerResourceMap;
    private BMap<BString, BMap<BString, Object>> payloadKeyResourceMap;
    private BMap<BString, BMap<BString, BMap<BString, Object>>> headerAndPayloadKeyResourceMap;
    private HashMap<String, RecordType> resourceDetails;

    private static final int CUSTOM_RESOURCE_PARAM_COUNT = 2;

    public WebSubServicesRegistry(WebSocketServicesRegistry webSocketServicesRegistry) {
        super(webSocketServicesRegistry);
    }

    public WebSubServicesRegistry(WebSocketServicesRegistry webSocketServicesRegistry,
                                  String topicIdentifier, String topicHeader,
                                  BMap<BString, Object> headerResourceMap,
                                  BMap<BString, BMap<BString, Object>> payloadKeyResourceMap,
                                  BMap<BString, BMap<BString, BMap<BString, Object>>>
                                          headerAndPayloadKeyResourceMap,
                                  HashMap<String, RecordType> resourceDetails) {
        super(webSocketServicesRegistry);
        this.topicIdentifier = topicIdentifier;
        this.topicHeader = topicHeader;
        this.headerResourceMap = headerResourceMap;
        this.payloadKeyResourceMap = payloadKeyResourceMap;
        this.headerAndPayloadKeyResourceMap = headerAndPayloadKeyResourceMap;
        this.resourceDetails = resourceDetails;
    }

    /**
     * Method to retrieve the identifier for topics based on which dispatching would happen for custom subscriber
     * services.
     *
     * @return the identifier for topics
     */
    String getTopicIdentifier() {
        return topicIdentifier;
    }

    /**
     * Method to retrieve the topic header upon which routing would be based, if specified.
     *
     * @return the topic header to consider
     */
    String getTopicHeader() {
        return topicHeader;
    }

    BMap<BString, Object> getHeaderResourceMap() {
        return headerResourceMap;
    }

    BMap<BString, BMap<BString, Object>> getPayloadKeyResourceMap() {
        return payloadKeyResourceMap;
    }

    /**
     * Method to retrieve the topic resource mapping if specified.
     *
     * @return the topic-resource map specified for the service
     */
    BMap<BString, BMap<BString, BMap<BString, Object>>> getHeaderAndPayloadKeyResourceMap() {
        return headerAndPayloadKeyResourceMap;
    }

    HashMap<String, RecordType> getResourceDetails() {
        return resourceDetails;
    }

    /**
     * Register a WebSubSubscriber service in the map.
     *
     * @param service to be registered
     */
    public void registerWebSubSubscriberService(BObject service, String basePath) {
        HttpService httpService = WebSubHttpService.buildWebSubSubscriberHttpService(service, basePath);
        String hostName = httpService.getHostName();
        if (servicesMapByHost.get(hostName) == null) {
            servicesByBasePath = new ConcurrentHashMap<>();
            sortedServiceURIs = new CopyOnWriteArrayList<>();
            servicesMapByHost.put(hostName, new ServicesMapHolder(servicesByBasePath, sortedServiceURIs));
        } else {
            servicesByBasePath = getServicesByHost(hostName);
            sortedServiceURIs = getSortedServiceURIsByHost(hostName);
        }
        servicesByBasePath.put(httpService.getBasePath(), httpService);
        logger.info("Service deployed : " + service.getType().getName() + " with context " + httpService.getBasePath());

        //basePath will get cached after registering service
        sortedServiceURIs.add(httpService.getBasePath());
        sortedServiceURIs.sort((basePath1, basePath2) -> basePath2.length() - basePath1.length());

        if (topicIdentifier != null) {
            // i.e., extension config exists
            validateCustomResources(httpService.getResources(), this);
        }
    }

    private static void validateCustomResources(List<HttpResource> resources, WebSubServicesRegistry serviceRegistry) {
        List<String> invalidResourceNames = retrieveInvalidResourceNames(resources,
                                                                         serviceRegistry.getResourceDetails());

        if (!invalidResourceNames.isEmpty()) {
            throw ErrorCreator.createError(StringUtils.fromString("Resource name(s) not included in " +
                    "the topic-resource mapping found: " + invalidResourceNames));
        }
    }

    private static List<String> retrieveInvalidResourceNames(List<HttpResource> resources,
                                                             HashMap<String, RecordType> resourceDetails) {
        Set<String> resourceNames = resourceDetails.keySet();
        List<String> invalidResourceNames = new ArrayList<>();

        for (HttpResource resource : resources) {
            String resourceName = resource.getName();

            if (RESOURCE_NAME_ON_INTENT_VERIFICATION.equals(resourceName)) {
                validateOnIntentVerificationResource(resource);
            } else if (!resourceNames.contains(resourceName)) {
                invalidResourceNames.add(resourceName);
            } else {
                validateCustomNotificationResource(resource, resourceDetails.get(resourceName));
            }
        }
        return invalidResourceNames;
    }

    // Runtime Validation for Specific Subscriber Services.

    private static void validateOnIntentVerificationResource(HttpResource resource) {
        List<Type> paramTypes = resource.getParamTypes();
        validateParamCount(paramTypes, 2, resource.getName());
        validateCallerParam(paramTypes.get(0));
        validateIntentVerificationParam(paramTypes.get(1));
    }

    private static void validateCustomNotificationResource(HttpResource resource, RecordType recordType) {
        List<Type> paramTypes = resource.getParamTypes();
        validateParamCount(paramTypes, CUSTOM_RESOURCE_PARAM_COUNT, resource.getName());
        validateNotificationParam(resource.getName(), paramTypes.get(0));
        validateRecordType(resource.getName(), paramTypes.get(1), recordType);
    }

    private static void validateParamCount(List<Type> paramTypes, int expectedCount, String resourceName) {
        int paramCount = paramTypes.size();
        if (paramCount < expectedCount) {
            throw ErrorCreator.createError(StringUtils.fromString(String.format(
                    "Invalid param count for WebSub Resource '%s': expected '%d', found '%d'",
                                                            resourceName, expectedCount, paramCount)));
        }
    }

    private static void validateCallerParam(Type paramVarType) {
        if (!isExpectedObjectParam(paramVarType, WEBSUB_SERVICE_CALLER)) {
            throw ErrorCreator.createError(StringUtils.fromString(String.format(message,
                    paramVarType.getQualifiedName(), RESOURCE_NAME_ON_INTENT_VERIFICATION, WEBSUB_PACKAGE,
                    WEBSUB_SERVICE_CALLER)));
        }
    }

    private static void validateIntentVerificationParam(Type paramVarType) {
        if (!isExpectedObjectParam(paramVarType, WEBSUB_INTENT_VERIFICATION_REQUEST)) {
            throw ErrorCreator.createError(StringUtils.fromString(String.format(message,
                    paramVarType.getQualifiedName(), RESOURCE_NAME_ON_INTENT_VERIFICATION,
                    WEBSUB_PACKAGE, WEBSUB_INTENT_VERIFICATION_REQUEST)));
        }
    }

    private static void validateNotificationParam(String resourceName, Type paramVarType) {
        if (!isExpectedObjectParam(paramVarType, WEBSUB_NOTIFICATION_REQUEST)) {
            throw ErrorCreator.createError(StringUtils.fromString(String.format(message,
                    paramVarType.getQualifiedName(), resourceName, WEBSUB_PACKAGE, WEBSUB_NOTIFICATION_REQUEST)));
        }
    }

    private static void validateRecordType(String resourceName, Type paramVarType, RecordType recordType) {
        if (!TypeUtils.isSameType(paramVarType, recordType)) {
            throw ErrorCreator.createError(StringUtils.fromString(String.format("Invalid parameter type '%s' in " +
                            "resource '%s'. Requires '%s'", paramVarType.getQualifiedName(), resourceName,
                            recordType.getQualifiedName())));
        }
    }

    private static boolean isExpectedObjectParam(Type specifiedType, String expectedWebSubRecordName) {
        if (specifiedType.getTag() != TypeTags.OBJECT_TYPE_TAG) {
            return false;
        }
        ObjectType objectType = (ObjectType) specifiedType;
        return objectType.getPackage().getOrg().equals(BALLERINA) &&
                objectType.getPackage().getName().equals(WEBSUB) &&
                objectType.getName().equals(expectedWebSubRecordName);
    }
}

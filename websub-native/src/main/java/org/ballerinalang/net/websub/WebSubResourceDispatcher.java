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

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import org.ballerinalang.net.http.HttpConstants;
import org.ballerinalang.net.http.HttpResource;
import org.ballerinalang.net.http.HttpService;
import org.ballerinalang.net.transport.contract.exceptions.ServerConnectorException;
import org.ballerinalang.net.transport.message.HttpCarbonMessage;
import org.ballerinalang.net.uri.URIUtil;

import java.io.UnsupportedEncodingException;
import java.util.Objects;

import static org.ballerinalang.net.http.HttpConstants.HTTP_METHOD_GET;
import static org.ballerinalang.net.http.HttpConstants.HTTP_METHOD_POST;
import static org.ballerinalang.net.transport.contract.Constants.HTTP_RESOURCE;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.ANNOTATED_TOPIC;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.ANN_NAME_WEBSUB_SUBSCRIBER_SERVICE_CONFIG;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.ANN_WEBSUB_ATTR_TARGET;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.DEFERRED_FOR_PAYLOAD_BASED_DISPATCHING;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.ENTITY_ACCESSED_REQUEST;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.RESOURCE_NAME_ON_INTENT_VERIFICATION;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.RESOURCE_NAME_ON_NOTIFICATION;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.RESOURCE_NAME_ON_SUBSCRIPTION_DENIED;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.TOPIC_ID_HEADER;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.TOPIC_ID_PAYLOAD_KEY;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.WEBSUB_PACKAGE_FULL_QUALIFIED_NAME;
import static org.ballerinalang.net.websub.WebSubUtils.getHttpRequest;
import static org.ballerinalang.net.websub.WebSubUtils.getJsonBody;

/**
 * Resource dispatcher specific for WebSub subscriber services.
 *
 * @since 0.965.0
 */
class WebSubResourceDispatcher {
    private static final String HUB_MODE_DENIED = "denied";
    private static final String HUB_MODE_SUBSCRIBE = "subscribe";
    private static final String HUB_MODE_UNSUBSCRIBE = "unsubscribe";

    static HttpResource findResource(HttpService service, 
                                     HttpCarbonMessage inboundRequest, 
                                     WebSubServicesRegistry servicesRegistry)
                                     throws BallerinaConnectorException, ServerConnectorException {

        String method = inboundRequest.getHttpMethod();

        String resourceName = getResourceName(method, inboundRequest, servicesRegistry);

        return getResource(resourceName, method, service, inboundRequest);
    }

    /**
     * Method to retrieve resource names for incomming HTTP Reuqest
     *
     * @param method            {@String} HTTP method for incomming request
     * @param inboundRequest    {@HttpCarbonMessage} inbound HTTP Request
     * @param serviceRegistry   {@WebSubServicesRegistry} WebSub service registry
     * @return                  {@String} specifying the resource name if found
     * @throws BallerinaConnectorException for any erroneous scenarios
     */
    private static String getResourceName(String requestMethod, 
                                          HttpCarbonMessage inboundRequest,
                                          WebSubServicesRegistry servicesRegistry) {
        String topicIdentifier = servicesRegistry.getTopicIdentifier();
        
        if (TOPIC_ID_HEADER.equals(topicIdentifier) && HTTP_METHOD_POST.equalsIgnoreCase(requestMethod)) {
            
            String topic = inboundRequest.getHeader(servicesRegistry.getTopicHeader());
        
            return retrieveResourceNameFromTopic(StringUtils.fromString(topic), servicesRegistry.getHeaderResourceMap());
        } else if (Objects.nonNull(topicIdentifier) && HTTP_METHOD_POST.equalsIgnoreCase(requestMethod)) {
            
            if (Objects.isNull(inboundRequest.getProperty(HTTP_RESOURCE))) {
                
                inboundRequest.setProperty(HTTP_RESOURCE, DEFERRED_FOR_PAYLOAD_BASED_DISPATCHING);
                
                return null;
            } else {
                if (topicIdentifier.equals(TOPIC_ID_PAYLOAD_KEY)) {
                
                    return retrieveResourceName(inboundRequest, servicesRegistry.getPayloadKeyResourceMap());
                } else {
                
                    return retrieveResourceName(inboundRequest, servicesRegistry);
                }
            }
        } else {
            return retrieveResourceName(requestMethod, inboundRequest);
        }
    }

    /**
     * Method to retrieve resource names from HTTP Method and hub.mode
     *
     * @param requestMethod    {@String} HTTP method of the incomming request
     * @return                 {@link WebSubSubscriberConstants#RESOURCE_NAME_ON_INTENT_VERIFICATION} if the method is GET,
     *                         {@link WebSubSubscriberConstants#RESOURCE_NAME_ON_NOTIFICATION} if the method is POST
     * @throws BallerinaConnectorException for any method other than GET or POST
     */
    private static String retrieveResourceName(String requestMethod, 
                                               HttpCarbonMessage inboundRequest) {
        if (HTTP_METHOD_POST.equalsIgnoreCase(requestMethod)) {
            return RESOURCE_NAME_ON_NOTIFICATION;
        } else if (HTTP_METHOD_GET.equalsIgnoreCase(requestMethod)) {
            String queryString = (String) inboundRequest.getProperty(HttpConstants.QUERY_STR);
            BMap<BString, Object> params = ValueCreator.createMapValue();
            
            String hubMode = "";
            
            try {
                URIUtil.populateQueryParamMap(queryString, params);
                hubMode = params.getArrayValue(StringUtils.fromString("hub.mode")).getBString(0).getValue();
            } catch (UnsupportedEncodingException e) {
                inboundRequest.setHttpStatusCode(404);
                throw new BallerinaConnectorException("Bad Request. No query params found");
            }

            if (HUB_MODE_DENIED.equalsIgnoreCase(hubMode)) {
                return RESOURCE_NAME_ON_SUBSCRIPTION_DENIED;
            } else if (HUB_MODE_SUBSCRIBE.equalsIgnoreCase(hubMode)) {
                return RESOURCE_NAME_ON_INTENT_VERIFICATION;
            } else {
                throw new BallerinaConnectorException(
                    String.format("HubMode[%s] is not allowed for WebSub Subscriber Services", hubMode));
            }
        } else {
            throw new BallerinaConnectorException(
                String.format("HTTP Method [%s] not allowed for WebSub Subscriber Services", requestMethod));
        }
    }

    /**
     * Method to retrieve the resource name when the mapping between topic and resource for custom subscriber services
     * is specified as a combination of a header and a key of the JSON payload.
     *
     * @param inboundRequest                 the request received
     * @param servicesRegistry               the service registry instance
     * @return                               the name of the resource as identified based on the topic
     * @throws BallerinaConnectorException   if a resource could not be mapped to the topic identified
     */
    private static String retrieveResourceName(HttpCarbonMessage inboundRequest,
                                               WebSubServicesRegistry servicesRegistry) {
        BMap<BString, BMap<BString, BMap<BString, Object>>> headerAndPayloadKeyResourceMap =
                servicesRegistry.getHeaderAndPayloadKeyResourceMap();
        BString topic = StringUtils.fromString(inboundRequest.getHeader(servicesRegistry.getTopicHeader()));
        BObject httpRequest = getHttpRequest(inboundRequest);
        BMap<BString, ?> jsonBody = getJsonBody(httpRequest);
        inboundRequest.setProperty(ENTITY_ACCESSED_REQUEST, httpRequest);

        if (headerAndPayloadKeyResourceMap.containsKey(topic)) {
            BMap<BString, BMap<BString, Object>> topicResourceMapForHeader =
                    headerAndPayloadKeyResourceMap.get(topic);
            for (BString key : topicResourceMapForHeader.getKeys()) {
                if (jsonBody.containsKey(key)) {
                    BMap<BString, Object> topicResourceMapForValue = topicResourceMapForHeader.get(key);
                    BString valueForKey = (BString) jsonBody.get(key);
                    if (topicResourceMapForValue.containsKey(valueForKey)) {
                        return retrieveResourceNameFromTopic(valueForKey, topicResourceMapForValue);
                    }
                }
            }
        }

        if (servicesRegistry.getHeaderResourceMap() != null) {
            BMap<BString, Object> headerResourceMap = servicesRegistry.getHeaderResourceMap();
            if (headerResourceMap.containsKey(topic)) {
                return retrieveResourceNameFromTopic(topic, headerResourceMap);
            }
        }

        if (servicesRegistry.getPayloadKeyResourceMap() != null) {
            BMap<BString, BMap<BString, Object>> payloadKeyResourceMap =
                    servicesRegistry.getPayloadKeyResourceMap();
            String resourceName = retrieveResourceNameForKey(jsonBody, payloadKeyResourceMap);
            if (resourceName != null) {
                return resourceName;
            }
        }
        throw new BallerinaConnectorException("Matching resource not found for dispatching based on Header and "
                                                      + "Payload Key");
    }

    /**
     * Method to retrieve the resource name when the mapping between topic and resource for custom subscriber services
     * is specified as a key of the JSON payload.
     *
     * @param inboundRequest         the request received
     * @param payloadKeyResourceMap  the mapping between the topics defined as a value of a payload key and resources
     * @return                       the name of the resource as identified based on the topic
     * @throws BallerinaConnectorException if a resource could not be mapped to the topic identified
     */
    private static String retrieveResourceName(HttpCarbonMessage inboundRequest,
                                               BMap<BString, BMap<BString, Object>> payloadKeyResourceMap) {
        BObject httpRequest = getHttpRequest(inboundRequest);
        
        BMap<BString, ?> requestBody = getJsonBody(httpRequest);
        
        inboundRequest.setProperty(ENTITY_ACCESSED_REQUEST, httpRequest);
        
        String resourceName = retrieveResourceNameForKey(requestBody, payloadKeyResourceMap);
        
        if (Objects.nonNull(resourceName) && !resourceName.isEmpty()) {
            return resourceName;
        } else {
            throw new BallerinaConnectorException("Matching resource not found for dispatching based on Payload Key");
        }
    }

    private static String retrieveResourceNameForKey(BMap<BString, ?> jsonBody,
                                                     BMap<BString, BMap<BString, Object>>
                                                             payloadKeyResourceMap) {
        for (BString key : payloadKeyResourceMap.getKeys()) {
            if (jsonBody.containsKey(key)) {
                BMap<BString, Object> topicResourceMapForValue = payloadKeyResourceMap.get(key);
                BString valueForKey = (BString) jsonBody.get(key);
                if (topicResourceMapForValue.containsKey(valueForKey)) {
                    return retrieveResourceNameFromTopic(valueForKey, topicResourceMapForValue);
                }
            }
        }
        return null;
    }


    /**
     * Method to retrieve the resource name from the topic -- resource map for a topic.
     *
     * @param topic             the topic for which the resource needs to be identified
     * @param topicResourceMap  the mapping between the topics and resources
     * @return                  the name of the resource as identified based on the topic
     * @throws BallerinaConnectorException if a resource could not be mapped to the topic
     */
    private static String retrieveResourceNameFromTopic(BString topic, BMap<BString, Object> topicResourceMap) {
        if (topicResourceMap.containsKey(topic)) {
            return ((BArray) topicResourceMap.get(topic)).getRefValue(0).toString();
        } else {
            throw new BallerinaConnectorException("resource not specified for topic : " + topic);
        }
    }

       /**
     * Method to retrieve requested HTTP resource for incomming request
     *
     * @param resourceName    {@String} requested resource name
     * @param requestMethod   {@String} HTTP method of incomming request
     * @param service         {@HttpService} HTTP service
     * @return                {@HttpResource} requested HTTP resource if found
     * @throws BallerinaConnectorException for any erroneous scenarios
     */
    private static HttpResource getResource(String resourceName, 
                                            String requestMethod,
                                            HttpService service, 
                                            HttpCarbonMessage inboundRequest) {
        HttpResource requestedResource = null;

        for (HttpResource resource : service.getResources()) {
            if (resource.getName().equals(resourceName)) {
                requestedResource = resource;
                break;
            }
        }

        if (Objects.isNull(requestedResource)) {
            if (RESOURCE_NAME_ON_INTENT_VERIFICATION.equals(resourceName)) {
                //if the request is a GET request indicating an intent verification request, and the user has not
                //specified an onIntentVerification resource, assume auto intent verification
                Object target = ((BMap) (service.getBalService().getType()).getAnnotation(StringUtils.fromString(
                        WEBSUB_PACKAGE_FULL_QUALIFIED_NAME + ":" + ANN_NAME_WEBSUB_SUBSCRIBER_SERVICE_CONFIG)))
                        .get(ANN_WEBSUB_ATTR_TARGET);
                String annotatedTopic = "";
    
                if (target instanceof BArray) {
                    annotatedTopic = ((BArray) target).getString(1);
                }
    
                if (annotatedTopic.isEmpty() && service instanceof WebSubHttpService) {
                    annotatedTopic = ((WebSubHttpService) service).getTopic();
                }
                inboundRequest.setProperty(ANNOTATED_TOPIC, annotatedTopic);
                inboundRequest.setProperty(HTTP_RESOURCE, ANNOTATED_TOPIC);
            } else if (RESOURCE_NAME_ON_SUBSCRIPTION_DENIED.equals(resourceName)) {
                inboundRequest.setHttpStatusCode(200);
                throw new BallerinaConnectorException("On subscription denied request is not handled in the service");
            } else {
                inboundRequest.setHttpStatusCode(404);
                throw new BallerinaConnectorException(String.format("no matching WebSub Subscriber service  resource [%s] found for method [%s]", resourceName, requestMethod));
            }
        }

        return requestedResource;
    }

}

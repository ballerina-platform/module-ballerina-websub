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

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.utils.XmlUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BXml;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.ballerinalang.langlib.value.CloneWithType;
import org.ballerinalang.mime.util.MimeUtil;
import org.ballerinalang.net.http.BallerinaHTTPConnectorListener;
import org.ballerinalang.net.http.HttpConstants;
import org.ballerinalang.net.http.HttpResource;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.net.transport.message.HttpCarbonMessage;
import org.ballerinalang.net.transport.message.HttpMessageDataStreamer;
import org.ballerinalang.net.uri.URIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.ballerinalang.mime.util.MimeConstants.TEXT_PLAIN;
import static org.ballerinalang.net.http.HttpConstants.CALLER;
import static org.ballerinalang.net.http.HttpConstants.HTTP_LISTENER_ENDPOINT;
import static org.ballerinalang.net.http.HttpConstants.PROTOCOL_HTTP_PKG_ID;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.ANNOTATED_TOPIC;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.EMPTY;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.ENTITY_ACCESSED_REQUEST;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.PARAM_HUB_CHALLENGE;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.PARAM_HUB_LEASE_SECONDS;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.PARAM_HUB_MODE;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.PARAM_HUB_TOPIC;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.REQUEST;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.RESOURCE_NAME_ON_INTENT_VERIFICATION;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.RESOURCE_NAME_ON_NOTIFICATION;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.RESOURCE_NAME_ON_SUBSCRIPTION_DENIED;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.SUBSCRIBE;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.SUBSCRIPTION_VERIFICATION_HUB_CHALLENGE;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.SUBSCRIPTION_VERIFICATION_HUB_MODE;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.SUBSCRIPTION_VERIFICATION_HUB_TOPIC;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.SUBSCRIPTION_VERIFICATION_LEASE_SECONDS;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.UNSUBSCRIBE;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.WEBSUB_CONTENT_DISTRIBUTION_MESSAGE;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.WEBSUB_NOTIFICATION_REQUEST;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.WEBSUB_PACKAGE_ID;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.WEBSUB_SERVICE_CALLER;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.WEBSUB_SUBSCRIPTION_VERIFICATION_MESSAGE;
import static org.ballerinalang.net.websub.WebSubUtils.getHttpRequest;
import static org.ballerinalang.net.websub.WebSubUtils.getJsonBody;

/**
 * HTTP Connection Listener for Ballerina WebSub services.
 */
public class BallerinaWebSubConnectorListener extends BallerinaHTTPConnectorListener {

    private static final Logger log = LoggerFactory.getLogger(BallerinaWebSubConnectorListener.class);
    private final Runtime runtime;
    private WebSubServicesRegistry webSubServicesRegistry;
    private BObject subscriberServiceListener;
    private PrintStream console = System.out;

    public BallerinaWebSubConnectorListener(Runtime runtime, WebSubServicesRegistry webSubServicesRegistry,
                                            BMap endpointConfig, BObject subscriberServiceListener) {
        super(webSubServicesRegistry, endpointConfig);
        this.runtime = runtime;
        this.webSubServicesRegistry = webSubServicesRegistry;
        this.subscriberServiceListener = subscriberServiceListener;
    }

    @Override
    public void onMessage(HttpCarbonMessage inboundMessage) {
        try {
            HttpResource httpResource;
            if (accessed(inboundMessage)) {
                if (inboundMessage.getProperty(HTTP_RESOURCE) instanceof String) {
                    if (inboundMessage.getProperty(HTTP_RESOURCE).equals(ANNOTATED_TOPIC)) {
                        autoRespondToIntentVerification(inboundMessage);
                        return;
                    } else {
                        //if deferred for dispatching based on payload
                        httpResource = WebSubDispatcher.findResource(webSubServicesRegistry, inboundMessage);
                    }
                } else {
                    httpResource = (HttpResource) inboundMessage.getProperty(HTTP_RESOURCE);
                }
                extractPropertiesAndStartResourceExecution(inboundMessage, httpResource);
                return;
            }
            httpResource = WebSubDispatcher.findResource(webSubServicesRegistry, inboundMessage);
            //TODO: fix to avoid defering on GET, when onIntentVerification is included
            if (inboundMessage.getProperty(HTTP_RESOURCE) == null) {
                inboundMessage.setProperty(HTTP_RESOURCE, httpResource);
                return;
            } else if (inboundMessage.getProperty(HTTP_RESOURCE) instanceof String) {
                return;
            }
            extractPropertiesAndStartResourceExecution(inboundMessage, httpResource);
        } catch (BallerinaConnectorException ex) {
            try {
                HttpUtil.handleFailure(inboundMessage, ex.getMessage());
            } catch (Exception e) {
                log.error("Cannot handle error using the error handler for: " + e.getMessage(), e);
            }
        } catch (Throwable t) {
            log.error("Internal server error", t);
            HttpCarbonMessage response = HttpUtil.createHttpCarbonMessage(false);
            response.waitAndReleaseAllEntities();
            response.setHttpStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
            response.addHttpContent(new DefaultLastHttpContent());
            HttpUtil.sendOutboundResponse(inboundMessage, response);
            return;
        }
    }

    @SuppressWarnings("unchecked")
    protected void extractPropertiesAndStartResourceExecution(HttpCarbonMessage httpCarbonMessage,
                                                              HttpResource httpResource) {
        int paramIndex = 0;
        BObject httpRequest;
        if (httpCarbonMessage.getProperty(ENTITY_ACCESSED_REQUEST) != null) {
            httpRequest = (BObject) httpCarbonMessage.getProperty(ENTITY_ACCESSED_REQUEST);
        } else {
            httpRequest = getHttpRequest(httpCarbonMessage);
        }

        BMap<BString, Object> subscriptionVerificationMessage = ValueCreator.createRecordValue(WEBSUB_PACKAGE_ID,
                WEBSUB_SUBSCRIPTION_VERIFICATION_MESSAGE);

        MethodType balResource = httpResource.getRemoteFunction();
        List<Type> paramTypes = httpResource.getParamTypes();
        Object[] signatureParams = new Object[paramTypes.size() * 2];
        String resourceName = httpResource.getName();
        if (RESOURCE_NAME_ON_INTENT_VERIFICATION.equals(resourceName)) {
            if (httpCarbonMessage.getProperty(HttpConstants.QUERY_STR) != null) {
                String queryString = (String) httpCarbonMessage.getProperty(HttpConstants.QUERY_STR);
                BMap<BString, Object> params = ValueCreator.createMapValue();
                try {
                    URIUtil.populateQueryParamMap(queryString, params);
                    subscriptionVerificationMessage.put(StringUtils.fromString(SUBSCRIPTION_VERIFICATION_HUB_MODE),
                            getParamStringValue(params, PARAM_HUB_MODE));
                    subscriptionVerificationMessage.put(StringUtils.fromString(SUBSCRIPTION_VERIFICATION_HUB_TOPIC),
                            getParamStringValue(params, PARAM_HUB_TOPIC));
                    subscriptionVerificationMessage.put(StringUtils.fromString(SUBSCRIPTION_VERIFICATION_HUB_CHALLENGE),
                            getParamStringValue(params, PARAM_HUB_CHALLENGE));
                    if (params.containsKey(PARAM_HUB_LEASE_SECONDS)) {
                        long leaseSec = Long.parseLong(getParamStringValue(params, PARAM_HUB_LEASE_SECONDS).getValue());
                        subscriptionVerificationMessage.put(StringUtils.fromString(SUBSCRIPTION_VERIFICATION_LEASE_SECONDS),
                                leaseSec);
                    }
                } catch (UnsupportedEncodingException e) {
                    log.error("Error populating query map for intent verification request received: "
                            + e.getMessage());
                    HttpCarbonMessage response = HttpUtil.createHttpCarbonMessage(false);
                    response.waitAndReleaseAllEntities();
                    response.setHttpStatusCode(HttpResponseStatus.NOT_FOUND.code());
                    response.addHttpContent(new DefaultLastHttpContent());
                    HttpUtil.sendOutboundResponse(httpCarbonMessage, response);
                    return;
                }
            }
            signatureParams[paramIndex++] = subscriptionVerificationMessage;
            signatureParams[paramIndex] = true;
        } else if (RESOURCE_NAME_ON_SUBSCRIPTION_DENIED.equals(resourceName)) {
            String queryString = (String) httpCarbonMessage.getProperty(HttpConstants.QUERY_STR);
            BMap<BString, Object> params = ValueCreator.createMapValue();
            String hubReason;
            try {
                URIUtil.populateQueryParamMap(queryString, params);
                hubReason = params.getArrayValue(StringUtils.fromString("hub.reason")).getBString(0).getValue();
            } catch (UnsupportedEncodingException e) {
                log.error("Error populating query map for subscription validation denied request received: "
                        + e.getMessage());
                HttpCarbonMessage response = HttpUtil.createHttpCarbonMessage(false);
                response.setHttpStatusCode(HttpResponseStatus.BAD_REQUEST.code());
                response.addHttpContent(new DefaultLastHttpContent());
                HttpUtil.sendOutboundResponse(httpCarbonMessage, response);
                return;
            }
            signatureParams[paramIndex++] = WebSubUtils.createError("SubscriptionDeniedError", hubReason);
            signatureParams[paramIndex] = true;
        } else { //Notification Resource
            //validate signature for requests received at the callback
            // validateSignature(httpCarbonMessage, httpResource, httpRequest);

            //signatureParams[paramIndex++] = createNotification(httpRequest);
            BMap<BString, Object> contentDistributionMessage = ValueCreator.createRecordValue(WEBSUB_PACKAGE_ID,
                    WEBSUB_CONTENT_DISTRIBUTION_MESSAGE);

            HttpMessageDataStreamer dataStreamer = new HttpMessageDataStreamer(httpCarbonMessage);
            InputStream inputStream = dataStreamer.getInputStream();

            String contentType = httpCarbonMessage.getHeader("Content-Type");
            if (contentType.equalsIgnoreCase("application/json")) {
                Object jsonPayload = JsonUtils.parse(inputStream);
                contentDistributionMessage.put(StringUtils.fromString("content"),
                        jsonPayload);
            } else if (contentType.equalsIgnoreCase("application/xml")) {
                BXml bXml = XmlUtils.parse(inputStream);
                contentDistributionMessage.put(StringUtils.fromString("content"), bXml);
            } else if (contentType.equalsIgnoreCase("text/plain")) {
                BString textContent = StringUtils.getStringFromInputStream(inputStream);
                contentDistributionMessage.put(StringUtils.fromString("content"), textContent);
            } else if (contentType.equalsIgnoreCase("application/octet-stream")) {
                byte[] byteArray;
                try {
                    byteArray = MimeUtil.getByteArray(inputStream);
                } catch (IOException e) {
                    log.error("Internal server error", e);
                    HttpCarbonMessage response = HttpUtil.createHttpCarbonMessage(false);
                    response.waitAndReleaseAllEntities();
                    response.setHttpStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                    response.addHttpContent(new DefaultLastHttpContent());
                    HttpUtil.sendOutboundResponse(httpCarbonMessage, response);
                    return;
                }
                BArray balBArray = ValueCreator.createArrayValue(byteArray);
                contentDistributionMessage.put(StringUtils.fromString("content"), balBArray);
            } else {
                log.error("Error process content update. Invalid Content-Type '" + contentType + "'.");
                HttpCarbonMessage response = HttpUtil.createHttpCarbonMessage(false);
                response.setHttpStatusCode(HttpResponseStatus.BAD_REQUEST.code());
                response.addHttpContent(new DefaultLastHttpContent());
                HttpUtil.sendOutboundResponse(httpCarbonMessage, response);
                return;
            }
            contentDistributionMessage.put(StringUtils.fromString("contentType"), StringUtils.fromString(contentType));

            BMap<BString, Object> mapValue = ValueCreator.createMapValue();
            HttpHeaders headers = httpCarbonMessage.getHeaders();
            for (Map.Entry<String, String> header : headers) {
                mapValue.put(StringUtils.fromString(header.getKey()), StringUtils.fromString(header.getValue()));
            }
            if (!mapValue.isEmpty()) {
                contentDistributionMessage.put(StringUtils.fromString("headers"), mapValue);
            }

            signatureParams[paramIndex++] = contentDistributionMessage;
            signatureParams[paramIndex++] = true;
            if (!RESOURCE_NAME_ON_NOTIFICATION.equals(balResource.getName())) {
                Object customRecordOrError = createCustomNotification(httpCarbonMessage, balResource, httpRequest);
                if (TypeUtils.getType(customRecordOrError).getTag() == TypeTags.ERROR_TAG) {
                    log.error("Data binding failed: " + ((BError) customRecordOrError).getPrintableStackTrace());
                    return;
                }

                signatureParams[paramIndex++] = customRecordOrError;
                signatureParams[paramIndex] = true;
            }
        }

        CountDownLatch latch = new CountDownLatch(1);
        WebSubEmptyCallableUnitCallback callback = new WebSubEmptyCallableUnitCallback(latch);
        //TODO handle BallerinaConnectorException
        BObject service = httpResource.getParentService().getBalService();
        runtime.invokeMethodAsync(service, balResource.getName(), null, null, callback, signatureParams);

        HttpCarbonMessage response = HttpUtil.createHttpCarbonMessage(false);
        response.waitAndReleaseAllEntities();
        if (RESOURCE_NAME_ON_INTENT_VERIFICATION.equals(resourceName)) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                // Ignore
            }
            Object result = callback.getResult();
            if (result instanceof BError) {
                BError subscriptionVerificationError = ((BError) result);
                String payload = subscriptionVerificationError.getMessage();
                response.setHttpStatusCode(HttpResponseStatus.NOT_FOUND.code());
                response.addHttpContent(new DefaultLastHttpContent(Unpooled.wrappedBuffer(payload.getBytes())));
            } else {
                BMap<BString, Object> verificationSuccessResult = (BMap<BString, Object>) result;
                updateResponseWithHeadersAndBody(response, verificationSuccessResult);

                String payload = subscriptionVerificationMessage.getStringValue(StringUtils.fromString("hubChallenge")).getValue();
                response.addHttpContent(new DefaultLastHttpContent(Unpooled.wrappedBuffer(payload.getBytes())));
                response.setHttpStatusCode(HttpResponseStatus.OK.code());
            }
        } else if (RESOURCE_NAME_ON_SUBSCRIPTION_DENIED.equals(resourceName)) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                // Ignore
            }
            Object result = callback.getResult();
            BMap<BString, Object> acknowledgementResult = (BMap<BString, Object>) result;
            updateResponseWithHeadersAndBody(response, acknowledgementResult);
            response.setHttpStatusCode(HttpResponseStatus.OK.code());
        } else {
            response.setHttpStatusCode(HttpResponseStatus.ACCEPTED.code());
            response.addHttpContent(new DefaultLastHttpContent());
        }
        HttpUtil.sendOutboundResponse(httpCarbonMessage, response);
        return;
    }

    private void updateResponseWithHeadersAndBody(HttpCarbonMessage response, BMap<BString, Object> balRecord) {

        String payload = "";
        BMap<BString, Object> body =
                ((BMap<BString, Object>) balRecord.getMapValue(StringUtils.fromString("body")));
        if (body != null && !body.isEmpty()) {
            for (Map.Entry<BString, Object> objectEntry : body.entrySet()) {
                payload = payload + objectEntry.getKey().getValue() + "=" + ((BString) objectEntry.getValue()).getValue() + "&";
            }
            if (body.size() >= 1) {
                payload = payload.substring(0, payload.length() - 2);
            }
            response.setHeader("Content-Type", "application/x-www-form-urlencoded");
            response.addHttpContent(new DefaultLastHttpContent(Unpooled.wrappedBuffer(payload.getBytes())));
        }

        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        BMap<BString, Object> headers =
                (BMap<BString, Object>) balRecord.getMapValue(StringUtils.fromString("headers"));
        if (headers != null) {
            headers.entrySet().forEach((objectEntry) -> {
                if (objectEntry.getValue() instanceof BString) {
                    httpHeaders.add(objectEntry.getKey().getValue(), ((BString) objectEntry.getValue()).getValue());
                } else {
                    BArray headerElements = (BArray) objectEntry.getValue();
                    if (headerElements.size() > 1) {
                        Iterator<String> valueIterator = Arrays.stream(headerElements.getStringArray()).map(String::valueOf)
                                .iterator();
                        httpHeaders.add(objectEntry.getKey().getValue(), valueIterator);
                    } else if (headerElements.size() == 1) {
                        httpHeaders.set(objectEntry.getKey().getValue(), headerElements.getBString(0).getValue());
                    }

                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private void validateSignature(HttpCarbonMessage httpCarbonMessage, HttpResource httpResource,
                                   BObject request) {

        //invoke processWebSubNotification function
        final BError[] returnValue = new BError[1];
        Object[] args = {request, true, httpResource.getParentService().getBalService(), true};
        CountDownLatch completeFunction = new CountDownLatch(1);
        runtime.invokeMethodAsync(subscriberServiceListener, "processWebSubNotification", null, null,
                                  new Callback() {
                                      @Override
                                      public void notifySuccess(Object result) {
                                          completeFunction.countDown();
                                      }

                                      @Override
                                      public void notifyFailure(BError error) {
                                          returnValue[0] = error;
                                          completeFunction.countDown();
                                      }
                                  }, args);
        try {
            completeFunction.await();
        } catch (InterruptedException ex) {
            log.debug("Signature Validation failed: " + ex.getMessage());
            httpCarbonMessage.setHttpStatusCode(404);
            throw new BallerinaConnectorException(ex);
        }
        BError error = returnValue[0];
        if (error != null) {
            log.debug("Signature Validation failed for Notification: " + error.getMessage());
            httpCarbonMessage.setHttpStatusCode(404);
            throw new BallerinaConnectorException("validation failed for notification");
        }
    }

    /**
     * Method to retrieve the struct representing the WebSub subscriber service endpoint.
     *
     * @param httpResource      the resource of the service receiving the request
     * @param httpCarbonMessage the HTTP message representing the request received
     * @param endpointConfig    listener endpoint configuration
     * @return the struct representing the subscriber service endpoint
     */
    private BObject getWebSubCaller(HttpResource httpResource, HttpCarbonMessage httpCarbonMessage,
                                        BMap endpointConfig) {
        BObject httpServiceServer = ValueCreator.createObjectValue(PROTOCOL_HTTP_PKG_ID, HTTP_LISTENER_ENDPOINT,
                          9090, endpointConfig); // sending a dummy port here as it gets initialized later - fix
        BObject httpCaller = ValueCreator.createObjectValue(PROTOCOL_HTTP_PKG_ID, CALLER);

        HttpUtil.enrichHttpCallerWithConnectionInfo(httpCaller, httpCarbonMessage, httpResource, endpointConfig);
        HttpUtil.enrichHttpCallerWithNativeData(httpCaller, httpCarbonMessage, endpointConfig);
        httpServiceServer.addNativeData(HttpConstants.SERVICE_ENDPOINT_CONNECTION_FIELD, httpCaller);
        return ValueCreator.createObjectValue(WEBSUB_PACKAGE_ID, WEBSUB_SERVICE_CALLER, httpCaller);
    }

    /**
     * Method to create the notification request representing WebSub notifications received.
     */
    private BObject createNotification(BObject httpRequest) {
        BObject notification = ValueCreator.createObjectValue(WEBSUB_PACKAGE_ID, WEBSUB_NOTIFICATION_REQUEST);
        notification.set(StringUtils.fromString(REQUEST), httpRequest);
        return notification;
    }

    /**
     * Method to create the notification request struct representing WebSub notifications received.
     */
    private Object createCustomNotification(HttpCarbonMessage inboundRequest, MethodType resource,
                                              BObject httpRequest) {
        RecordType recordType = webSubServicesRegistry.getResourceDetails().get(resource.getName());
        BMap<BString, ?> jsonBody = getJsonBody(httpRequest);
        inboundRequest.setProperty(ENTITY_ACCESSED_REQUEST, httpRequest);
        return CloneWithType.convert(recordType, jsonBody);
    }

    /**
     * Method to automatically respond to intent verification requests for subscriptions/unsubscriptions if a resource
     * named {@link WebSubSubscriberConstants#RESOURCE_NAME_ON_INTENT_VERIFICATION} is not specified.
     *
     * @param httpCarbonMessage the message/request received
     */
    private void autoRespondToIntentVerification(HttpCarbonMessage httpCarbonMessage) {
        HttpCarbonMessage response = HttpUtil.createHttpCarbonMessage(false);
        response.waitAndReleaseAllEntities();

        if (httpCarbonMessage.getProperty(ANNOTATED_TOPIC) == null) {
            console.println("ballerina: Intent Verification denied - expected topic details not found");
            sendIntentVerificiationDenialResponse(httpCarbonMessage, response);
            return;
        }

        if (httpCarbonMessage.getProperty(HttpConstants.QUERY_STR) == null) {
            console.println("ballerina: Intent Verification denied - invalid intent verification request");
            sendIntentVerificiationDenialResponse(httpCarbonMessage, response);
            return;
        }

        String annotatedTopic = httpCarbonMessage.getProperty(ANNOTATED_TOPIC).toString();
        String queryString = (String) httpCarbonMessage.getProperty(HttpConstants.QUERY_STR);
        BMap<BString, Object> params = ValueCreator.createMapValue();
        try {
            URIUtil.populateQueryParamMap(queryString, params);
            if (!params.containsKey(PARAM_HUB_MODE) || !params.containsKey(PARAM_HUB_TOPIC) ||
                    !params.containsKey(PARAM_HUB_CHALLENGE)) {
                sendIntentVerificiationDenialResponse(httpCarbonMessage, response);
                console.println("error: Error auto-responding to intent verification request: Mode, Topic "
                                        + "and/or challenge not specified");
                return;
            }

            BString mode = getParamStringValue(params, PARAM_HUB_MODE);
            if ((SUBSCRIBE.equals(mode) || UNSUBSCRIBE.equals(mode))
                    && annotatedTopic.equals(getParamStringValue(params, PARAM_HUB_TOPIC).getValue())) {
                BString challenge = getParamStringValue(params, PARAM_HUB_CHALLENGE);
                response.addHttpContent(new DefaultLastHttpContent(Unpooled.wrappedBuffer(
                        challenge.getValue().getBytes(StandardCharsets.UTF_8))));
                response.setHeader(HttpHeaderNames.CONTENT_TYPE.toString(), TEXT_PLAIN);
                response.setHttpStatusCode(HttpResponseStatus.ACCEPTED.code());
                String intentVerificationMessage = "ballerina: Intent Verification agreed - Mode [" + mode
                        + "], Topic [" + annotatedTopic + "]";
                if (params.containsKey(PARAM_HUB_LEASE_SECONDS)) {
                    intentVerificationMessage = intentVerificationMessage.concat(
                            ", Lease Seconds [" + getParamStringValue(params, PARAM_HUB_LEASE_SECONDS) + "]");
                }
                console.println(intentVerificationMessage);
                HttpUtil.sendOutboundResponse(httpCarbonMessage, response);
            } else {
                console.println("ballerina: Intent Verification denied - Mode [" + mode + "], Topic ["
                                        + getParamStringValue(params, PARAM_HUB_TOPIC) + "]");
                sendIntentVerificiationDenialResponse(httpCarbonMessage, response);
            }
        } catch (UnsupportedEncodingException e) {
            console.println("ballerina: Intent Verification denied - error extracting query parameters: " +
                                    e.getMessage());
            sendIntentVerificiationDenialResponse(httpCarbonMessage, response);
        }
    }

    private static void sendIntentVerificiationDenialResponse(HttpCarbonMessage httpCarbonMessage,
                                                              HttpCarbonMessage response) {
        response.setHttpStatusCode(HttpResponseStatus.NOT_FOUND.code());
        response.addHttpContent(new DefaultLastHttpContent());
        HttpUtil.sendOutboundResponse(httpCarbonMessage, response);
    }

    private BString getParamStringValue(BMap<BString, Object> params, BString key) {
        if (!params.containsKey(key)) {
            return EMPTY;
        }
        Object param = params.get(key);
        if (TypeUtils.getType(param).getTag() != TypeTags.ARRAY_TAG || ((BArray) param).size() < 1) {
            return EMPTY;
        }
        return StringUtils.fromString(((BArray) param).get(0).toString());
    }
}

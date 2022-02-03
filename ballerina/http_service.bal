// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/http;
import ballerina/log;
import ballerina/jballerina.java;

isolated service class HttpService {
    *http:Service;
    
    private final HttpToWebsubAdaptor adaptor;
    private final string callback;
    private final string? secretKey;
    private final boolean isSubscriptionValidationDeniedAvailable;
    private final boolean isSubscriptionVerificationAvailable;
    private final boolean isUnsubscriptionVerificationAvailable;
    private final boolean isEventNotificationAvailable;
    private boolean unsubscriptionVerified;

    isolated function init(HttpToWebsubAdaptor adaptor, string callback, string? secretKey) returns error? {
        self.adaptor = adaptor;
        self.callback = callback;
        self.secretKey = secretKey;
        self.unsubscriptionVerified = false;
        string[] methodNames = adaptor.getServiceMethodNames();
        self.isSubscriptionValidationDeniedAvailable = isMethodAvailable("onSubscriptionValidationDenied", methodNames);
        self.isSubscriptionVerificationAvailable = isMethodAvailable("onSubscriptionVerification", methodNames);
        self.isUnsubscriptionVerificationAvailable = isMethodAvailable("onUnsubscriptionVerification", methodNames);
        self.isEventNotificationAvailable = isMethodAvailable("onEventNotification", methodNames);
    }

    isolated resource function post .(http:Caller caller, http:Request request) returns Error? {
        http:Response response = new;
        response.statusCode = http:STATUS_ACCEPTED;
        if self.isEventNotificationAvailable {
            string? configuredSecret = self.secretKey;
            string secretKey = configuredSecret is () ? "" : configuredSecret;
            error? result = processEventNotification(caller, request, response, self.adaptor, secretKey);
            if result is error {
                response.statusCode = http:STATUS_INTERNAL_SERVER_ERROR;
            }
        } else {
            response.statusCode = http:STATUS_NOT_IMPLEMENTED;
        }
        check respondToRequest(caller, response);
    }

    isolated resource function get .(http:Caller caller, http:Request request) returns Error? {
        http:Response response = new;
        response.statusCode = http:STATUS_OK;
        RequestQueryParams params = retrieveRequestQueryParams(request);
        match params?.hubMode {
            MODE_SUBSCRIBE | MODE_UNSUBSCRIBE => {
                if params?.hubChallenge is () || params?.hubTopic is () {
                    response.statusCode = http:STATUS_BAD_REQUEST;
                } else {
                    self.processVerification(params, caller, response);
                }
            }
            MODE_DENIED => {
                if self.isSubscriptionValidationDeniedAvailable {
                    processSubscriptionDenial(caller, response, params, self.adaptor);
                } else {
                    log:printError("Subscription is denied by the hub");
                    response.statusCode = http:STATUS_OK;
                    updateResponseBody(response, ACKNOWLEDGEMENT["body"], ACKNOWLEDGEMENT["headers"]);
                }
            }
            _ => {
                response.statusCode = http:STATUS_BAD_REQUEST;
                string errorMessage = "The request does not include valid `hub.mode` form param.";
                response.setTextPayload(errorMessage);
            }
        }
        check respondToRequest(caller, response);
    }

    isolated function processVerification(RequestQueryParams params, http:Caller caller, http:Response response) {
        if params?.hubMode == MODE_SUBSCRIBE && self.isSubscriptionVerificationAvailable {
            processSubscriptionVerification(caller, response, params, self.adaptor);
        } else if params?.hubMode == MODE_UNSUBSCRIBE && self.isUnsubscriptionVerificationAvailable {
            processUnsubscriptionVerification(caller, response, params, self.adaptor);
        } else {
            response.statusCode = http:STATUS_OK;
            response.setTextPayload(<string>params?.hubChallenge);
        }

        // if the received verification event is unsubscription, then update the internal state
        if params?.hubMode == MODE_UNSUBSCRIBE {
            lock {
                self.unsubscriptionVerified = true;
            }
        }
    }

    public isolated function initiateSubscription() returns error? {
        SubscriberServiceConfiguration? config = self.retrieveSubscriberConfig();
        if config is SubscriberServiceConfiguration {
            check subscribe(config, self.callback);
        }
    }

    public isolated function initiateUnsubscription() returns error? {
        SubscriberServiceConfiguration? config = self.retrieveSubscriberConfig();
        if config is SubscriberServiceConfiguration {
            // if unsubscribe on-shutdown is switched off, do not proceed
            if !config.unsubscribeOnShutdown {
                // since unsubscribe on-shutdown is switched off, marking unsubscription verified
                // otherwise the listener will wait for graceful-shutdown timeout
                lock {
                    self.unsubscriptionVerified = true;
                }
                return;
            }
            log:printInfo("Unsubscribing from the hub...");
            check unsubscribe(config, self.callback);
        }
    }

    public isolated function isUnsubscriptionVerified() returns boolean {
        lock {
            return self.unsubscriptionVerified;
        }
    }

    isolated function retrieveSubscriberConfig() returns SubscriberServiceConfiguration? = @java:Method {
        'class: "io.ballerina.stdlib.websub.NativeHttpToWebsubAdaptor"
    } external;
}

isolated function subscribe(SubscriberServiceConfiguration config, string callback) returns error? {
    string hub;
    string topic;
    [string, string]? resourceDetails = check retrieveResourceDetails(config);
    if resourceDetails is [string, string] {
        [hub, topic] = resourceDetails;
    } else {
        log:printWarn("Subscription not initiated as subscriber target-URL is not provided");
        return;
    }
    SubscriptionClient subscriberClientEp = check getSubscriberClient(hub, config?.httpConfig);
    SubscriptionChangeRequest request = retrieveSubscriptionRequest(topic, config, callback);
    SubscriptionChangeResponse response = check subscriberClientEp->subscribe(request);
    string subscriptionSuccessMsg = string `Subscription Request successfully sent to Hub[${response.hub}], 
                    for Topic[${response.topic}], with Callback [${callback}]`;
    log:printDebug(subscriptionSuccessMsg);
}

isolated function unsubscribe(SubscriberServiceConfiguration config, string callback) returns error? {
    string hub;
    string topic;
    [string, string]? resourceDetails = check retrieveResourceDetails(config);
    if resourceDetails is [string, string] {
        [hub, topic] = resourceDetails;
    } else {
        log:printWarn("Unsubscription not initiated as subscriber target-URL is not provided");
        return;
    }

    SubscriptionClient subscriberClientEp = check getSubscriberClient(hub, config?.httpConfig);
    SubscriptionChangeRequest request = retrieveSubscriptionRequest(topic, config, callback);
    SubscriptionChangeResponse response = check subscriberClientEp->unsubscribe(request);
    string subscriptionSuccessMsg = string `Unubscription Request successfully sent to Hub[${response.hub}], 
                    for Topic[${response.topic}], with Callback [${callback}]`;
    log:printDebug(subscriptionSuccessMsg);
}

isolated function retrieveResourceDetails(SubscriberServiceConfiguration serviceConfig) returns [string, string]|error? {
    string|[string, string]? target = serviceConfig?.target;
    if target is string {
        var discoveryConfig = serviceConfig?.discoveryConfig;
        string?|string[] expectedMediaTypes = discoveryConfig?.accept ?: ();
        string?|string[] expectedLanguageTypes = discoveryConfig?.acceptLanguage ?: ();
        DiscoveryService discoveryClient = check new (target, discoveryConfig?.httpConfig);
        [string, string] resourceDetails = check discoveryClient->discoverResourceUrls(expectedMediaTypes, expectedLanguageTypes);
        return resourceDetails;
    } else if target is [string, string] {
        return target;
    }
    return;
}

isolated function getSubscriberClient(string hubUrl, http:ClientConfiguration? config) returns SubscriptionClient|error {
    if config is http:ClientConfiguration {
        return check new SubscriptionClient(hubUrl, config); 
    } else {
        return check new SubscriptionClient(hubUrl);
    }
}

isolated function isMethodAvailable(string methodName, string[] methods) returns boolean {
    return methods.indexOf(methodName) is int;
}

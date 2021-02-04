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

service class HttpService {
    private SubscriberService subscriberService;
    private SubscriberServiceConfiguration serviceConfig;
    private string callbackUrl;
    private boolean isSubscriptionValidationDeniedAvailable = false;
    private boolean isSubscriptionVerificationAvailable = false;
    private boolean isEventNotificationAvailable = false;

    # Invoked during the initialization of a `websub:HttpService`
    #
    # + subscriberService   - {@code websub:SubscriberService} provided service
    # + serviceConfig       - {@code SubscriberServiceConfiguration} subscriber-service
    #                          related configurations
    # + callback            - {@code string} dynamically generated callback-url
    public function init(SubscriberService subscriberService, SubscriberServiceConfiguration config, 
                         string callback) returns error? {
        self.subscriberService = subscriberService;
        self.serviceConfig = config;
        self.callbackUrl = callback;
        
        string[] methodNames = getServiceMethodNames(subscriberService);
        
        foreach var methodName in methodNames {
            match methodName {
                "onSubscriptionValidationDenied" => {
                    self.isSubscriptionValidationDeniedAvailable = true;
                }
                "onSubscriptionVerification" => {
                    self.isSubscriptionVerificationAvailable = true;
                }
                "onEventNotification" => {
                    self.isEventNotificationAvailable = true;
                }
                _ => {
                    log:printError("Unrecognized method [" + methodName + "] found in the implementation");
                }
            }
        }

        var result = self.initiateSubscription();
        if (result is error) {
            string errorMsg = "Subscription initiation failed due to [" + result.message() + "]";
            return error SubscriptionInitiationFailedError(errorMsg);
        }
    }

    function initiateSubscription() returns error? {
        string|[string, string] target = self.serviceConfig.target;
        
        string hubUrl;
        string topicUrl;
        
        if (target is string) {
            var discoveryConfig = self.serviceConfig.discoveryConfig;
            http:ClientConfiguration? discoveryHttpConfig = discoveryConfig?.httpConfig ?: ();
            string?|string[] expectedMediaTypes = discoveryConfig?.accept ?: ();
            string?|string[] expectedLanguageTypes = discoveryConfig?.acceptLanguage ?: ();

            DiscoveryService discoveryClient = check new (target, discoveryHttpConfig);

            var discoveryDetails = discoveryClient->discoverResourceUrls(expectedMediaTypes, expectedLanguageTypes);

            if (discoveryDetails is [string, string]) {
                [hubUrl, topicUrl] = <[string, string]> discoveryDetails;
            } else {
                return error Error(discoveryDetails.message());
            }
        } else {
            [hubUrl, topicUrl] = <[string, string]> target;
        }

        http:ClientConfiguration? subscriptionClientConfig = self.serviceConfig?.httpConfig ?: ();
        SubscriptionClient subscriberClientEp = check new (hubUrl, subscriptionClientConfig);

        string callback = self.serviceConfig?.callback ?: self.callbackUrl;

        var request = retrieveSubscriptionRequest(topicUrl, callback, self.serviceConfig);

        var response = subscriberClientEp->subscribe(request);

        if (response is SubscriptionChangeResponse) {
            string subscriptionSuccessMsg = "Subscription Request successfully sent to Hub["
                                            + response.hub + "], for Topic[" 
                                            + response.topic + "], with Callback [" + callback + "]";
            log:print(subscriptionSuccessMsg + ". Awaiting intent verification.");
        } else {
            return response;
        }
    }

    resource function post .(http:Caller caller, http:Request request) {
        http:Response response = new;
        response.statusCode = http:STATUS_ACCEPTED;

        if (self.isEventNotificationAvailable) {
            string secretKey = self.serviceConfig?.secret ?: "";
            processEventNotification(caller, request, response, self.subscriberService, secretKey);
        } else {
            response.statusCode = http:STATUS_NOT_IMPLEMENTED;
        }

        respondToRequest(caller, response);
    }

    resource function get .(http:Caller caller, http:Request request) {
        http:Response response = new;
        response.statusCode = http:STATUS_OK;

        RequestQueryParams params = retrieveRequestQueryParams(request);

        match params.hubMode {
            MODE_SUBSCRIBE | MODE_UNSUBSCRIBE => {
                if (params.hubChallenge is () || params.hubTopic is ()) {
                    response.statusCode = http:STATUS_BAD_REQUEST;
                } else {
                    if (self.isSubscriptionVerificationAvailable) {
                        processSubscriptionVerification(caller, response, <@untainted> params, self.subscriberService);
                    } else {
                        response.statusCode = http:STATUS_OK;
                        response.setTextPayload(<string>params.hubChallenge);
                    }
                }
            }
            MODE_DENIED => {
                if (self.isSubscriptionValidationDeniedAvailable) {
                    processSubscriptionDenial(caller, response, <@untainted> params, self.subscriberService);
                } else {
                    response.statusCode = http:STATUS_OK;
                    Acknowledgement result = {
                        body: {
                            "message": "Subscription Denial Acknowledged"
                        }
                    };
                    updateResponseBody(response, result["body"], result["headers"]);
                }
            }
            _ => {
                response.statusCode = http:STATUS_BAD_REQUEST;
                string errorMessage = "The request does not include valid `hub.mode` form param.";
                response.setTextPayload(errorMessage);
            }
        }

        respondToRequest(caller, response);
    }
}

isolated function getServiceMethodNames(SubscriberService subscriberService) returns string[] = @java:Method {
    'class: "io.ballerina.stdlib.websub.SubscriberNativeOperationHandler"
} external;
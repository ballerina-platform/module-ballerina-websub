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

# Represent underlying HTTP Service on top of which the Subscriber-Serivice runs.
service class HttpService {
    private SubscriberService subscriberService;
    private string? secretKey;
    private boolean isSubscriptionValidationDeniedAvailable = false;
    private boolean isSubscriptionVerificationAvailable = false;
    private boolean isEventNotificationAvailable = false;

    # Invoked during the initialization of a `websub:HttpService`
    #
    # + subscriberService - {@code websub:SubscriberService} provided service
    # + serviceConfig - {@code SubscriberServiceConfiguration} subscriber-service
    #                   related configurations
    # + callback - {@code string} dynamically generated callback-url
    isolated function init(SubscriberService subscriberService, string? secretKey) returns error? {
        self.subscriberService = subscriberService;
        self.secretKey = secretKey;
        
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
                    log:printError(string`Unrecognized method [${methodName}] found in the implementation`);
                }
            }
        }
    }

    # Resource-Method handling the HTTP POST requests
    # 
    # + caller - {@code http:Caller} reference
    # + request - {@code http:Request} reference
    isolated resource function post .(http:Caller caller, http:Request request) {
        http:Response response = new;
        response.statusCode = http:STATUS_ACCEPTED;
        if (self.isEventNotificationAvailable) {
            string secretKey = self.secretKey is () ? "" : <string>self.secretKey;
            var result = processEventNotification(caller, request, response, self.subscriberService, secretKey);
            if (result is error) {
                response.statusCode = http:STATUS_INTERNAL_SERVER_ERROR;
            }
        } else {
            response.statusCode = http:STATUS_NOT_IMPLEMENTED;
        }

        respondToRequest(caller, response);
    }

    # Resource-Method handling the HTTP GET requests
    # 
    # + caller - {@code http:Caller} reference
    # + request - {@code http:Request} reference
    isolated resource function get .(http:Caller caller, http:Request request) {
        http:Response response = new;
        response.statusCode = http:STATUS_OK;

        RequestQueryParams params = retrieveRequestQueryParams(request);

        match params?.hubMode {
            MODE_SUBSCRIBE | MODE_UNSUBSCRIBE => {
                if (params?.hubChallenge is () || params?.hubTopic is ()) {
                    response.statusCode = http:STATUS_BAD_REQUEST;
                } else {
                    if (self.isSubscriptionVerificationAvailable) {
                        processSubscriptionVerification(caller, response, <@untainted> params, self.subscriberService);
                    } else {
                        response.statusCode = http:STATUS_OK;
                        response.setTextPayload(<string>params?.hubChallenge);
                    }
                }
            }
            MODE_DENIED => {
                if (self.isSubscriptionValidationDeniedAvailable) {
                    processSubscriptionDenial(caller, response, <@untainted> params, self.subscriberService);
                } else {
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

        respondToRequest(caller, response);
    }
}

# Invoke native method to retrive implemented method names in the subscriber service
# 
# + subscriberService - current subscriber-service
# + return - {@code string[]} containing the method-names in current implementation
isolated function getServiceMethodNames(SubscriberService subscriberService) returns string[] = @java:Method {
    'class: "io.ballerina.stdlib.websub.SubscriberNativeOperationHandler"
} external;
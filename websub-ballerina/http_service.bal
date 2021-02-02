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
import ballerina/jballerina.java;

service class HttpService {
    private SubscriberService subscriberService;
    private SubscriberServiceConfiguration? configurations;
    private boolean isSubscriptionValidationDeniedAvailable = false;
    private boolean isSubscriptionVerificationAvailable = false;
    private boolean isEventNotificationAvailable = false;

    # Invoked during the initialization of a `websub:HttpService`
    #
    # + subscriberService   - {@code websub:SubscriberService} provided service
    public isolated function init(SubscriberService subscriberService) returns error? {
        self.subscriberService = subscriberService;
        
        string[] methodNames = getServiceMethodNames(subscriberService);

        self.configurations = retrieveSubscriberServiceAnnotations(subscriberService);
        if (self.configurations is ()) {
            return error ServiceInitializationError("Could not find the required service-configurations");
        }
        
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
                _ => {}
            }
        }
    }

    resource function post .(http:Caller caller, http:Request request) {
        http:Response response = new;
        response.statusCode = http:STATUS_ACCEPTED;

        if (self.isEventNotificationAvailable) {
            string secret = self.configurations?.secret ?: "";
            processEventNotification(caller, request, response, 
                                     self.subscriberService, 
                                     secret);
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
                if (self.isSubscriptionVerificationAvailable) {
                    processSubscriptionVerification(caller, response, <@untainted> params, self.subscriberService);
                } else {
                    response.statusCode = http:STATUS_OK;
                    response.setTextPayload(params.hubChallenge);
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
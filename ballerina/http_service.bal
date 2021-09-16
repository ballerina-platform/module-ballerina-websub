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

# Represents an underlying HTTP Service.
isolated service class HttpService {
    private final HttpToWebsubAdaptor adaptor;
    private final string? secretKey;
    private final boolean isSubscriptionValidationDeniedAvailable;
    private final boolean isSubscriptionVerificationAvailable;
    private final boolean isUnsubscriptionVerificationAvailable;
    private final boolean isEventNotificationAvailable;

    # Initializes `websub:HttpService` endpoint.
    # ```ballerina
    # websub:HttpService httpServiceEp = check new (adaptor, "sercretKey1");
    # ```
    # 
    # + adaptor - The `websub:HttpToWebsubAdaptor` instance which used as a wrapper to execute service methods
    # + callback - Optional `secretKey` value to be used in the content distribution verification
    isolated function init(HttpToWebsubAdaptor adaptor, string? secretKey) {
        self.adaptor = adaptor;
        self.secretKey = secretKey;
        string[] methodNames = adaptor.getServiceMethodNames();
        self.isSubscriptionValidationDeniedAvailable = isMethodAvailable("onSubscriptionValidationDenied", methodNames);
        self.isSubscriptionVerificationAvailable = isMethodAvailable("onSubscriptionVerification", methodNames);
        self.isUnsubscriptionVerificationAvailable = isMethodAvailable("onUnsubscriptionVerification", methodNames);
        self.isEventNotificationAvailable = isMethodAvailable("onEventNotification", methodNames);
    }

    # Receives HTTP POST requests.
    # 
    # + caller - The `http:Caller` reference for the current request
    # + request - Received `http:Request` instance
    isolated resource function post .(http:Caller caller, http:Request request) {
        http:Response response = new;
        response.statusCode = http:STATUS_ACCEPTED;
        if self.isEventNotificationAvailable {
            string secretKey = self.secretKey is () ? "" : <string>self.secretKey;
            error? result = processEventNotification(caller, request, response, self.adaptor, secretKey);
            if result is error {
                response.statusCode = http:STATUS_INTERNAL_SERVER_ERROR;
            }
        } else {
            response.statusCode = http:STATUS_NOT_IMPLEMENTED;
        }

        respondToRequest(caller, response);
    }

    # Receives HTTP GET requests.
    # 
    # + caller - The `http:Caller` reference for the current request
    # + request - Received `http:Request` instance
    isolated resource function get .(http:Caller caller, http:Request request) {
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

    isolated function processVerification(RequestQueryParams params, http:Caller caller, http:Response response) {
        if params?.hubMode == MODE_SUBSCRIBE && self.isSubscriptionVerificationAvailable {
            processSubscriptionVerification(caller, response, params, self.adaptor);
        } else if params?.hubMode == MODE_UNSUBSCRIBE && self.isUnsubscriptionVerificationAvailable {
            processUnsubscriptionVerification(caller, response, params, self.adaptor);
        } else {
            response.statusCode = http:STATUS_OK;
            response.setTextPayload(<string>params?.hubChallenge);
        }
    }
}

# Retrieves whether the particular remote method is available.
# 
# + methodName - Name of the required method
# + methods - All available methods
# + return - `true` if method available or else `false`
isolated function isMethodAvailable(string methodName, string[] methods) returns boolean {
    return methods.indexOf(methodName) is int;
}

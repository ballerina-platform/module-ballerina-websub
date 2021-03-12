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
import ballerina/mime;

# Porcesses the subscription / unsubscription intent verification requests from `hub`
# 
# + caller - {@code http:Caller} reference
# + response - {@code http:Response} to be returned to the caller
# + params - query parameters retrieved from the {@code http:Request}
# + subscriberService - service to be invoked via native method
isolated function processSubscriptionVerification(http:Caller caller, http:Response response, 
                                                  RequestQueryParams params, SubscriberService subscriberService) {
    SubscriptionVerification message = {
        hubMode: <string>params.hubMode,
        hubTopic: <string>params.hubTopic,
        hubChallenge: <string>params.hubChallenge,
        hubLeaseSeconds: params.hubLeaseSeconds
    };

    SubscriptionVerificationSuccess|SubscriptionVerificationError result = callOnSubscriptionVerificationMethod(subscriberService, message);

    if (result is SubscriptionVerificationError) {
        result = <SubscriptionVerificationError>result;
        response.statusCode = http:STATUS_NOT_FOUND;
        var errorDetails = result.detail();
        updateResponseBody(response, errorDetails["body"], errorDetails["headers"], result.message());
    } else {
        response.statusCode = http:STATUS_OK;
        response.setTextPayload(<string>params.hubChallenge);
    }
}

# Porcesses the subscription / unsubscription denial requests from `hub`
# 
# + caller - {@code http:Caller} reference
# + response - {@code http:Response} to be returned to the caller
# + params - query parameters retrieved from the {@code http:Request}
# + subscriberService - service to be invoked via native method
isolated function processSubscriptionDenial(http:Caller caller, http:Response response,
                                            RequestQueryParams params, SubscriberService subscriberService) {
    var reason = params.hubReason is () ? "" : <string>params.hubReason;
    SubscriptionDeniedError subscriptionDeniedMessage = error SubscriptionDeniedError(reason);

    var result = callOnSubscriptionDeniedMethod(subscriberService, subscriptionDeniedMessage);
    
    if (result is ()) {
        result = <Acknowledgement>{
            body: {
                "message": "Subscription Denial Acknowledged"
            }
        };
    } else {
        result = <Acknowledgement>result;
    }

    response.statusCode = http:STATUS_OK;
    updateResponseBody(response, result["body"], result["headers"]);
}

# Porcesses the content distribution requests from `hub`
# 
# + caller - {@code http:Caller} reference
# + request - original HTTP request
# + response - {@code http:Response} to be returned to the caller
# + subscriberService - service to be invoked via native method
# + secretKey - pre-shared client-secret value
isolated function processEventNotification(http:Caller caller, http:Request request, 
                                           http:Response response, SubscriberService subscriberService,
                                           string secretKey) {
    boolean isVerifiedContent = false;
    var payloadType = request.getContentType();

    if (payloadType.includes("multipart")) {
        var payload = request.getBodyParts();
        if (payload is mime:Entity[]) {
            var verificationResponse = verifyContent(request, secretKey, payload);      
            if (verificationResponse is boolean) {
                isVerifiedContent = verificationResponse;
            } else {
                response.statusCode = http:STATUS_INTERNAL_SERVER_ERROR;
                return;
            }
        } else {
            response.statusCode = http:STATUS_INTERNAL_SERVER_ERROR;
            return;
        }
    } else {
        var payload = request.getTextPayload();
        if (payload is string) {
            var verificationResponse = verifyContent(request, secretKey, payload);
            if (verificationResponse is boolean) {
                isVerifiedContent = verificationResponse;
            } else {
                response.statusCode = http:STATUS_INTERNAL_SERVER_ERROR;
                return;
            }
        } else {
            response.statusCode = http:STATUS_INTERNAL_SERVER_ERROR;
            return;
        }
    }

    if (!isVerifiedContent) {
        return;
    }
                                               
    string contentType = request.getContentType();
    map<string|string[]> headers = retrieveRequestHeaders(request);
    ContentDistributionMessage? message = ();

    match contentType {
        "application/json" => {
            message = {
                headers: headers,
                contentType: "application/json",
                content: checkpanic request.getJsonPayload()
            };
        }
        "application/xml" => {
            message = {
                headers: headers,
                contentType: "application/xml",
                content: checkpanic request.getXmlPayload()
            };  
        }
        "text/plain" => {
            message = {
                headers: headers,
                contentType: "text/plain",
                content: checkpanic request.getTextPayload()
            };              
        }
        "application/octet-stream" => {
            message = {
                headers: headers,
                contentType: "application/octet-stream",
                content: checkpanic request.getBinaryPayload()
            };  
        }
        _ => {
            log:printError(string`Unrecognized content-type [${contentType}] found`);
        }
    }

    if (message is ()) {
        response.statusCode = http:STATUS_BAD_REQUEST;
        return;
    } else {
        Acknowledgement | SubscriptionDeletedError? result = callOnEventNotificationMethod(subscriberService, message);
        if (result is Acknowledgement) {
            updateResponseBody(response, result["body"], result["headers"]);
        } else if (result is SubscriptionDeletedError) {
            response.statusCode = http:STATUS_GONE;
            var errorDetails = result.detail();
            updateResponseBody(response, errorDetails["body"], errorDetails["headers"], result.message());
        }
        return;
    }
}

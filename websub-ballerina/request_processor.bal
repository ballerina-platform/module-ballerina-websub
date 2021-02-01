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

isolated function processSubscriptionVerification(http:Caller caller,
                                                  http:Response response,
                                                  RequestQueryParams params, 
                                                  SubscriberService subscriberService) {
    SubscriptionVerification message = {
        hubMode: params.hubMode,
        hubTopic: params.hubTopic,
        hubChallenge: params.hubChallenge,
        hubLeaseSeconds: params.hubLeaseSeconds
    };

    SubscriptionVerificationSuccess | SubscriptionVerificationError result = callOnSubscriptionVerificationMethod(subscriberService, message);

    if (result is SubscriptionVerificationError) {
        result = <SubscriptionVerificationError>result;
        response.statusCode = http:STATUS_NOT_FOUND;
        string errorMessage = result.message();
        response.setTextPayload(errorMessage);
        // respondToRequest(caller, response);
    } else {
        response.statusCode = http:STATUS_OK;
        response.setTextPayload(params.hubChallenge);
        // respondToRequest(caller, response);
    }
}

isolated function processSubscriptionDenial(http:Caller caller,
                                            http:Response response,
                                            RequestQueryParams params, 
                                            SubscriberService subscriberService) {
    string reason = params.hubReason.trim().length() == 0 ? "Hub has denied the susbcription" : params.hubReason;
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
    // respondToRequest(caller, response);
}

isolated function processEventNotification(http:Caller caller, 
                                           http:Request request, 
                                           http:Response response, 
                                           SubscriberService subscriberService,
                                           string secretKey) {
    var payload = request.getTextPayload();

    boolean isVerifiedContent = false;
    if (payload is string) {
        isVerifiedContent = verifyContent(request, secretKey, payload);
    } else {
        response.statusCode = http:STATUS_INTERNAL_SERVER_ERROR;
        return;
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
        _ => {}
    }

    if (message is ()) {
        response.statusCode = http:STATUS_BAD_REQUEST;
        return;
    } else {
        Acknowledgement | SubscriptionDeletedError? result = callOnEventNotificationMethod(
                                                                    subscriberService, message);
        if (result is Acknowledgement) {
            updateResponseBody(response, result["body"], result["headers"]);
            return;
        } else if (result is SubscriptionDeletedError) {
            response.statusCode = http:STATUS_GONE;
            return;
        }
    }

    // respondToRequest(caller, response);
}
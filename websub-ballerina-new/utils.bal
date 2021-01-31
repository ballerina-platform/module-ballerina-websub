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
        respondToRequest(caller, response);
    } else {
        response.statusCode = http:STATUS_OK;
        response.setTextPayload(params.hubChallenge);
        respondToRequest(caller, response);
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
    respondToRequest(caller, response);
}

isolated function updateResponseBody(http:Response response, anydata? messageBody, map<string|string[]>? headers) {
    string payload = "";
    if (messageBody is map<string>) {
        foreach var ['key, value] in messageBody.entries() {
            payload = payload + "&" + 'key + "=" + value;
        }
    }
    response.setTextPayload(payload);
    response.setHeader("Content-type","application/x-www-form-urlencoded");
    if (headers is map<string|string[]>) {
        foreach var [header, value] in headers.entries() {
            if (value is string) {
                response.setHeader(header, value);
            } else {
                foreach var valueElement in value {
                    response.addHeader(header, valueElement);
                }
            }
        }
    }
}

isolated function retrieveRequestQueryParams(http:Request request) returns RequestQueryParams {
    map<string[]> queryParams = request.getQueryParams();

    string[] hubModeValues = queryParams.get(HUB_MODE);
    string hubMode = hubModeValues.length() == 1 ? hubModeValues[0] : "";

    string[] hubTopicValues = queryParams.get(HUB_TOPIC);
    string hubTopic = hubTopicValues.length() == 1 ? hubTopicValues[0] : "";

    string[] hubChallengeValues = queryParams.get(HUB_CHALLENGE);
    string hubChallenge = hubChallengeValues.length() == 1 ? hubChallengeValues[0] : "";

    string[] hubLeaseSecondsValues =  queryParams.get(HUB_LEASE_SECONDS);
    string? hubLeaseSeconds = hubLeaseSecondsValues.length() == 1 ? hubLeaseSecondsValues[0] : ();

    string[] hubReasonValues = queryParams.get(HUB_REASON);
    string hubReason = hubReasonValues.length() == 1 ? hubReasonValues[0] : "";

    RequestQueryParams params = {
        hubMode: hubMode,
        hubTopic: hubTopic,
        hubChallenge: hubChallenge,
        hubLeaseSeconds: hubLeaseSeconds,
        hubReason: hubReason
    };

    return params;
}

isolated function respondToRequest(http:Caller caller, http:Response response) {
    var responseError = caller->respond(response);
}
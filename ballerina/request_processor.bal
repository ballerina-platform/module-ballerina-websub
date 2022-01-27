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
import ballerina/mime;
import ballerina/log;
import ballerina/url;

isolated function processSubscriptionVerification(http:Caller caller, http:Response response, 
                                                  RequestQueryParams params, HttpToWebsubAdaptor adaptor) {
    SubscriptionVerification message = {
        hubMode: <string>params?.hubMode,
        hubTopic: <string>params?.hubTopic,
        hubChallenge: <string>params?.hubChallenge,
        hubLeaseSeconds: params?.hubLeaseSeconds
    };

    SubscriptionVerificationSuccess|error result = adaptor.callOnSubscriptionVerificationMethod(message);
    if result is SubscriptionVerificationError {
        response.statusCode = http:STATUS_NOT_FOUND;
        var errorDetails = result.detail();
        updateResponseBody(response, errorDetails["body"], errorDetails["headers"], result.message());
    } else if result is error {
        response.statusCode = http:STATUS_NOT_FOUND;
        updateResponseBody(response, (), (), result.message());
    } else {
        response.statusCode = http:STATUS_OK;
        response.setTextPayload(<string>params?.hubChallenge);
    }
}

isolated function processUnsubscriptionVerification(http:Caller caller, http:Response response, 
                                                    RequestQueryParams params, HttpToWebsubAdaptor adaptor) {
    UnsubscriptionVerification message = {
        hubMode: <string>params?.hubMode,
        hubTopic: <string>params?.hubTopic,
        hubChallenge: <string>params?.hubChallenge,
        hubLeaseSeconds: params?.hubLeaseSeconds
    };

    UnsubscriptionVerificationSuccess|error result = adaptor.callOnUnsubscriptionVerificationMethod(message);
    if result is UnsubscriptionVerificationError {
        response.statusCode = http:STATUS_NOT_FOUND;
        var errorDetails = result.detail();
        updateResponseBody(response, errorDetails["body"], errorDetails["headers"], result.message());
    } else if result is error {
        response.statusCode = http:STATUS_NOT_FOUND;
        updateResponseBody(response, (), (), result.message());
    } else {
        response.statusCode = http:STATUS_OK;
        response.setTextPayload(<string>params?.hubChallenge);
    }
}

isolated function processSubscriptionDenial(http:Caller caller, http:Response response,
                                            RequestQueryParams params, HttpToWebsubAdaptor adaptor) {
    var reason = params?.hubReason is () ? "" : <string>params?.hubReason;
    SubscriptionDeniedError subscriptionDeniedMessage = error SubscriptionDeniedError(reason);
    Acknowledgement|error? result = adaptor.callOnSubscriptionDeniedMethod(subscriptionDeniedMessage);
    response.statusCode = http:STATUS_OK;
    if result is () || result is error {
        updateResponseBody(response, ACKNOWLEDGEMENT["body"], ACKNOWLEDGEMENT["headers"]);
    } else {
        updateResponseBody(response, result["body"], result["headers"]);
    }
}

isolated function processEventNotification(http:Caller caller, http:Request request, 
                                           http:Response response, HttpToWebsubAdaptor adaptor,
                                           string secretKey) returns error? {
    string payload = check request.getTextPayload();
    boolean isVerifiedContent = check verifyContent(request, secretKey, payload);
    if !isVerifiedContent {
        return;
    }
                                               
    string contentType = request.getContentType();
    map<string|string[]> headers = retrieveRequestHeaders(request);
    ContentDistributionMessage? message = ();

    match contentType {
        mime:APPLICATION_JSON => {
            message = {
                headers: headers,
                contentType: contentType,
                content: check request.getJsonPayload()
            };
        }
        mime:APPLICATION_XML => {
            message = {
                headers: headers,
                contentType: contentType,
                content: check request.getXmlPayload()
            };  
        }
        mime:TEXT_PLAIN => {
            message = {
                headers: headers,
                contentType: contentType,
                content: check request.getTextPayload()
            };              
        }
        mime:APPLICATION_OCTET_STREAM => {
            message = {
                headers: headers,
                contentType: contentType,
                content: check request.getBinaryPayload()
            };  
        }
        mime:APPLICATION_FORM_URLENCODED => {
            map<string> formContent = check request.getFormParams();
            map<string> decodedContent = {};
            foreach var ['key, value] in formContent.entries() {
                decodedContent['key] = check url:decode(value, "UTF-8");
            }
            message = {
                headers: headers,
                contentType: contentType,
                content: decodedContent
            }; 
        }
        _ => {
            log:printError(string `Unrecognized content-type [${contentType}] found`);
        }
    }

    if message is () {
        response.statusCode = http:STATUS_BAD_REQUEST;
        return;
    } else {
        Acknowledgement|error? result = adaptor.callOnEventNotificationMethod(message, request);
        if result is Acknowledgement {
            updateResponseBody(response, result["body"], result["headers"]);
        } else if result is SubscriptionDeletedError {
            response.statusCode = http:STATUS_GONE;
            var errorDetails = result.detail();
            updateResponseBody(response, errorDetails["body"], errorDetails["headers"], result.message());
        } else {
            updateResponseBody(response, ACKNOWLEDGEMENT["body"], ACKNOWLEDGEMENT["headers"]); 
        }
        return;
    }
}

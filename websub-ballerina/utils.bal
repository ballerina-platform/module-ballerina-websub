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
import ballerina/regex;
import ballerina/crypto;
import ballerina/log;
import ballerina/lang.'string as strings;
import ballerina/random;

# Generates a random-string of given length
# 
# + length - required length of the generated string
# + return - generated random string value or `error` if any error occurred in the execution
isolated function generateRandomString(int length) returns string | error {
    int[] codePoints = [];
    int leftLimit = 48; // numeral '0'
    int rightLimit = 122; // letter 'z'
    int iterator = 0;
    while (iterator < length) {
        int randomInt = check random:createIntInRange(leftLimit, rightLimit);
        // character literals from 48 - 57 are numbers | 65 - 90 are capital letters | 97 - 122 are simple letters
        if (randomInt <= 57 || randomInt >= 65) && (randomInt <= 90 || randomInt >= 97) {
            codePoints.push(randomInt);
            iterator += 1;
        }
    }
    return strings:fromCodePointInts(codePoints);
}

# Generate the `websub:SubscriptionChangeRequest` from the configurations.
# 
# + topicUrl - `topic` to which subscriber want to subscribe
# + callback - Subscriber callback URL to be used by the `hub`
# + config - user defined subscriber-service configurations
# + return - {@code websub:SubscriptionChangeRequest} from the configurations provided
isolated function retrieveSubscriptionRequest(string topicUrl, string callback, 
                                              SubscriberServiceConfiguration config) returns SubscriptionChangeRequest {        
    SubscriptionChangeRequest request = { topic: topicUrl, callback: callback };
        
    var leaseSeconds = config?.leaseSeconds;
    if (leaseSeconds is int) {
        request.leaseSeconds = leaseSeconds;
    }

    var secret = config?.secret;
    if (secret is string) {
        request.secret = secret;
    }

    return request;
}

# Retrieve the request-headers from the `http:Request`.
# 
# + request - {@code http:Request} to be processed
# + return - {@code map<string|string[]>} containing the header values
isolated function retrieveRequestHeaders(http:Request request) returns map<string|string[]> {
    string[] headerNames = request.getHeaderNames();
    map<string|string[]> headers = {};

    foreach var headerName in headerNames {
        http:HeaderNotFoundError | string[] headerValue = request.getHeaders(headerName);
        if (headerValue is string[]) {
            headers[headerName] = headerValue;
        }
    }

    return headers;
}

# Retrieve request query parameters.
# 
# + request - {@code http:Request} to be processed
# + return - {@code websub:RequestQueryParams} containing the query parameter values
isolated function retrieveRequestQueryParams(http:Request request) returns RequestQueryParams {
    map<string[]> queryParams = request.getQueryParams();

    string? hubMode = request.getQueryParamValue(HUB_MODE);
    string? hubTopic = request.getQueryParamValue(HUB_TOPIC);
    string? hubChallenge = request.getQueryParamValue(HUB_CHALLENGE);
    string? hubLeaseSeconds = request.getQueryParamValue(HUB_LEASE_SECONDS);
    string? hubReason = request.getQueryParamValue(HUB_REASON);

    if (hubMode is string) {
        if (hubTopic is string && hubChallenge is string) {
            return {
                hubMode: hubMode,
                hubTopic: hubTopic,
                hubChallenge: hubChallenge,
                hubLeaseSeconds: hubLeaseSeconds
            };
        } else if (hubReason is string) {
            return {
                hubMode: hubMode,
                hubReason: hubReason
            };
        } else {
            return {};
        }
    } else {
        return {};
    }
}

# Verifies the `http:Request` payload with the provided signature value.
# 
# + request - current {@code http:Request}
# + secret - pre-shared client-secret value
# + payload - {@code string} value of the request body
# + return - `true` if the verification is successfull, else `false`
isolated function verifyContent(http:Request request, string secret, string payload) returns boolean|error {
    if (secret.trim().length() > 0) {
        if (request.hasHeader(X_HUB_SIGNATURE)) {
                var xHubSignature = request.getHeader(X_HUB_SIGNATURE);
                
                if (xHubSignature is http:HeaderNotFoundError || xHubSignature.trim().length() == 0) {
                    return false;
                } else {
                    string[] splitSignature = regex:split(<string>xHubSignature, "=");
                    string method = splitSignature[0];
                    string signature = regex:replaceAll(<string>xHubSignature, method + "=", "");
                    byte[] generatedSignature = check retrieveContentHash(method, secret, payload);
                    return signature == generatedSignature.toBase16(); 
                }          
        } else {
            return false;
        }
    } else {
        return true;
    }
}

# Generates HMac value for the paload depending on the provided algorithm.
# 
# + method - `HMac` algorithm to be used
# + key - pre-shared secret-key value
# + payload - content to be hashed
# + return - {@code byte[]} representing the `hMac`
isolated function retrieveContentHash(string method, string key, string payload) returns byte[]|error {
    byte[] keyArr = key.toBytes();
    byte[] contentPayload = payload.toBytes();
    byte[] hashedContent = [];

    match method {
        SHA1 => {
            return crypto:hmacSha1(contentPayload, keyArr);
        }
        SHA_256 => {
            return crypto:hmacSha256(contentPayload, keyArr);
        }
        SHA_384 => {
            return crypto:hmacSha384(contentPayload, keyArr);
        }
        SHA_512 => {
            return crypto:hmacSha512(contentPayload, keyArr);
        }
        _ => {
            string errorMsg = string`Unrecognized hashning-method [${method}] found`;
            log:printError(errorMsg);
            return error Error(errorMsg);
        }
    }
}

# Updates `http:Response` body with provided parameters.
# 
# + response - {@code http:Response} to be updated
# + messageBody - content for the response body
# + headers - additional header-parameters to included in the response
# + reason - reason for action execution failure / success
isolated function updateResponseBody(http:Response response, anydata? messageBody, 
                                     map<string|string[]>? headers, string? reason = ()) {
    string payload = reason is () ? "" : "reason=" + reason;
    if (messageBody is map<string> && messageBody.length() > 0) {
        string[] messageParams = [];
        payload += "&";
        foreach var ['key, value] in messageBody.entries() {
            messageParams.push('key + "=" + value);
        }
        payload += strings:'join("&", ...messageParams);
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

# Respond to the received `http:Request`.
# 
# + caller - {@code http:Caller} which intiate the request
# + response - {@code http:Response} to be sent to the caller
isolated function respondToRequest(http:Caller caller, http:Response response) {
    http:ListenerError? responseError = caller->respond(response);
}

# Checks whether response is successfull 
# 
# + statusCode - statusCode found in the {@code http:Response}
# + return - `true` if the `statusCode` is between 200 to 300, else false
isolated function isSuccessStatusCode(int statusCode) returns boolean {
    return (200 <= statusCode && statusCode < 300);
}
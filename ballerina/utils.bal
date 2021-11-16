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

# Generates the `websub:SubscriptionChangeRequest` from the configurations.
# ```ballerina
# websub:SubscriptionChangeRequest subscriptionRequest = retrieveSubscriptionRequest("https://sample.topic.com", "https://sample.callback/subscriber", config);
# ```
# 
# + topicUrl - The `topic` to which the subscriber wants to subscribe
# + config - User-defined subscriber service configurations
# + callback - Callback URL for subscription/unsubscription verification
# + return - Generated `websub:SubscriptionChangeRequest` from the provided configurations
isolated function retrieveSubscriptionRequest(string topicUrl, SubscriberServiceConfiguration config, string callback) returns SubscriptionChangeRequest {        
    SubscriptionChangeRequest request = { topic: topicUrl, callback: callback };
        
    int? leaseSeconds = config?.leaseSeconds;
    if leaseSeconds is int {
        request.leaseSeconds = leaseSeconds;
    }

    string? secret = config?.secret;
    if secret is string {
        request.secret = secret;
    }

    return request;
}

# Retrieves the request-headers from the `http:Request`.
# ```ballerina
# map<string|string[]> availableHeaders = retrieveRequestHeaders(httpRequest);
# ```
# 
# + request - Original `http:Request` object
# + return - Header values found in the provided `http:Request`
isolated function retrieveRequestHeaders(http:Request request) returns map<string|string[]> {
    string[] headerNames = request.getHeaderNames();
    map<string|string[]> headers = {};

    foreach var headerName in headerNames {
        http:HeaderNotFoundError | string[] headerValue = request.getHeaders(headerName);
        if headerValue is string[] {
            headers[headerName] = headerValue;
        }
    }

    return headers;
}

# Retrieves the request query parameters.
# ```ballerina
# websub:RequestQueryParams queryParams = retrieveRequestQueryParams(httpRequest);
# ```
# 
# + request - Original `http:Request` object
# + return - The `websub:RequestQueryParams` instance containing the query parameter values
isolated function retrieveRequestQueryParams(http:Request request) returns RequestQueryParams {
    string? hubMode = request.getQueryParamValue(HUB_MODE);
    string? hubTopic = request.getQueryParamValue(HUB_TOPIC);
    string? hubChallenge = request.getQueryParamValue(HUB_CHALLENGE);
    string? hubLeaseSeconds = request.getQueryParamValue(HUB_LEASE_SECONDS);
    string? hubReason = request.getQueryParamValue(HUB_REASON);

    if hubMode is string {
        if hubTopic is string && hubChallenge is string {
            return {
                hubMode: hubMode,
                hubTopic: hubTopic,
                hubChallenge: hubChallenge,
                hubLeaseSeconds: hubLeaseSeconds
            };
        } else if hubReason is string {
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
# ```ballerina
# boolean isVerified = check verifyContent(httpRequest, secretKey, requestPayload);
# ```
# 
# + request - Original `http:Request` object
# + secret - Pre-shared subscriber secret key
# + payload - Request payload
# + return - `true` if the verification is successful or else `false`
isolated function verifyContent(http:Request request, string secret, string payload) returns boolean|error {
    if secret.trim().length() > 0 {
        if request.hasHeader(X_HUB_SIGNATURE) {
                string xHubSignature = check request.getHeader(X_HUB_SIGNATURE);
                if xHubSignature.trim().length() == 0 {
                    return false;
                } else {
                    string[] splitSignature = regex:split(xHubSignature, "=");
                    string method = splitSignature[0];
                    string signature = regex:replaceAll(xHubSignature, method + "=", "");
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

# Generates the HMAC value for the payload depending on the provided algorithm.
# ```ballerina
# byte[] hMacSignature = check retrieveContentHash("SHA1", secretKey, requestPayload);
# ```
# 
# + method - `HMAC` algorithm to be used
# + key - Pre-shared subscriber secret key
# + payload - Request payload to be hashed
# + return - Calculated HMAC value if successfull or else an `error`
isolated function retrieveContentHash(string method, string key, string payload) returns byte[]|error {
    byte[] keyArr = key.toBytes();
    byte[] contentPayload = payload.toBytes();
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
            string errorMsg = string `Unrecognized hashning-method [${method}] found`;
            log:printError(errorMsg);
            return error Error(errorMsg);
        }
    }
}

# Updates the `http:Response` body with the provided additional parameters.
# ```ballerina
# updateResponseBody(httpResponse, messageBody, additionalHeaders);
# ```
# 
# + response - Original `http:Response` object
# + messageBody - Content for the response body
# + headers - Additional header parameters to be included in the response
# + reason - Optional reason parameter for the action execution failure
isolated function updateResponseBody(http:Response response, anydata? messageBody, 
                                     map<string|string[]>? headers, string? reason = ()) {
    string payload = reason is () ? "" : "reason=" + reason;
    if messageBody is map<string> && messageBody.length() > 0 {
        string[] messageParams = [];
        payload += "&";
        foreach var ['key, value] in messageBody.entries() {
            messageParams.push('key + "=" + value);
        }
        payload += strings:'join("&", ...messageParams);
    }

    response.setTextPayload(payload);
    response.setHeader("Content-type","application/x-www-form-urlencoded");
    if headers is map<string|string[]> {
        foreach var [header, value] in headers.entries() {
            if value is string {
                response.setHeader(header, value);
            } else {
                foreach var valueElement in value {
                    response.addHeader(header, valueElement);
                }
            }
        }
    }
}

# Responds to the received `http:Request`.
# ```ballerina
# respondToRequest(httpCaller, httpResponse);
# ```
# 
# + caller - The `http:Caller` reference for the current request
# + response - Updated `http:Response`
# + return - An `websub:Error` if there is an error while responding to the request or else `()`
isolated function respondToRequest(http:Caller caller, http:Response response) returns Error? {
    http:ListenerError? responseError = caller->respond(response);
    if responseError is http:ListenerError {
        return error Error("Error occurred while responding to the request ", responseError);
    }
}

# Checks whether the response is successful.
# ```ballerina
# boolean isSuccessfull = isSuccessStatusCode(404);
# ```
# 
# + statusCode - Received HTTP status code
# + return - `true` if the `statusCode` is between 200 to 300 or else `false`
isolated function isSuccessStatusCode(int statusCode) returns boolean {
    return (200 <= statusCode && statusCode < 300);
}

isolated function retrieveHttpClient(string url, http:ClientConfiguration? config) returns http:Client|Error {
    if config is http:ClientConfiguration {
        http:Client|error clientEp = new (url, config);
        if (clientEp is http:Client) {
            return clientEp;
        } else {
            return error Error("Client initialization failed", clientEp);
        }
    } else {
        http:Client|error clientEp = new (url);
        if (clientEp is http:Client) {
            return clientEp;
        } else {
            return error Error("Client initialization failed", clientEp);
        }
    }
}

# Returns the value of the specified header. If the specified header key maps to multiple values, the first of
# these values is returned.
#
# + msg - Current `websub:ContentDistributionMessage` object
# + headerName - The header name
# + return - The first header value for the specified header name or the `http:HeaderNotFoundError` if the header is not
#            found.
public isolated function getHeader(ContentDistributionMessage msg, string headerName) returns string|http:HeaderNotFoundError {
    http:Request originalRequest = retrieveHttpRequest(msg);
    return originalRequest.getHeader(headerName);
}

# Gets all the header values to which the specified header key maps to.
#
# + msg - Current `websub:ContentDistributionMessage` object
# + headerName - The header name
# + return - The header values the specified header key maps to or the `http:HeaderNotFoundError` if the header is not
#            found.
public isolated function getHeaders(ContentDistributionMessage msg, string headerName) returns string[]|http:HeaderNotFoundError {
    http:Request originalRequest = retrieveHttpRequest(msg);
    return originalRequest.getHeaders(headerName);
}

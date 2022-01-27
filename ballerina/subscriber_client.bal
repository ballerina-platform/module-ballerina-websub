// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/url;
import ballerina/http;
import ballerina/mime;

# The HTTP based client for WebSub subscription and unsubscription.
public isolated client class SubscriptionClient {
    private final string url;
    private final http:Client httpClient;

    # Initializes the `websub:SubscriptionClient` instance.
    # ```ballerina
    # websub:SubscriptionClient subscriptionClientEp = check new ("https://sample.hub.com");
    # ```
    # 
    # + url    - The URL at which the subscription should be changed
    # + config - Optional `http:ClientConfiguration` for the underlying client
    # + return - The `websub:SubscriptionClient` or an `websub:Error` if the initialization failed
    public isolated function init(string url, *http:ClientConfiguration config) returns Error? {
        self.url = url;
        self.httpClient = check retrieveHttpClient(self.url, config);
    }

    # Sends a subscription request to the provided `hub`.
    # ```ballerina
    # websub:SubscriptionChangeResponse response = check subscriberClientEp->subscribe(subscriptionRequest);
    # ```
    #
    # + subscriptionRequest - The request payload containing the subscription details
    # + return - The `websub:SubscriptionChangeResponse` indicating that the subscription initiation was successful
    #            or else an `websub:SubscriptionInitiationError`
    isolated remote function subscribe(SubscriptionChangeRequest subscriptionRequest)
            returns SubscriptionChangeResponse|SubscriptionInitiationError {
        http:Client httpClient = self.httpClient;
        http:Request builtSubscriptionRequest = buildSubscriptionChangeRequest(MODE_SUBSCRIBE, subscriptionRequest);
        http:Response|error response = httpClient->post("", builtSubscriptionRequest);
        return processHubResponse(self.url, MODE_SUBSCRIBE, subscriptionRequest, response);
    }

    # Sends an unsubscription request to a WebSub Hub.
    # ```ballerina
    # websub:SubscriptionChangeResponse response = check subscriberClientEp->unsubscribe(subscriptionRequest);
    # ```
    # + unsubscriptionRequest - The request payload containing the unsubscription details
    # + return - The `websub:SubscriptionChangeResponse` indicating that the unsubscription initiation was successful
    #            or else an `websub:SubscriptionInitiationError`
    isolated remote function unsubscribe(SubscriptionChangeRequest unsubscriptionRequest)
            returns SubscriptionChangeResponse|SubscriptionInitiationError {
        http:Client httpClient = self.httpClient;
        http:Request builtUnsubscriptionRequest = buildSubscriptionChangeRequest(MODE_UNSUBSCRIBE, unsubscriptionRequest);
        http:Response|error response = httpClient->post("", builtUnsubscriptionRequest);
        return processHubResponse(self.url, MODE_UNSUBSCRIBE, unsubscriptionRequest, response);
    }

}

isolated function buildSubscriptionChangeRequest(string mode, 
                                                 SubscriptionChangeRequest subscriptionChangeRequest) 
                                                 returns http:Request {
    http:Request request = new;
    string callback = subscriptionChangeRequest.callback;
    var encodedCallback = url:encode(callback, "UTF-8");
    if encodedCallback is string {
        callback = encodedCallback;
    }

    string body = HUB_MODE + "=" + mode
        + "&" + HUB_TOPIC + "=" + subscriptionChangeRequest.topic
        + "&" + HUB_CALLBACK + "=" + callback;
    if mode == MODE_SUBSCRIBE {
        if subscriptionChangeRequest.secret.trim() != "" {
            body = body + "&" + HUB_SECRET + "=" + subscriptionChangeRequest.secret;
        }
        if subscriptionChangeRequest.leaseSeconds != 0 {
            body = body + "&" + HUB_LEASE_SECONDS + "=" + subscriptionChangeRequest.leaseSeconds.toString();
        }
    }
    request.setTextPayload(body);
    request.setHeader(CONTENT_TYPE, mime:APPLICATION_FORM_URLENCODED);
    return request;
}

isolated function processHubResponse(string hub, string mode, 
                                     SubscriptionChangeRequest subscriptionChangeRequest,
                                     http:Response|http:PayloadType|error response) returns SubscriptionChangeResponse|SubscriptionInitiationError {

    string topic = subscriptionChangeRequest.topic;
    if response is error {
        return error SubscriptionInitiationError("Error occurred for request: Mode[" + mode+ "] at Hub[" + hub + "] - " + response.message());
    } else {
        http:Response hubResponse = <http:Response> response;
        int responseStatusCode = hubResponse.statusCode;
        if responseStatusCode == http:STATUS_TEMPORARY_REDIRECT
                || responseStatusCode == http:STATUS_PERMANENT_REDIRECT {
            return error SubscriptionInitiationError("Redirection response received for subscription change request made with " +
                               "followRedirects disabled or after maxCount exceeded: Hub [" + hub + "], Topic [" +
                               subscriptionChangeRequest.topic + "]");
        } else if !isSuccessStatusCode(responseStatusCode) {
            var responsePayload = hubResponse.getTextPayload();
            string errorMessage = "Error in request: Mode[" + mode + "] at Hub[" + hub + "]";
            if responsePayload is string {
                errorMessage = errorMessage + " - " + responsePayload;
            } else {
                errorMessage = errorMessage + " - Error occurred identifying cause: " + responsePayload.message();
            }
            return error SubscriptionInitiationError(errorMessage);
        } else {
            SubscriptionChangeResponse subscriptionChangeResponse = {hub:hub, topic:topic, response:hubResponse};
            return subscriptionChangeResponse;
        }
    }
}

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
import ballerina/log;
import ballerina/mime;

# The HTTP based client for WebSub subscription and unsubscription.
public client class SubscriptionClient {

    private string url;
    private http:Client httpClient;
    private http:FollowRedirects? followRedirects = ();

    # Initializes the `websub:SubscriptionClient` instance.
    # ```ballerina
    # websub:SubscriptionClient subscriptionClientEp = check new ("https://sample.hub.com");
    # ```
    # 
    # + url    - The URL at which the subscription should be changed
    # + config - Optional `http:ClientConfiguration` for the underlying client
    public isolated function init(string url, *http:ClientConfiguration config) returns error? {
        self.url = url;
        self.httpClient = check new (self.url, config);
        self.followRedirects = config?.followRedirects;
    }

    # Sends a subscription request to the provided `hub`.
    # ```ballerina
    # websub:SubscriptionChangeResponse response = check websubHubClientEP->subscribe(subscriptionRequest);
    # ```
    #
    # + subscriptionRequest - The request payload containing subscription details
    # + return - The `websub:SubscriptionChangeResponse` indicating subscription initiation was successful
    #            or else an `error`
    isolated remote function subscribe(SubscriptionChangeRequest subscriptionRequest)
        returns @tainted SubscriptionChangeResponse|error {

        http:Client httpClient = self.httpClient;
        http:Request builtSubscriptionRequest = buildSubscriptionChangeRequest(MODE_SUBSCRIBE, subscriptionRequest);
        var response = httpClient->post("", builtSubscriptionRequest);
        int redirectCount = getRedirectionMaxCount(self.followRedirects);
        return processHubResponse(self.url, MODE_SUBSCRIBE, subscriptionRequest, response, httpClient,
                                  redirectCount);
    }

    # Sends an unsubscription request to a WebSub Hub.
    # ```ballerina
    # websub:SubscriptionChangeResponse response = check websubHubClientEP->unsubscribe(subscriptionRequest);
    # ```
    # + unsubscriptionRequest - The request payload containing unsubscription details
    # + return - The `websub:SubscriptionChangeResponse` indicating unsubscription initiation was successful
    #            or else an `error`
    isolated remote function unsubscribe(SubscriptionChangeRequest unsubscriptionRequest)
        returns @tainted SubscriptionChangeResponse|error {

        http:Client httpClient = self.httpClient;
        http:Request builtUnsubscriptionRequest = buildSubscriptionChangeRequest(MODE_UNSUBSCRIBE, unsubscriptionRequest);
        var response = httpClient->post("", builtUnsubscriptionRequest);
        int redirectCount = getRedirectionMaxCount(self.followRedirects);
        return processHubResponse(self.url, MODE_UNSUBSCRIBE, unsubscriptionRequest, response, httpClient,
                                  redirectCount);
    }

}

# Generates `http:Request` with provided `websub:subscriptionChangeRequest`.
# ```ballerina
# http:Request subscriptionRequest = buildSubscriptionChangeRequest("subscribe", { 
#           topic: "https://sample.topic.com", 
#           callback: "https://sample.subscriber.com" 
# });
# ```
# 
# + mode - Mode of subscription (subscribe/unsubscribe)
# + subscriptionChangeRequest - The request payload containing subscription/unsubscription details
# + return - A `http:Request` to be sent to the hub to subscribe/unsubscribe
isolated function buildSubscriptionChangeRequest(@untainted string mode, 
                                                 SubscriptionChangeRequest subscriptionChangeRequest) 
                                                returns (http:Request) {
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

# Processes the response received from the `hub`.
# ```ballerina
# websub:SubscriptionChangeResponse subscriptionResponse = check processHubResponse("https://sample.hub.com", "subscribe", { 
#           topic: "https://sample.topic.com", 
#           callback: "https://sample.subscriber.com" 
# }, httpResponse, httpClientEp, 2);
# ```
# 
# + hub - The `hub` to which the subscription/unsubscription request was sent
# + mode - Mode of subscription (subscribe/unsubscribe)
# + subscriptionChangeRequest - The request containing the subscription/unsubscription details
# + response - Original response received from the `hub` as `http:Response`,`http:PayloadType', or an `error`
# + httpClient - The underlying `http:Client` endpoint
# + remainingRedirects - Available redirects for the current subscription
# + return - `websub:SubscriptionChangeResponse` if the requested subscription action is successfull or else `error`
isolated function processHubResponse(@untainted string hub, @untainted string mode, 
                                     SubscriptionChangeRequest subscriptionChangeRequest,
                                     http:Response|http:PayloadType|error response, http:Client httpClient, 
                                     int remainingRedirects) returns @tainted SubscriptionChangeResponse|error {

    string topic = subscriptionChangeRequest.topic;
    if response is error {
        return error SubscriptionInitiationFailedError("Error occurred for request: Mode[" + mode+ "] at Hub[" + hub + "] - " + response.message());
    } else {
        http:Response hubResponse = <http:Response> response;
        int responseStatusCode = hubResponse.statusCode;
        if responseStatusCode == http:STATUS_TEMPORARY_REDIRECT
                || responseStatusCode == http:STATUS_PERMANENT_REDIRECT {
            if remainingRedirects > 0 {
                string redirected_hub = check hubResponse.getHeader("Location");
                return invokeClientConnectorOnRedirection(redirected_hub, mode, subscriptionChangeRequest,
                                                            httpClient.config.auth, remainingRedirects - 1);
            }
            return error SubscriptionInitiationFailedError("Redirection response received for subscription change request made with " +
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
            return error SubscriptionInitiationFailedError(errorMessage);
        } else {
            if responseStatusCode != http:STATUS_ACCEPTED {
                log:printWarn(string`Subscription request considered successful for non 202 status code: ${responseStatusCode.toString()}`);
            }
            SubscriptionChangeResponse subscriptionChangeResponse = {hub:hub, topic:topic, response:hubResponse};
            return subscriptionChangeResponse;
        }
    }
}

# Handles subscription redirections.
# ```ballerina
# websub:SubscriptionChangeResponse subscriptionResponse = check invokeClientConnectorOnRedirection("https://sample.hub.com", "subscribe", { 
#           topic: "https://sample.topic.com", 
#           callback: "https://sample.subscriber.com" 
# }, {}, 2);
# ```
#
# + hub - The `hub` to which the subscription/unsubscription request was sent
# + mode - Mode of subscription (subscribe/unsubscribe)
# + subscriptionChangeRequest - The request containing the subscription/unsubscription details
# + auth - The auth config to use at the `hub` (if specified)
# + remainingRedirects - Available redirects for the current subscription
# + return - `websub:SubscriptionChangeResponse` if the request was successful or else an `error`
isolated function invokeClientConnectorOnRedirection(@untainted string hub, @untainted string mode, 
                                                     SubscriptionChangeRequest subscriptionChangeRequest, 
                                                     http:ClientAuthConfig? auth, int remainingRedirects)
    returns @tainted SubscriptionChangeResponse|error {

    if mode == MODE_SUBSCRIBE {
        return subscribeWithRetries(hub, subscriptionChangeRequest, auth, remainingRedirects = remainingRedirects);
    }
    return unsubscribeWithRetries(hub, subscriptionChangeRequest, auth, remainingRedirects = remainingRedirects);
}

# Sends subscription request with retries.
# ```ballerina
# websub:SubscriptionChangeResponse subscriptionResponse = subscribeWithRetries("https://sample.hub.com", { 
#           topic: "https://sample.topic.com", 
#           callback: "https://sample.subscriber.com" 
# }, {}, 2);
# ```
# 
# + url - The `hub` URL to which the subscription request should send
# + subscriptionRequest - The request containing the subscription details
# + auth - The auth config to use at the `hub` (if specified)
# + remainingRedirects - Available redirects for the current subscription
# + return - `websub:SubscriptionChangeResponse` if the request was successful or else an `error`
isolated function subscribeWithRetries(string url, SubscriptionChangeRequest subscriptionRequest,
                                       http:ClientAuthConfig? auth, int remainingRedirects = 0)
             returns @tainted SubscriptionChangeResponse| error {
    http:Client clientEndpoint = check new http:Client(url, { auth: auth });
    http:Request builtSubscriptionRequest = buildSubscriptionChangeRequest(MODE_SUBSCRIBE, subscriptionRequest);
    var response = clientEndpoint->post("", builtSubscriptionRequest);
    return processHubResponse(url, MODE_SUBSCRIBE, subscriptionRequest, response, clientEndpoint,
                              remainingRedirects);
}

# Sends unsubscription request with retries.
# ```ballerina
# websub:SubscriptionChangeResponse subscriptionResponse = unsubscribeWithRetries("https://sample.hub.com", { 
#           topic: "https://sample.topic.com", 
#           callback: "https://sample.subscriber.com" 
# }, {}, 2);
# ```
# 
# + url - The `hub` URL to which the subscription request should send
# + unsubscriptionRequest - The request containing the unsubscription details
# + auth - The auth config to use at the `hub` (if specified)
# + remainingRedirects - Available redirects for the current subscription
# + return - `websub:SubscriptionChangeResponse` if the request was successful or else an `error`
isolated function unsubscribeWithRetries(string url, SubscriptionChangeRequest unsubscriptionRequest,
                                         http:ClientAuthConfig? auth, int remainingRedirects = 0)
             returns @tainted SubscriptionChangeResponse|error {
    http:Client clientEndpoint = check new http:Client(url, {
        auth: auth
    });
    http:Request builtSubscriptionRequest = buildSubscriptionChangeRequest(MODE_UNSUBSCRIBE, unsubscriptionRequest);
    var response = clientEndpoint->post("", builtSubscriptionRequest);
    return processHubResponse(url, MODE_UNSUBSCRIBE, unsubscriptionRequest, response, clientEndpoint,
                              remainingRedirects);
}

# Retrieves maximum redirects allowed for a subscription/unsubscripton request.
# ```ballerina
# int maxRedirects = getRedirectionMaxCount({});
# ```
# 
# + followRedirects - Optional user provided `http:FollowRedirects` configuration
# + return - Maximum number of redirects allowed for subscription/unsubscripton request
isolated function getRedirectionMaxCount(http:FollowRedirects? followRedirects) returns int {
    if followRedirects is http:FollowRedirects {
        if followRedirects.enabled {
            return followRedirects.maxCount;
        }
    }
    return 0;
}

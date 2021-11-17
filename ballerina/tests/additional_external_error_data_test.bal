// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/log;
import ballerina/http;
import ballerina/regex;   
import ballerina/test;

listener Listener additionalErrorDetailsListener = new (9093);

SubscriberService serviceWithAdditionalErrorDetails = @SubscriberServiceConfig {
    target: "http://0.0.0.0:9191/common/discovery",
    leaseSeconds: 36000,
    unsubscribeOnShutdown: false
} 
    service object {
    isolated remote function onSubscriptionVerification(SubscriptionVerification msg) 
                        returns SubscriptionVerificationSuccess|SubscriptionVerificationError {
        log:printDebug("onSubscriptionVerification invoked ", verificationMessage = msg);
        if (msg.hubTopic == "test1") {

            return error SubscriptionVerificationError(
                "Hub topic not supported", 
                headers = {"header1": "value"}, 
                body = {"message": "Hub topic not supported"});
        } else {
            return SUBSCRIPTION_VERIFICATION_SUCCESS;
        }
    }

    isolated remote function onEventNotification(ContentDistributionMessage event) 
                        returns Acknowledgement|SubscriptionDeletedError? {
        log:printDebug("onEventNotification invoked: ", contentDistributionNotification = event);
        return error SubscriptionDeletedError(
            "Subscriber wants to unsubscribe", 
            headers = {"header1": "value"}, 
            body = {"message": "Unsubscribing from the topic"});
    }
};

@test:BeforeGroups { value:["service-with-additional-details"] }
function beforeAdditionalErrorDetailsService() returns error? {
    check additionalErrorDetailsListener.attach(serviceWithAdditionalErrorDetails, "subscriber");
}

@test:AfterGroups { value:["service-with-additional-details"] }
function afterAdditionalErrorDetailsService() returns error? {
    check additionalErrorDetailsListener.gracefulStop();
}

http:Client subscriberServiceErrorDetailsClientEp = check new ("http://localhost:9093/subscriber");

@test:Config { 
    groups: ["service-with-additional-details"]
}
function testOnIntentVerificationFailedErrorDetails() returns error? {
    http:Response response = check subscriberServiceErrorDetailsClientEp->get("/?hub.mode=subscribe&hub.topic=test1&hub.challenge=1234");
    test:assertEquals(response.statusCode, 404);
    string payload = check response.getTextPayload();
    map<string> responseBody = decodeResponseBody(payload);
    test:assertEquals(responseBody["message"], "Hub topic not supported");
}

@test:Config { 
    groups: ["service-with-additional-details"]
}
function testOnEventNotificationFailedErrorDetails() returns error? {
    http:Request request = new;
    xml requestPayload = xml `<body><action>publish</action></body>`;
    request.setPayload(requestPayload);

    http:Response response = check subscriberServiceErrorDetailsClientEp->post("/", request);
    test:assertEquals(response.statusCode, 410);
    string payload = check response.getTextPayload();
    map<string> responseBody = decodeResponseBody(payload);
    test:assertEquals(responseBody["message"], "Unsubscribing from the topic");
}

isolated function decodeResponseBody(string payload) returns map<string> {
    map<string> body = {};
    if (payload.length() > 0) {
        string[] splittedPayload = regex:split(payload, "&");
        foreach string bodyPart in splittedPayload {
            string[] responseComponent =  regex:split(bodyPart, "=");
            if (responseComponent.length() == 2) {
                body[responseComponent[0]] = responseComponent[1];
            }
        }
        return body;
    }
    return body;
}

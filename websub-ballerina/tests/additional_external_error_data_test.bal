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

import ballerina/io;
import ballerina/http;
import ballerina/regex;   
import ballerina/test;

listener Listener additionalErrorDetailsListener = new (9093);

var serviceWithAdditionalErrorDetails = @SubscriberServiceConfig { target: "http://0.0.0.0:9191/common/discovery", leaseSeconds: 36000 } 
                              service object {
    remote function onSubscriptionVerification(SubscriptionVerification msg)
                        returns SubscriptionVerificationSuccess | SubscriptionVerificationError {
        io:println("onSubscriptionVerification invoked");
        if (msg.hubTopic == "test1") {

            return error SubscriptionVerificationError(
                "Hub topic not supported", 
                headers = {"header1": "value"}, 
                body = {"message": "Hub topic not supported"});
        } else {
            return {};
        }
      }

    remote function onEventNotification(ContentDistributionMessage event) 
                        returns Acknowledgement | SubscriptionDeletedError? {
        io:println("onEventNotification invoked: ", event);
        return error SubscriptionDeletedError(
            "Subscriber wants to unsubscribe",
            headers = {"header1": "value"}, 
            body = {"message": "Unsubscribing from the topic"});
    }
};

@test:BeforeGroups { value:["service-with-additional-details"] }
function beforeAdditionalErrorDetailsService() {
    checkpanic additionalErrorDetailsListener.attach(serviceWithAdditionalErrorDetails, "subscriber");
}

@test:AfterGroups { value:["service-with-additional-details"] }
function afterAdditionalErrorDetailsService() {
    checkpanic additionalErrorDetailsListener.gracefulStop();
}

http:Client subscriberServiceErrorDetailsClientEp = checkpanic new("http://localhost:9093/subscriber");

@test:Config { 
    groups: ["service-with-additional-details"]
}
function testOnIntentVerificationFailedErrorDetails() returns @tainted error? {
    http:Request request = new;

    var response = check subscriberServiceErrorDetailsClientEp->get("/?hub.mode=subscribe&hub.topic=test1&hub.challenge=1234", request);
    if (response is http:Response) {
        test:assertEquals(response.statusCode, 404);
        var payload = response.getTextPayload();
        if (payload is error) {
            test:assertFail("Could not retrieve response body");
        } else {
            var responseBody = decodeResponseBody(payload);
            io:println(responseBody);
            test:assertEquals(responseBody["message"], "Hub topic not supported");
        }
    } else {
        test:assertFail("UnsubscriptionIntentVerification test failed");
    }
}

@test:Config { 
    groups: ["service-with-additional-details"]
}
function testOnEventNotificationFailedErrorDetails() returns @tainted error? {
    http:Request request = new;
    xml requestPayload = xml `<body><action>publish</action></body>`;
    request.setPayload(requestPayload);

    var response = check subscriberServiceErrorDetailsClientEp->post("/", request);
    if (response is http:Response) {
        test:assertEquals(response.statusCode, 410);
        var payload = response.getTextPayload();
        if (payload is error) {
            test:assertFail("Could not retrieve response body");
        } else {
            var responseBody = decodeResponseBody(payload);
            io:println(responseBody);
            test:assertEquals(responseBody["message"], "Unsubscribing from the topic");
        }
    } else {
        test:assertFail("UnsubscriptionIntentVerification test failed");
    }
}

isolated function decodeResponseBody(string payload) returns map<string> {
    map<string> body = {};
    if (payload.length() > 0) {
        string[] splittedPayload = regex:split(payload, "&");
        foreach var bodyPart in splittedPayload {
            var responseComponent =  regex:split(bodyPart, "=");
            if (responseComponent.length() == 2) {
                body[responseComponent[0]] = responseComponent[1];
            }
        }
        return body;
    }
    return body;
}

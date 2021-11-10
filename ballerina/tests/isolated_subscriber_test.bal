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
import ballerina/test;
import ballerina/http;
import ballerina/lang.value;
import ballerina/mime;

string[] receivedMsgs = [];

@SubscriberServiceConfig {}
service /subscriber on new Listener(9103) {
    remote function onSubscriptionVerification(SubscriptionVerification msg) returns SubscriptionVerificationSuccess|SubscriptionVerificationError {
        if (msg.hubTopic == "test1") {
            return SUBSCRIPTION_VERIFICATION_ERROR;
        } else {
            return SUBSCRIPTION_VERIFICATION_SUCCESS;
        }
    }

    remote function onEventNotification(ContentDistributionMessage event) returns Acknowledgement|error? {
        match event.contentType {
            mime:TEXT_PLAIN => {
                string msg = check value:ensureType(event.content);
                receivedMsgs.push(msg);
            }
            _ => {
                log:printDebug("Received content-type is invalid");
            }
        }
        return ACKNOWLEDGEMENT;
    }
}

http:Client isolatedSubTestClient = check new ("http://localhost:9103/subscriber");

@test:Config { 
    groups: ["isolatedSubscriber"]
}
function testOnSubscriptionValidationWithIsolatedSub() returns error? {
    http:Response response = check isolatedSubTestClient->get("/?hub.mode=denied&hub.reason=justToTest");
    test:assertEquals(response.statusCode, 200);
}

@test:Config {
    groups: ["isolatedSubscriber"]
 }
function testOnIntentVerificationSuccessWithIsolatedSub() returns error? {
    http:Response response = check isolatedSubTestClient->get("/?hub.mode=subscribe&hub.topic=test&hub.challenge=1234");
    test:assertEquals(response.statusCode, 200);
    test:assertEquals(response.getTextPayload(), "1234");
}

@test:Config { 
    groups: ["isolatedSubscriber"]
}
function testOnIntentVerificationFailureWithIsolatedSub() returns error? {
    http:Response response = check isolatedSubTestClient->get("/?hub.mode=subscribe&hub.topic=test1&hub.challenge=1234");
    test:assertEquals(response.statusCode, 404);
    string payload = check response.getTextPayload();
    map<string> responseBody = decodeResponseBody(payload);
    test:assertEquals(responseBody["reason"], "Subscription verification failed");
}

@test:Config {
    groups: ["isolatedSubscriber"]
 }
function testOnEventNotificationSuccessWithIsolatedSub() returns error? {
    http:Request request = new;
    json payload =  {"action": "publish", "mode": "remote-hub"};
    request.setPayload(payload);
    http:Response response = check isolatedSubTestClient->post("/", request);
    test:assertEquals(response.statusCode, 202);
}

@test:Config {
    groups: ["isolatedSubscriber"]
 }
function testOnEventNotificationSuccessTextWithIsolatedSub() returns error? {
    http:Request request = new;
    request.setPayload("Hello, World...!");
    http:Response response = check isolatedSubTestClient->post("/", request);
    test:assertEquals(response.statusCode, 202);
}

@test:Config {
    groups: ["isolatedSubscriber"],
    dependsOn: [ testOnEventNotificationSuccessTextWithIsolatedSub ]
 }
function testReceivedValidContent() returns error? {
    string validMsg = receivedMsgs.pop();
    test:assertEquals(validMsg, "Hello, World...!");
}

@test:Config {
    groups: ["isolatedSubscriber"]
}
function testOnEventNotificationSuccessXmlWithIsolatedSub() returns error? {
    http:Request request = new;
    xml payload = xml `<body><action>publish</action></body>`;
    request.setPayload(payload);
    http:Response response = check isolatedSubTestClient->post("/", request);
    test:assertEquals(response.statusCode, 202);
}

@test:Config {
    groups: ["isolatedSubscriber"]
}
function testOnEventNotificationSuccessForUrlEncodedWithIsolatedSub() returns error? {
    http:Request request = new;
    request.setTextPayload("param1=value1&param2=value2");
    check request.setContentType(mime:APPLICATION_FORM_URLENCODED);
    http:Response response = check isolatedSubTestClient->post("", request);
    test:assertEquals(response.statusCode, 202);
}

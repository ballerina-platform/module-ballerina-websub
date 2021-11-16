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
import ballerina/mime;

listener Listener multiServiceListener = new(9096);

@SubscriberServiceConfig {
    unsubscribeOnShutdown: false
} 
service /subscriberOne on multiServiceListener {
    isolated remote function onSubscriptionValidationDenied(SubscriptionDeniedError msg) returns Acknowledgement? {
        log:printDebug("onSubscriptionValidationDenied invoked");
        return ACKNOWLEDGEMENT;
    }

    isolated remote function onSubscriptionVerification(SubscriptionVerification msg)
                        returns SubscriptionVerificationSuccess|SubscriptionVerificationError {
        log:printDebug("onSubscriptionVerification invoked");
        if (msg.hubTopic == "test1") {
            return SUBSCRIPTION_VERIFICATION_ERROR;
        } else {
            return SUBSCRIPTION_VERIFICATION_SUCCESS;
        }
      }

    isolated remote function onEventNotification(ContentDistributionMessage event) 
                        returns Acknowledgement|SubscriptionDeletedError? {
        log:printDebug("onEventNotification invoked ", contentDistributionMessage = event);
        return ACKNOWLEDGEMENT;
    }
}

@SubscriberServiceConfig {
    unsubscribeOnShutdown: false
} 
service /subscriberTwo on multiServiceListener {
    isolated remote function onSubscriptionValidationDenied(SubscriptionDeniedError msg) returns Acknowledgement? {
        log:printDebug("onSubscriptionValidationDenied invoked");
        return ACKNOWLEDGEMENT;
    }

    isolated remote function onSubscriptionVerification(SubscriptionVerification msg)
                        returns SubscriptionVerificationSuccess|SubscriptionVerificationError {
        log:printDebug("onSubscriptionVerification invoked");
        if (msg.hubTopic == "test1") {
            return SUBSCRIPTION_VERIFICATION_ERROR;
        } else {
            return SUBSCRIPTION_VERIFICATION_SUCCESS;
        }
      }

    isolated remote function onEventNotification(ContentDistributionMessage event) 
                        returns Acknowledgement|SubscriptionDeletedError? {
        log:printDebug("onEventNotification invoked ", contentDistributionMessage = event);
        return ACKNOWLEDGEMENT;
    }
}

http:Client clientForServiceOne = check new ("http://localhost:9096/subscriberOne");
http:Client clientForServiceTwo = check new ("http://localhost:9096/subscriberTwo");

@test:Config { 
    groups: ["multiServiceListener"]
}
function testOnSubscriptionValidationWithServiceOne() returns error? {
    http:Response response = check clientForServiceOne->get("/?hub.mode=denied&hub.reason=justToTest");
    test:assertEquals(response.statusCode, 200);
}

@test:Config { 
    groups: ["multiServiceListener"]
}
function testOnSubscriptionValidationWithServiceTwo() returns error? {
    http:Response response = check clientForServiceTwo->get("/?hub.mode=denied&hub.reason=justToTest");
    test:assertEquals(response.statusCode, 200);
}

@test:Config { 
    groups: ["multiServiceListener"]
}
function testOnIntentVerificationFailureServiceOne() returns error? {
    http:Response response = check clientForServiceOne->get("/?hub.mode=subscribe&hub.topic=test1&hub.challenge=1234");
    test:assertEquals(response.statusCode, 404);
    string payload = check response.getTextPayload();
    map<string> responseBody = decodeResponseBody(payload);
    test:assertEquals(responseBody["reason"], "Subscription verification failed");
}

@test:Config { 
    groups: ["multiServiceListener"]
}
function testOnIntentVerificationFailureServiceTwo() returns error? {
    http:Response response = check clientForServiceTwo->get("/?hub.mode=subscribe&hub.topic=test1&hub.challenge=1234");
    test:assertEquals(response.statusCode, 404);
    string payload = check response.getTextPayload();
    map<string> responseBody = decodeResponseBody(payload);
    test:assertEquals(responseBody["reason"], "Subscription verification failed");
}

@test:Config {
    groups: ["multiServiceListener"]
 }
function testOnEventNotificationSuccessServiceOne() returns error? {
    http:Request request = new;
    json payload =  {"action": "publish", "mode": "remote-hub"};
    request.setPayload(payload);

    http:Response response = check clientForServiceOne->post("/", request);
    test:assertEquals(response.statusCode, 202);
}

@test:Config {
    groups: ["multiServiceListener"]
 }
function testOnEventNotificationSuccessServiceTwo() returns error? {
    http:Request request = new;
    json payload =  {"action": "publish", "mode": "remote-hub"};
    request.setPayload(payload);

    http:Response response = check clientForServiceTwo->post("/", request);
    test:assertEquals(response.statusCode, 202);
}

@test:Config {
    groups: ["multiServiceListener"]
}
function testOnEventNotificationSuccessXmlServiceOne() returns error? {
    http:Request request = new;
    xml payload = xml `<body><action>publish</action></body>`;
    request.setPayload(payload);

    http:Response response = check clientForServiceOne->post("/", request);
    test:assertEquals(response.statusCode, 202);
}

@test:Config {
    groups: ["multiServiceListener"]
}
function testOnEventNotificationSuccessXmlServiceTwo() returns error? {
    http:Request request = new;
    xml payload = xml `<body><action>publish</action></body>`;
    request.setPayload(payload);

    http:Response response = check clientForServiceTwo->post("/", request);
    test:assertEquals(response.statusCode, 202);
}

@test:Config {
    groups: ["multiServiceListener"]
}
function testOnEventNotificationSuccessForUrlEncodedServiceOne() returns error? {
    http:Request request = new;
    request.setTextPayload("param1=value1&param2=value2");
    check request.setContentType(mime:APPLICATION_FORM_URLENCODED);
    http:Response response = check clientForServiceOne->post("", request);
    test:assertEquals(response.statusCode, 202);
}

@test:Config {
    groups: ["multiServiceListener"]
}
function testOnEventNotificationSuccessForUrlEncodedServiceTwo() returns error? {
    http:Request request = new;
    request.setTextPayload("param1=value1&param2=value2");
    check request.setContentType(mime:APPLICATION_FORM_URLENCODED);
    http:Response response = check clientForServiceTwo->post("", request);
    test:assertEquals(response.statusCode, 202);
}

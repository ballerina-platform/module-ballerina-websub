// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

service class SimpleWebsubService {
    *SubscriberService;
    remote isolated function onSubscriptionValidationDenied(SubscriptionDeniedError msg) returns Acknowledgement? {
        log:printDebug("onSubscriptionValidationDenied invoked");
        Acknowledgement ack = {
                  headers: {"header1": "value"},
                  body: {"formparam1": "value1"}
        };
        return ack;
    }

    remote isolated function onSubscriptionVerification(SubscriptionVerification msg)
                        returns SubscriptionVerificationSuccess | SubscriptionVerificationError {
        log:printDebug("onSubscriptionVerification invoked");
        if (msg.hubTopic == "test1") {
            return error SubscriptionVerificationError("Hub topic not supported");
        } else {
            return {};
        }
      }

    remote isolated function onEventNotification(ContentDistributionMessage event) 
                        returns Acknowledgement | SubscriptionDeletedError? {
        log:printDebug("onEventNotification invoked ", contentDistributionMessage = event);
        return {};
    }
}

listener Listener manualConfigAttachListener = new (9097);
SimpleWebsubService simpleSubscriberServiceInstace = new;

@test:BeforeGroups { value:["manualConfigAttach"] }
function beforeManualConfigAttachTest() {
    SubscriberServiceConfiguration config = {
        target: "http://0.0.0.0:9191/common/discovery",
        leaseSeconds: 36000
    };
    checkpanic manualConfigAttachListener.attachWithConfig(simpleSubscriberServiceInstace, config, "subscriber");
}

@test:AfterGroups { value:["manualConfigAttach"] }
function afterManualConfigAttachTest() {
    checkpanic manualConfigAttachListener.gracefulStop();
}

http:Client manualConfigAttachClientEp = checkpanic new("http://localhost:9097/subscriber");

@test:Config { 
    groups: ["manualConfigAttach"]
}
function testOnSubscriptionValidationWithManualConfigAttach() returns @tainted error? {
    http:Response response = check manualConfigAttachClientEp->get("/?hub.mode=denied&hub.reason=justToTest");
    test:assertEquals(response.statusCode, 200);
}

@test:Config {
    groups: ["manualConfigAttach"]
 }
function testOnIntentVerificationSuccessWithManualConfigAttach() returns @tainted error? {
    http:Response response = check manualConfigAttachClientEp->get("/?hub.mode=subscribe&hub.topic=test&hub.challenge=1234");
    test:assertEquals(response.statusCode, 200);
    test:assertEquals(response.getTextPayload(), "1234");
}

@test:Config { 
    groups: ["manualConfigAttach"]
}
function testOnIntentVerificationFailureWithManualConfigAttach() returns @tainted error? {
    http:Response response = check manualConfigAttachClientEp->get("/?hub.mode=subscribe&hub.topic=test1&hub.challenge=1234");
    test:assertEquals(response.statusCode, 404);
    string payload = check response.getTextPayload();
    map<string> responseBody = decodeResponseBody(payload);
    test:assertEquals(responseBody["reason"], "Hub topic not supported");
}

@test:Config {
    groups: ["manualConfigAttach"]
 }
function testOnEventNotificationSuccessWithManualConfigAttach() returns @tainted error? {
    http:Request request = new;
    json payload =  {"action": "publish", "mode": "remote-hub"};
    request.setPayload(payload);
    http:Response response = check manualConfigAttachClientEp->post("/", request);
    test:assertEquals(response.statusCode, 202);
}


@test:Config {
    groups: ["manualConfigAttach"]
}
function testOnEventNotificationSuccessXmlWithManualConfigAttach() returns @tainted error? {
    http:Request request = new;
    xml payload = xml `<body><action>publish</action></body>`;
    request.setPayload(payload);
    http:Response response = check manualConfigAttachClientEp->post("/", request);
    test:assertEquals(response.statusCode, 202);
}

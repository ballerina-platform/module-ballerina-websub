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

listener Listener basicSubscriberListener = new (9090);

var simpleSubscriberService = @SubscriberServiceConfig { target: "http://0.0.0.0:9191/common/discovery", leaseSeconds: 36000 } 
                              service object {
    remote function onSubscriptionValidationDenied(SubscriptionDeniedError msg) returns Acknowledgement? {
        log:print("onSubscriptionValidationDenied invoked");
        Acknowledgement ack = {
                  headers: {"header1": "value"},
                  body: {"formparam1": "value1"}
        };
        return ack;
    }

    remote function onSubscriptionVerification(SubscriptionVerification msg)
                        returns SubscriptionVerificationSuccess | SubscriptionVerificationError {
        log:print("onSubscriptionVerification invoked");
        if (msg.hubTopic == "test1") {
            return error SubscriptionVerificationError("Hub topic not supported");
        } else {
            return {};
        }
      }

    remote function onEventNotification(ContentDistributionMessage event) 
                        returns Acknowledgement | SubscriptionDeletedError? {
        log:print("onEventNotification invoked ", contentDistributionMessage = event);
        return {};
    }
};

@test:BeforeGroups { value:["simple-subscriber"] }
function beforeSimpleSubscriberTest() {
    checkpanic basicSubscriberListener.attach(simpleSubscriberService, "subscriber");
}

@test:AfterGroups { value:["simple-subscriber"] }
function afterSimpleSubscriberTest() {
    checkpanic basicSubscriberListener.gracefulStop();
}

http:Client httpClient = checkpanic new("http://localhost:9090/subscriber");

@test:Config { 
    groups: ["simple-subscriber"]
}
function testOnSubscriptionValidation() returns @tainted error? {
    http:Request request = new;

    var response = check httpClient->get("/?hub.mode=denied&hub.reason=justToTest", request);
    if (response is http:Response) {
        test:assertEquals(response.statusCode, 200);
    } else {
        test:assertFail("UnsubscriptionIntentVerification test failed");
    }
}

@test:Config {
    groups: ["simple-subscriber"]
 }
function testOnIntentVerificationSuccess() returns @tainted error? {
    http:Request request = new;

    var response = check httpClient->get("/?hub.mode=subscribe&hub.topic=test&hub.challenge=1234", request);
    if (response is http:Response) {
        test:assertEquals(response.statusCode, 200);
        test:assertEquals(response.getTextPayload(), "1234");
    } else {
        test:assertFail("UnsubscriptionIntentVerification test failed");
    }
}

@test:Config { 
    groups: ["simple-subscriber"]
}
function testOnIntentVerificationFailure() returns @tainted error? {
    http:Request request = new;

    var response = check httpClient->get("/?hub.mode=subscribe&hub.topic=test1&hub.challenge=1234", request);
    if (response is http:Response) {
        test:assertEquals(response.statusCode, 404);
        var payload = response.getTextPayload();
        if (payload is error) {
            test:assertFail("Could not retrieve response body");
        } else {
            var responseBody = decodeResponseBody(payload);
            log:print("Decoded payload retrieved ", payload = responseBody);
            test:assertEquals(responseBody["reason"], "Hub topic not supported");
        }
    } else {
        test:assertFail("UnsubscriptionIntentVerification test failed");
    }
}

@test:Config {
    groups: ["simple-subscriber"]
 }
function testOnEventNotificationSuccess() returns @tainted error? {
    http:Request request = new;
    json payload =  {"action": "publish", "mode": "remote-hub"};
    request.setPayload(payload);

    var response = check httpClient->post("/", request);
    if (response is http:Response) {
        test:assertEquals(response.statusCode, 202);
    } else {
        test:assertFail("UnsubscriptionIntentVerification test failed");
    }
}


@test:Config {
    groups: ["simple-subscriber"]
}
function testOnEventNotificationSuccessXml() returns @tainted error? {
    http:Request request = new;
    xml payload = xml `<body><action>publish</action></body>`;
    request.setPayload(payload);

    var response = check httpClient->post("/", request);
    if (response is http:Response) {
        test:assertEquals(response.statusCode, 202);
    } else {
        test:assertFail("UnsubscriptionIntentVerification test failed");
    }
}
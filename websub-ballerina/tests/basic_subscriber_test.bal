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
import ballerina/test;

listener Listener testListener = new(9090);

@SubscriberServiceConfig {
    path: "/websub",
    subscribeOnStartUp: false,
    target: ["http://localhost:9191/websub/hub", "http://websubpubtopic.com"],
    leaseSeconds: 36000,
    secret: "Kslk30SNF2AChs2"
}
service /subscriber on testListener {
      remote function onSubscriptionValidationDenied(SubscriptionDeniedError msg) returns Acknowledgement {
            io:println("onSubscriptionValidationDenied invoked");
            Acknowledgement ack = {
                  headers: {"header1": "value"},
                  body: {"formparam1": "value1"}
              };
            return ack;
      }

      remote function onSubscriptionVerification(SubscriptionVerification msg)
                        returns SubscriptionVerificationSuccess|SubscriptionVerificationError {
            io:println("onSubscriptionVerification invoked");
            if (msg.hubTopic == "test") {
                return {};
            } else {
                return error SubscriptionVerificationError("Hub topic not supported");
            }
      }

      remote function onEventNotification(ContentDistributionMessage event) {
            io:println("onEventNotification invoked");
            io:println(event.headers);
            io:println(event.contentType);
            io:println(event.content);
      }
}

http:Client httpClient = checkpanic new("http://localhost:9090/subscriber");

@test:Config {
}
function testOnSubscriptionValidation() returns @tainted error? {
    http:Request request = new;

    var response = check httpClient->get("/?hub.mode=denied&hub.reason=justToTest", request);
    if (response is http:Response) {
        test:assertEquals(response.statusCode, 200);
        io:println(response.getTextPayload());
    } else {
        test:assertFail("UnsubscriptionIntentVerification test failed");
    }
}

@test:Config {
}
function testOnIntentVerificationSuccess() returns @tainted error? {
    http:Request request = new;

    var response = check httpClient->get("/?hub.mode=accepted&hub.topic=test&hub.challenge=1234", request);
    if (response is http:Response) {
        test:assertEquals(response.statusCode, 200);
        test:assertEquals(response.getTextPayload(), "1234");
    } else {
        test:assertFail("UnsubscriptionIntentVerification test failed");
    }
}

@test:Config {
}
function testOnIntentVerificationFailure() returns @tainted error? {
    http:Request request = new;

    var response = check httpClient->get("/?hub.mode=accepted&hub.topic=test1&hub.challenge=1234", request);
    if (response is http:Response) {
        test:assertEquals(response.statusCode, 404);
        test:assertEquals(response.getTextPayload(), "Hub topic not supported");
    } else {
        test:assertFail("UnsubscriptionIntentVerification test failed");
    }
}

@test:Config {
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
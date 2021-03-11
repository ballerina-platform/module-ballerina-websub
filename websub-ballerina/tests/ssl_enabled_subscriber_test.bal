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

ListenerConfiguration listenerConfigs = {
    secureSocket: {
        key: {
            path: "tests/resources/ballerinaKeystore.pkcs12",
            password: "ballerina"
        }
    }
};

listener Listener sslEnabledListener = new(9095, listenerConfigs);

@SubscriberServiceConfig {} 
service /subscriber on sslEnabledListener {
    remote function onSubscriptionValidationDenied(SubscriptionDeniedError msg) returns Acknowledgement? {
        log:printDebug("onSubscriptionValidationDenied invoked");
        Acknowledgement ack = {
                  headers: {"header1": "value"},
                  body: {"formparam1": "value1"}
        };
        return ack;
    }

    remote function onSubscriptionVerification(SubscriptionVerification msg)
                        returns SubscriptionVerificationSuccess|SubscriptionVerificationError {
        log:printDebug("onSubscriptionVerification invoked");
        if (msg.hubTopic == "test1") {
            return error SubscriptionVerificationError("Hub topic not supported");
        } else {
            return {};
        }
      }

    remote function onEventNotification(ContentDistributionMessage event) 
                        returns Acknowledgement|SubscriptionDeletedError? {
        log:printDebug("onEventNotification invoked ", contentDistributionMessage = event);
        return {};
    }
}

http:ClientConfiguration httpsConfig = {
    secureSocket: {
        cert: {
            path: "tests/resources/ballerinaTruststore.pkcs12",
            password: "ballerina"
        }
    }
};
http:Client sslEnabledClient = checkpanic new("https://localhost:9095/subscriber", httpsConfig);

@test:Config { 
    groups: ["sslEnabledSubscriber"]
}
function testOnSubscriptionValidationWithSsl() returns @tainted error? {
    http:Request request = new;

    http:Response response = check sslEnabledClient->get("/?hub.mode=denied&hub.reason=justToTest", request);
    test:assertEquals(response.statusCode, 200);
}

@test:Config {
    groups: ["sslEnabledSubscriber"]
 }
function testOnIntentVerificationSuccessWithSsl() returns @tainted error? {
    http:Request request = new;

    http:Response response = check sslEnabledClient->get("/?hub.mode=subscribe&hub.topic=test&hub.challenge=1234", request);
    test:assertEquals(response.statusCode, 200);
    test:assertEquals(response.getTextPayload(), "1234");
}

@test:Config { 
    groups: ["sslEnabledSubscriber"]
}
function testOnIntentVerificationFailureWithSsl() returns @tainted error? {
    http:Request request = new;

    http:Response response = check sslEnabledClient->get("/?hub.mode=subscribe&hub.topic=test1&hub.challenge=1234", request);
    test:assertEquals(response.statusCode, 404);
    string payload = check response.getTextPayload();
    map<string> responseBody = decodeResponseBody(payload);
    test:assertEquals(responseBody["reason"], "Hub topic not supported");
}

@test:Config {
    groups: ["sslEnabledSubscriber"]
 }
function testOnEventNotificationSuccessWithSsl() returns @tainted error? {
    http:Request request = new;
    json payload =  {"action": "publish", "mode": "remote-hub"};
    request.setPayload(payload);

    http:Response response = check sslEnabledClient->post("/", request);
    test:assertEquals(response.statusCode, 202);
}


@test:Config {
    groups: ["sslEnabledSubscriber"]
}
function testOnEventNotificationSuccessXmlWithSsl() returns @tainted error? {
    http:Request request = new;
    xml payload = xml `<body><action>publish</action></body>`;
    request.setPayload(payload);

    http:Response response = check sslEnabledClient->post("/", request);
    test:assertEquals(response.statusCode, 202);
}
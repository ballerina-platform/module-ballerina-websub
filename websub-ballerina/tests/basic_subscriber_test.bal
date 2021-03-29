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

listener Listener basicSubscriberListener = new (9090);

var simpleSubscriberService = @SubscriberServiceConfig { target: "http://0.0.0.0:9191/common/discovery", leaseSeconds: 36000 } 
                              service object {
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
        match event.contentType {
            mime:APPLICATION_FORM_URLENCODED => {
                map<string[]> content = <map<string[]>> event.content;
                log:printInfo("URL encoded content received ", content = content);
            }
            _ => {
                log:printDebug("onEventNotification invoked ", contentDistributionMessage = event);
            }
        }
        
        return ACKNOWLEDGEMENT;
    }
};

@test:BeforeGroups { value:["simpleSubscriber"] }
function beforeSimpleSubscriberTest() {
    checkpanic basicSubscriberListener.attach(simpleSubscriberService, "subscriber");
}

@test:AfterGroups { value:["simpleSubscriber"] }
function afterSimpleSubscriberTest() {
    checkpanic basicSubscriberListener.gracefulStop();
}

http:Client httpClient = checkpanic new("http://localhost:9090/subscriber");

@test:Config { 
    groups: ["simpleSubscriber"]
}
function testOnSubscriptionValidation() returns @tainted error? {
    http:Response response = check httpClient->get("/?hub.mode=denied&hub.reason=justToTest");
    test:assertEquals(response.statusCode, 200);
}

@test:Config {
    groups: ["simpleSubscriber"]
 }
function testOnIntentVerificationSuccess() returns @tainted error? {
    http:Response response = check httpClient->get("/?hub.mode=subscribe&hub.topic=test&hub.challenge=1234");
    test:assertEquals(response.statusCode, 200);
    test:assertEquals(response.getTextPayload(), "1234");
}

@test:Config { 
    groups: ["simpleSubscriber"]
}
function testOnIntentVerificationFailure() returns @tainted error? {
    http:Response response = check httpClient->get("/?hub.mode=subscribe&hub.topic=test1&hub.challenge=1234");
    test:assertEquals(response.statusCode, 404);
    string payload = check response.getTextPayload();
    map<string> responseBody = decodeResponseBody(payload);
    test:assertEquals(responseBody["reason"], "Subscription verification failed");
}

@test:Config {
    groups: ["simpleSubscriber"]
 }
function testOnEventNotificationSuccess() returns @tainted error? {
    http:Request request = new;
    json payload =  {"action": "publish", "mode": "remote-hub"};
    request.setPayload(payload);

    http:Response response = check httpClient->post("/", request);
    test:assertEquals(response.statusCode, 202);
}


@test:Config {
    groups: ["simpleSubscriber"]
}
function testOnEventNotificationSuccessXml() returns @tainted error? {
    http:Request request = new;
    xml payload = xml `<body><action>publish</action></body>`;
    request.setPayload(payload);

    http:Response response = check httpClient->post("/", request);
    test:assertEquals(response.statusCode, 202);
}

@test:Config {
    groups: ["simpleSubscriber"]
}
function testOnEventNotificationSuccessForUrlEncoded() returns @tainted error? {
    http:Request request = new;
    check request.setContentType(mime:APPLICATION_FORM_URLENCODED);
    http:Response response = check httpClient->post("/?param1=value1&param2=value2", request);
    test:assertEquals(response.statusCode, 202);
}

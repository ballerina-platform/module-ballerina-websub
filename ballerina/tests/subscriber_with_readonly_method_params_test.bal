// Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/mime;
import ballerina/http;

@SubscriberServiceConfig {}
service /subscriber on new Listener(9104) {
    isolated remote function onSubscriptionValidationDenied(readonly & SubscriptionDeniedError msg) returns Acknowledgement? {
        test:assertTrue(msg is readonly);
        return ACKNOWLEDGEMENT;
    }

    isolated remote function onSubscriptionVerification(readonly & SubscriptionVerification msg)
                        returns SubscriptionVerificationSuccess|SubscriptionVerificationError {
        test:assertTrue(msg is readonly);
        if (msg.hubTopic == "test1") {
            return SUBSCRIPTION_VERIFICATION_ERROR;
        } else {
            return SUBSCRIPTION_VERIFICATION_SUCCESS;
        }
    }

    remote function onUnsubscriptionVerification(readonly & UnsubscriptionVerification msg)
                    returns UnsubscriptionVerificationSuccess|UnsubscriptionVerificationError {
        test:assertTrue(msg is readonly);
        if (msg.hubTopic == "test1") {
            return UNSUBSCRIPTION_VERIFICATION_ERROR;
        } else {
            return UNSUBSCRIPTION_VERIFICATION_SUCCESS;
        }
    }

    isolated remote function onEventNotification(readonly & ContentDistributionMessage event)
                        returns Acknowledgement|SubscriptionDeletedError? {
        test:assertTrue(event is readonly);
        match event.contentType {
            mime:APPLICATION_FORM_URLENCODED => {
                map<string> content = <map<string>>event.content;
                log:printInfo("URL encoded content received ", content = content);
            }
            _ => {
                log:printDebug("onEventNotification invoked ", contentDistributionMessage = event);
            }
        }

        return ACKNOWLEDGEMENT;
    }
}

http:Client readonlyParamTestClient = check new ("http://localhost:9104/subscriber");

@test:Config {
    groups: ["subscriberWithReadonlyParams"]
}
function testOnSubscriptionValidationWithReadonly() returns error? {
    http:Response response = check readonlyParamTestClient->get("/?hub.mode=denied&hub.reason=justToTest");
    test:assertEquals(response.statusCode, 200);
}

@test:Config {
    groups: ["subscriberWithReadonlyParams"]
}
function testOnIntentVerificationSuccessWithReadonly() returns error? {
    http:Response response = check readonlyParamTestClient->get("/?hub.mode=subscribe&hub.topic=test&hub.challenge=1234");
    test:assertEquals(response.statusCode, 200);
    test:assertEquals(response.getTextPayload(), "1234");
}

@test:Config {
    groups: ["subscriberWithReadonlyParams"]
}
function testOnEventNotificationSuccessWithReadonly() returns error? {
    http:Request request = new;
    json payload = {"action": "publish", "mode": "remote-hub"};
    request.setPayload(payload);
    http:Response response = check readonlyParamTestClient->post("/", request);
    test:assertEquals(response.statusCode, 202);
}

@test:Config {
    groups: ["subscriberWithReadonlyParams"]
}
function testUnsubscriptionIntentVerificationSuccessWithReadonly() returns error? {
    http:Response response = check readonlyParamTestClient->get("/?hub.mode=unsubscribe&hub.topic=test&hub.challenge=1234");
    test:assertEquals(response.statusCode, 200);
    test:assertEquals(response.getTextPayload(), "1234");
}

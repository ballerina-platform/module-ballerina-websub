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

import ballerina/test;
import ballerina/http;
import ballerina/mime;

listener Listener errorReturnsSubscriberListener = new (9099);

SubscriberService subscriberWithErrorReturns = @SubscriberServiceConfig { target: "http://0.0.0.0:9191/common/discovery", leaseSeconds: 36000, unsubscribeOnShutdown: false } 
                              service object {
    isolated remote function onSubscriptionValidationDenied(SubscriptionDeniedError msg) returns error? {
        return error ("Error occured while processing request");
    }

    isolated remote function onSubscriptionVerification(SubscriptionVerification msg) returns error {
        return error ("Error occured while processing request");
    }

    isolated remote function onEventNotification(ContentDistributionMessage event) returns error? {
        return error ("Error occured while processing request");
    }
};

@test:BeforeGroups { value:["subscriberWithErrorReturns"] }
function beforeSubscriberWithErrorReturnsTest() returns error? {
    check errorReturnsSubscriberListener.attach(subscriberWithErrorReturns, "subscriber");
}

@test:AfterGroups { value:["subscriberWithErrorReturns"] }
function afterSubscriberWithErrorReturnsTest() returns error? {
    check errorReturnsSubscriberListener.gracefulStop();
}

http:Client SubscriberWithErrorReturnsClientEp = check new("http://localhost:9099/subscriber");

@test:Config { 
    groups: ["subscriberWithErrorReturns"]
}
function testOnSubscriptionValidationWithErrorReturnType() returns error? {
    http:Response response = check SubscriberWithErrorReturnsClientEp->get("/?hub.mode=denied&hub.reason=justToTest");
    test:assertEquals(response.statusCode, 200);
}

@test:Config {
    groups: ["subscriberWithErrorReturns"]
 }
function testOnIntentVerificationSuccessWithErrorReturnType() returns error? {
    http:Response response = check SubscriberWithErrorReturnsClientEp->get("/?hub.mode=subscribe&hub.topic=test&hub.challenge=1234");
    test:assertEquals(response.statusCode, 404);
    test:assertEquals(response.getTextPayload(), "reason=Error occured while processing request");
}

@test:Config {
    groups: ["subscriberWithErrorReturns"]
}
function testOnEventNotificationSuccessXmlWithErrorReturnType() returns error? {
    http:Request request = new;
    xml payload = xml `<body><action>publish</action></body>`;
    request.setPayload(payload);
    http:Response response = check SubscriberWithErrorReturnsClientEp->post("/", request);
    test:assertEquals(response.statusCode, 202);
}

@test:Config {
    groups: ["subscriberWithErrorReturns"]
}
function testOnEventNotificationSuccessForUrlEncodedWithErrorReturnType() returns error? {
    http:Request request = new;
    request.setTextPayload("param1=value1&param2=value2");
    check request.setContentType(mime:APPLICATION_FORM_URLENCODED);
    http:Response response = check SubscriberWithErrorReturnsClientEp->post("", request);
    test:assertEquals(response.statusCode, 202);
}

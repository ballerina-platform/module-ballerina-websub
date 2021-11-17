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

listener Listener serviceWithDefaultImplListener = new (9091);

SubscriberService serviceWithDefaultImpl = @SubscriberServiceConfig { target: "http://0.0.0.0:9191/common/discovery", leaseSeconds: 36000, secret: "Kslk30SNF2AChs2", unsubscribeOnShutdown: false } 
                              service object {
    isolated remote function onEventNotification(ContentDistributionMessage event) 
                        returns Acknowledgement|SubscriptionDeletedError? {
        log:printDebug("onEventNotification invoked ", contentDistributionMessage = event);
        Acknowledgement ack = {
            headers: {
                "header1": "value1-1",
                "header-2": [ "value2-1", "value2-2"]
            }
        };
        return ack;
    }
};

@test:BeforeGroups { value:["defaultMethodImpl"] }
function beforeGroupTwo() returns error? {
    check serviceWithDefaultImplListener.attach(serviceWithDefaultImpl, "subscriber");
}

@test:AfterGroups { value:["defaultMethodImpl"] }
function afterGroupTwo() returns error? {
    check serviceWithDefaultImplListener.gracefulStop();
}

http:Client serviceWithDefaultImplClientEp = check new("http://localhost:9091/subscriber");

@test:Config { 
    groups: ["defaultMethodImpl"]
}
function testOnSubscriptionValidationDefaultImpl() returns error? {
    http:Response response = check serviceWithDefaultImplClientEp->get("/?hub.mode=denied&hub.reason=justToTest");
    test:assertEquals(response.statusCode, 200);
}

@test:Config {
    groups: ["defaultMethodImpl"]
 }
function testOnIntentVerificationSuccessDefaultImpl() returns error? {
    http:Response response = check serviceWithDefaultImplClientEp->get("/?hub.mode=subscribe&hub.topic=test&hub.challenge=1234");
    test:assertEquals(response.statusCode, 200);
    test:assertEquals(response.getTextPayload(), "1234");
}

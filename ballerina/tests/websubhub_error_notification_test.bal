// Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org).
//
// WSO2 LLC. licenses this file to you under the Apache License,
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
import ballerina/lang.runtime;

isolated boolean isOnHubErrorInvoked = false;

SubscriberService websubSubscriber = @SubscriberServiceConfig {} service object {
    
    isolated remote function onSubscriptionValidationDenied(SubscriptionDeniedError msg) {}

    isolated remote function onSubscriptionVerification(SubscriptionVerification msg) returns SubscriptionVerificationSuccess {
        return SUBSCRIPTION_VERIFICATION_SUCCESS;
    }

    isolated remote function onEventNotification(ContentDistributionMessage event) {}

    remote function onHubError(InternalHubError msg) returns Acknowledgement|error? {
        lock {
            isOnHubErrorInvoked = true;
        }
        return ACKNOWLEDGEMENT;
    }
};

final http:Client hubNotifySubscriberEp = check new (string `http://localhost:${BASIC_SUB_PORT}/hub-notify-subscriber`);

@test:BeforeGroups { 
    value:["hubErrorNotification"] 
}
function beforeHubNotificationSubscriberTest() returns error? {
    check basicSubscriberListener.attach(websubSubscriber, "hub-notify-subscriber");
}

@test:Config { 
    groups: ["hubErrorNotification"]
}
function testOnHubError() returns error? {
    http:Response response = check hubNotifySubscriberEp->get("/?hub.mode=hub-error&hub.topic=http://example.com/topic&hub.reason=Broker+unavailable");
    test:assertEquals(response.statusCode, 200, "Received failure status, expected successful response");
    boolean onHubErrorInvoked = false;
    lock {
        onHubErrorInvoked = isOnHubErrorInvoked;
    }
    runtime:sleep(2.0);
    test:assertTrue(onHubErrorInvoked, "`onHubError` not invoked even though the notification was dispatched");
}

@test:Config { 
    groups: ["hubErrorNotification"]
}
function testInvalidHubMode() returns error? {
    http:Response response = check hubNotifySubscriberEp->get("/?hub.mode=internal-hub-error&hub.topic=http://example.com/topic&hub.reason=Broker+unavailable");
    test:assertEquals(response.statusCode, 400, "Received success status, expected failure response");
}

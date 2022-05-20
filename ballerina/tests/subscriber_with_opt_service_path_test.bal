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
import ballerina/lang.runtime;
import ballerina/test;

isolated boolean subscriptionVerified = false;

listener Listener subscriberWithoutServicePathListener = new (9105);

SubscriberService subscriberWithoutServicePath = @SubscriberServiceConfig {
    target: "http://127.0.0.1:9191/common/discovery",
    callback: "http://localhost:9105",
    leaseSeconds: 36000
}
service object {
    isolated remote function onSubscriptionVerification(SubscriptionVerification msg) returns SubscriptionVerificationSuccess {
        lock {
            subscriptionVerified = true;
        }
        log:printInfo("onSubscriptionVerification invoked");
        return SUBSCRIPTION_VERIFICATION_SUCCESS;
    }

    isolated remote function onEventNotification(ContentDistributionMessage event) returns Acknowledgement {
        log:printInfo("onEventNotification invoked ", contentDistributionMessage = event);
        return ACKNOWLEDGEMENT;
    }
};

@test:BeforeGroups { value:["subscriberWithoutServicePath"] }
function beforeSubscriberWithoutServicePathTest() returns error? {
    runtime:sleep(5);
    log:printInfo("Initializing subscriber service");
    check subscriberWithoutServicePathListener.attach(subscriberWithoutServicePath, ());
}

@test:AfterGroups { value:["subscriberWithoutServicePath"] }
function afterSubscriberWithoutServicePathTest() returns error? {
    check subscriberWithoutServicePathListener.gracefulStop();
}

@test:Config { 
    groups: ["subscriberWithoutServicePath"]
}
function testSubscriptionWithoutServicePath() returns error? {
    runtime:sleep(5);
    boolean verified = false;
    lock {
        verified = subscriptionVerified;
    }
    test:assertTrue(verified, "Subscription verification was unssuccessfull");
}

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

listener Listener optionalTargetListener = new (9094);

SubscriberService optionalSubscriberTarget = @SubscriberServiceConfig { leaseSeconds: 36000, secret: "Kslk30SNF2AChs2", unsubscribeOnShutdown: false } 
                              service object {
    isolated remote function onEventNotification(ContentDistributionMessage event) 
                        returns Acknowledgement | SubscriptionDeletedError? {
        log:printDebug("onEventNotification invoked ", contentDistributionMessage = event);
        return ACKNOWLEDGEMENT;
    }
};

@test:AfterGroups { value:["optionalSubscriberTarget"] }
function afterOptionalTargetUrlTest() returns error? {
    check optionalTargetListener.gracefulStop();
}

@test:Config { 
    groups: ["optionalSubscriberTarget"]
}
function testOptionalTargetUrl() returns error? {
    check optionalTargetListener.attach(optionalSubscriberTarget, "/samples");
    check optionalTargetListener.'start();
}

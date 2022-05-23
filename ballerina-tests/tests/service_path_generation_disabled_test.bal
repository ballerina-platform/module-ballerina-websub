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

import ballerina/test;
import ballerina/websub;

listener websub:Listener pathGenDisabledListener1 = new (10002);

@websub:SubscriberServiceConfig {
    callback: "http://localhost:10002"
}
service on pathGenDisabledListener1 {
    remote function onEventNotification(websub:ContentDistributionMessage message)
                returns websub:Acknowledgement|websub:SubscriptionDeletedError? {
        return websub:ACKNOWLEDGEMENT;
    }
}

listener websub:Listener pathGenDisabledListener2 = new (10003);

@websub:SubscriberServiceConfig {
    callback: "http://localhost:10003"
}
service / on pathGenDisabledListener2 {
    remote function onEventNotification(websub:ContentDistributionMessage message)
                returns websub:Acknowledgement|websub:SubscriptionDeletedError? {
        return websub:ACKNOWLEDGEMENT;
    }
}

@test:Config { 
    groups: ["integrationTest"]
}
function testServicePathAutoGenerationDisabled() returns error? {
    check pathGenDisabledListener1.gracefulStop();
    check pathGenDisabledListener2.gracefulStop();
}

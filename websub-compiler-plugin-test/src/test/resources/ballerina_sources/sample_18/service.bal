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

import ballerina/websub;
import ballerina/io;

@websub:SubscriberServiceConfig{}
service /subscriber on new websub:Listener(9090) {
    remote function onSubscriptionValidationDenied(websub:SubscriptionDeniedError msg) returns websub:Acknowledgement|error? {
        return websub:ACKNOWLEDGEMENT;
    }

    remote function onSubscriptionVerification(websub:SubscriptionVerification msg)
                            returns websub:SubscriptionVerificationSuccess|websub:SubscriptionVerificationError|error {
        if (msg.hubTopic == "test1") {
                return websub:SUBSCRIPTION_VERIFICATION_ERROR;
        } else {
            return websub:SUBSCRIPTION_VERIFICATION_SUCCESS;
        }
    }

    remote function onEventNotification(websub:ContentDistributionMessage event) returns error? {
        var retrievedContent = event.content;
        if (retrievedContent is json) {
            if (retrievedContent.zen is string) {
                int hookId = check retrievedContent.hook_id;
                json sender = check retrievedContent.sender;
                int senderId = check sender.id;
                io:println(string `PingEvent received for webhook [${hookId}]`);
                io:println(string `Event sender [${senderId}]`);
            } else if (retrievedContent.ref is string) {
                json repository = check retrievedContent.repository;
                string repositoryName = check repository.name;
                string lastUpdatedTime = check repository.updated_at;
                io:println(string `PushEvent received for [${repositoryName}]`);
                io:println(string `Last updated at ${lastUpdatedTime}`);
            }
        } else {
            io:println("Unrecognized content type, hence ignoring");
        }
    }
}

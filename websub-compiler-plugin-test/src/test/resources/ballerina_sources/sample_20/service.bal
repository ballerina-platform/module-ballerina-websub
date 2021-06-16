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
import ballerina/lang.value;

@websub:SubscriberServiceConfig{}
service /subscriber on new websub:Listener(9090) {
    remote function onEventNotification(websub:ContentDistributionMessage event) returns error? {
        if self.isJsonContent(event) {
            json retrievedContent = check value:ensureType(event.content, json);
            if self.isPingEvent(retrievedContent) {
                int hookId = check retrievedContent.hook_id;
                json sender = check retrievedContent.sender;
                int senderId = check sender.id;
                io:println(string `PingEvent received for webhook [${hookId}]`);
                io:println(string `Event sender [${senderId}]`);
            } else if self.isPushEvent(retrievedContent) {
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

    function isJsonContent(websub:ContentDistributionMessage event) returns boolean {
        return event.content is json;
    }

    function isPingEvent(json retrievedContent) returns boolean {
        return retrievedContent.zen is string;
    }

    function isPushEvent(json retrievedContent) returns boolean {
        return retrievedContent.ref is string;
    }
}

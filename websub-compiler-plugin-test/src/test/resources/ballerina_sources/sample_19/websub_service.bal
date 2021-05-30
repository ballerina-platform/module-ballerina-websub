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
import ballerina/log;
import ballerina/jballerina.java;

isolated service class WebSubService {
    *websub:SubscriberService;

    public isolated function init(CustomWebhookService 'service) returns error? {
        self.externInit('service);
    }

    isolated remote function onEventNotification(websub:ContentDistributionMessage event)
                        returns websub:Acknowledgement|websub:SubscriptionDeletedError|error? {
        log:printInfo("onEventNotification invoked ", contentDistributionMessage = event);
        if (event.content is json) {
            Payload payload = check event.content.cloneWithType(Payload);
            string eventType = payload["eventType"];
            json eventData = payload["eventData"];
            log:printInfo("Received data ", data = eventData);
            match (eventType) {
                "start" => {
                    StartupMessage message = check eventData.cloneWithType(StartupMessage);
                    var response = self.callOnStartupMethod(message);
                    if (response is StartupError) {
                        return error websub:SubscriptionDeletedError(response.message());
                    }
                }
                "notify" => {
                    EventNotification message = check eventData.cloneWithType(EventNotification);
                    var response = self.callOnEventMethod(message);
                }
                _ => {}
            }
        }

        return {};
    }

    isolated function externInit(CustomWebhookService 'service) = @java:Method {
        'class: "io.ballerinax.webhook.NativeWebhookAdaptor"
    } external;

    isolated function callOnStartupMethod(StartupMessage msg)
                                    returns Acknowledgement|StartupError? = @java:Method {
        'class: "io.ballerinax.webhook.NativeWebhookAdaptor"
    } external;

    isolated function callOnEventMethod(EventNotification msg)
                                    returns Acknowledgement? = @java:Method {
        'class: "io.ballerinax.webhook.NativeWebhookAdaptor"
    } external;
}

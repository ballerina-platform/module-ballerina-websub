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
import ballerina/http;

public class Listener {
    private websub:Listener subscriberListener;
    private websub:SubscriberService? subscriberService;

    public isolated function init(int|http:Listener listenTo, *websub:ListenerConfiguration config) returns error? {
        self.subscriberListener = check new(listenTo, config);
        self.subscriberService = ();
    }

    public isolated function attach(CustomWebhookService s, string[]|string? name = ()) returns error? {
        websub:SubscriberServiceConfiguration? configuration = retrieveSubscriberServiceAnnotations(s);
        if (configuration is websub:SubscriberServiceConfiguration) {
            self.subscriberService = check new WebSubService(s);
            check self.subscriberListener.attachWithConfig(<websub:SubscriberService>self.subscriberService, configuration, name);
        } else {
            return error ListenerError("Could not find the required service-configurations");
        }
    }

    public isolated function detach(CustomWebhookService s) returns error? {
        check self.subscriberListener.detach(<websub:SubscriberService>self.subscriberService);
    }

    public isolated function 'start() returns error? {
        check self.subscriberListener.'start();
    }

    public isolated function gracefulStop() returns error? {
        return self.subscriberListener.gracefulStop();
    }

    public isolated function immediateStop() returns error? {
        return self.subscriberListener.immediateStop();
    }
}

isolated function retrieveSubscriberServiceAnnotations(CustomWebhookService serviceType) returns websub:SubscriberServiceConfiguration? {
    typedesc<any> serviceTypedesc = typeof serviceType;
    return serviceTypedesc.@websub:SubscriberServiceConfig;
}

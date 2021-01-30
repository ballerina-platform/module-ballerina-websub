// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/http;
// import ballerina/mime;
import ballerina/jballerina.java;

service class HttpService {
    private SubscriberService subscriberService;
    private boolean isSubscriptionValidationDeniedAvailable = false;
    private boolean isSubscriptionVerificationAvailable = false;
    private boolean isEventNotificationAvailable = false;

    # Invoked during the initialization of a `websub:HttpService`
    #
    # + subscriberService   - {@code websub:SubscriberService} provided service
    public isolated function init(SubscriberService subscriberService) {
        self.subscriberService = subscriberService;
        
        string[] methodNames = getServiceMethodNames(subscriberService);
        
        foreach var methodName in methodNames {
            match methodName {
                "onSubscriptionValidationDenied" => {
                    self.isSubscriptionValidationDeniedAvailable = true;
                }
                "onSubscriptionVerification" => {
                    self.isSubscriptionVerificationAvailable = true;
                }
                "onEventNotification" => {
                    self.isEventNotificationAvailable = true;
                }
                _ => {}
            }
        }
    }

    // [todo - ayesh] read the spec and implement proper HTTP POST methods which are required
    resource function post .(http:Caller caller, http:Request request) {

    }

    // [todo - ayesh] read the spec and implement proper HTTP GET methods which are required
    resource function get .(http:Caller caller, http:Request request) {

    }
}

isolated function getServiceMethodNames(SubscriberService hubService) returns string[] = @java:Method {
    'class: "io.ballerina.stdlib.websub.SubscriberNativeOperationHandler"
} external;
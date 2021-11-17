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

import ballerina/test;
import ballerina/http;
import ballerina/log;

const string CALLBACK = "https://sample.subscriber.com/subscriber";
const string DISCOVERY_SUCCESS_URL = "http://127.0.0.1:9192/common/discovery";
const string DISCOVERY_FAILURE_URL = "http://127.0.0.1:9192/common/failed";
const string HUB_SUCCESS_URL = "http://127.0.0.1:9192/common/hub";
const string HUB_FAILURE_URL = "http://127.0.0.1:9192/common/failed";
const string COMMON_TOPIC = "https://sample.topic.com";

service /common on new http:Listener(9192) {
    isolated resource function get discovery(http:Caller caller, http:Request request) returns error? {
        http:Response response = new;
        response.addHeader("Link", "<http://127.0.0.1:9192/common/hub>; rel=\"hub\"");
        response.addHeader("Link", "<https://sample.topic.com>; rel=\"self\"");
        check caller->respond(response);
    }

    isolated resource function post hub(http:Caller caller, http:Request request) returns error? {
        check caller->respond();
    }
}

isolated function getServiceAnnotationConfig(string|[string, string] target) returns SubscriberServiceConfiguration {
    return {
        target: target,
        leaseSeconds: 36000,
        callback: CALLBACK
    };
}

final SubscriberService websubServiceObj = service object {
    isolated remote function onEventNotification(ContentDistributionMessage event) 
                        returns Acknowledgement {        
        return ACKNOWLEDGEMENT;
    }
};

@test:Config { 
    groups: ["subscriptionInitiation"]
}
isolated function testSubscriptionInitiationSuccessWithDiscoveryUrl() returns error? {
    SubscriberServiceConfiguration config = getServiceAnnotationConfig(DISCOVERY_SUCCESS_URL);
    check subscribe(config, "https://sample.com/sub1");
}

@test:Config {
    groups: ["subscriptionInitiation"]
}
isolated function testSubscriptionInitiationSuccessWithDiscoveryUrlAndSecret() returns error? {
    SubscriberServiceConfiguration config = getServiceAnnotationConfig(DISCOVERY_SUCCESS_URL);
    config.secret = "test123#";
    check subscribe(config, "https://sample.com/sub1");
}

@test:Config {
    groups: ["subscriptionInitiation"]
}
isolated function testSubscriptionInitiationSuccessWithHubAndTopic() returns error? {
    SubscriberServiceConfiguration config = getServiceAnnotationConfig([ HUB_SUCCESS_URL, COMMON_TOPIC ]);
    check subscribe(config, "https://sample.com/sub1");
}

@test:Config {
    groups: ["subscriptionInitiation"]
}
isolated function testSubscriptionInitiationSuccessWithHubAndTopicAndSecret() returns error? {
    SubscriberServiceConfiguration config = getServiceAnnotationConfig([ HUB_SUCCESS_URL, COMMON_TOPIC ]);
    config.secret = "test123#";
    check subscribe(config, "https://sample.com/sub1");
}

@test:Config { 
    groups: ["subscriptionInitiation"]
}
isolated function testSubscriptionInitiationFailureWithDiscoveryUrl() returns error? {
    SubscriberServiceConfiguration config = getServiceAnnotationConfig(DISCOVERY_FAILURE_URL);
    var response = subscribe(config, "https://sample.com/sub1");
    test:assertTrue(response is ResourceDiscoveryFailedError);
    if response is error {
        string errorDetails = response.message();
        string errorMsg = string `Subscription initiation failed due to: ${errorDetails}`;
        log:printError(errorMsg);
    }
}

@test:Config { 
    groups: ["subscriptionInitiation"]
}
isolated function testSubscriptionInitiationFailureWithHubAndTopic() returns error? {
    SubscriberServiceConfiguration config = getServiceAnnotationConfig([ HUB_FAILURE_URL, COMMON_TOPIC ]);
    var response = subscribe(config, "https://sample.com/sub1");
    test:assertTrue(response is SubscriptionInitiationError);
    if response is error {
        string errorDetails = response.message();
        string errorMsg = string `Subscription initiation failed due to: ${errorDetails}`;
        log:printError(errorMsg);
    }
}

@test:Config { 
    groups: ["unSubscriptionInitiation"]
}
isolated function testUnSubscriptionInitiationSuccessWithDiscoveryUrl() returns error? {
    SubscriberServiceConfiguration config = {
        target: DISCOVERY_SUCCESS_URL,
        leaseSeconds: 36000,
        callback: CALLBACK,
        unsubscribeOnShutdown: true
    };
    check unsubscribe(config, "https://sample.com/sub1");
}

@test:Config { 
    groups: ["unSubscriptionInitiation"]
}
isolated function testUnSubscriptionInitiationFailureWithDiscoveryUrl() returns error? {
    SubscriberServiceConfiguration config = {
        target: DISCOVERY_FAILURE_URL,
        leaseSeconds: 36000,
        callback: CALLBACK,
        unsubscribeOnShutdown: true
    };
    var response = unsubscribe(config, "https://sample.com/sub1");
    test:assertTrue(response is ResourceDiscoveryFailedError);
    if response is error {
        string errorDetails = response.message();
        string errorMsg = string `Subscription initiation failed due to: ${errorDetails}`;
        log:printError(errorMsg);
    }
}

@test:Config { 
    groups: ["unSubscriptionInitiation"]
}
isolated function testUnSubscriptionInitiationFailureWithHubAndTopic() returns error? {
    SubscriberServiceConfiguration config = {
        target: [HUB_FAILURE_URL, COMMON_TOPIC],
        leaseSeconds: 36000,
        callback: CALLBACK,
        unsubscribeOnShutdown: true
    };
    var response = unsubscribe(config, "https://sample.com/sub1");
    test:assertTrue(response is SubscriptionInitiationError);
    if response is error {
        string errorDetails = response.message();
        string errorMsg = string `Subscription initiation failed due to: ${errorDetails}`;
        log:printError(errorMsg);
    }
}

@test:Config { 
    groups: ["unSubscriptionInitiation"]
}
isolated function testUnSubscriptionInitiationSuccessWithHubAndTopic() returns error? {
    SubscriberServiceConfiguration config = {
        target: [HUB_SUCCESS_URL, COMMON_TOPIC],
        leaseSeconds: 36000,
        callback: CALLBACK,
        unsubscribeOnShutdown: true
    };
    check unsubscribe(config, "https://sample.com/sub1");
}

@test:Config { 
    groups: ["unSubscriptionInitiation"]
}
isolated function testUnSubscriptionInitiationDisable() returns error? {
    SubscriberServiceConfiguration config = getServiceAnnotationConfig([ HUB_SUCCESS_URL, COMMON_TOPIC ]);
    check unsubscribe(config, "https://sample.com/sub1");
}

listener Listener ls = new (9100);

@test:Config { 
    groups: ["subscriptionInitiation"]
}
function testSubInitFailedWithListenerForResourceDiscoveryFailure() returns error? {
    var res = ls.attachWithConfig(websubServiceObj, getServiceAnnotationConfig(DISCOVERY_FAILURE_URL), "sub");
    if res is error {
        log:printError("[testSubInitFailedWithListenerForResourceDiscoveryFailure] error occurred ", 'error = res);
    }
    test:assertFalse(res is error);
    var startDetails = ls.'start();
    test:assertTrue(startDetails is error);
    if startDetails is error {
        string expected = "Subscription initiation failed due to: Link header unavailable in discovery response";
        test:assertEquals(startDetails.message(), expected);
    }
    check ls.gracefulStop();
}

@test:Config { 
    groups: ["subscriptionInitiation"]
}
function testSubInitFailedWithListenerForSubFailure() returns error? {
    var res = ls.attachWithConfig(websubServiceObj, getServiceAnnotationConfig([ HUB_FAILURE_URL, COMMON_TOPIC ]), "sub");
    test:assertFalse(res is error);
    var startDetails = ls.'start();
    test:assertTrue(startDetails is error);
    if startDetails is error {
        string expected = "Subscription initiation failed due to: Error in request: Mode[subscribe] at Hub[http://127.0.0.1:9192/common/failed] - no matching resource found for path : /common/failed , method : POST";
        test:assertEquals(startDetails.message(), expected);
    }
    check ls.gracefulStop();
}

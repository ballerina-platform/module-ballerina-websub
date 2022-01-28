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

import ballerina/http;
import ballerina/log;
import ballerina/uuid;
import ballerina/test;
import ballerina/lang.runtime;

isolated boolean verified = false;

isolated function updateVerificationState(boolean state) {
    lock {
        verified = state;
    }
}

isolated function isVerified() returns boolean {
    lock {
        return verified;
    }
}

service /common on new http:Listener(9197) {
    isolated resource function post hub(http:Caller caller, http:Request request) returns error? {        
        string mode = check getHubMode(request);
        log:printInfo("[UNSUB_TEST] Received a request for : ", hubMode = mode);
        check caller->respond();
        error? result = notifySubscriber(mode);
        if result is error {
            log:printError("[UNSUB_TEST] Error occurred while verifying sub/unsub", result);
        }
    }
}

isolated function getHubMode(http:Request request) returns string|error {
    map<string> queryParams = check request.getFormParams();
    return queryParams[HUB_MODE] ?: "";
}

final http:Client notificationClientEp = check  new("http://127.0.0.1:9102/sub");

isolated function notifySubscriber(string mode) returns error? {
    string challenge = uuid:createType4AsString();
    string queryParams = string`?${HUB_MODE}=${mode}&${HUB_TOPIC}=test&${HUB_CHALLENGE}=${challenge}&${HUB_LEASE_SECONDS}=100000`;
    log:printInfo("[UNSUB_VER] Sending verification: ", params = challenge);
    string response = check notificationClientEp->get(queryParams);
    log:printInfo("[UNSUB_VER] Received verification", response = response);
    if challenge == response {
        log:printInfo("[UNSUB_VER] Updating verification status");
        updateVerificationState(true);
    }
}

listener Listener unsubscriptionTestListener = new (9102);

SubscriberService unsubscriptionTestSubscriber = @SubscriberServiceConfig {
    target: ["http://127.0.0.1:9197/common/hub", "test"], 
    leaseSeconds: 36000,
    unsubscribeOnShutdown: true
}
service object {
    isolated remote function onEventNotification(ContentDistributionMessage event) 
                        returns Acknowledgement|SubscriptionDeletedError? {
        return ACKNOWLEDGEMENT;
    }

    isolated remote function onUnsubscriptionVerification(UnsubscriptionVerification msg) returns UnsubscriptionVerificationSuccess {
        return UNSUBSCRIPTION_VERIFICATION_SUCCESS;
    }
};

@test:Config { 
    groups: ["unsubscriptionViaGracefulstop"]
}
function testUnsubscriptionOnGracefulStop() returns error? {
    check unsubscriptionTestListener.attach(unsubscriptionTestSubscriber, "sub");
    check unsubscriptionTestListener.'start();
    log:printInfo("[UNSUB_VER] Starting Subscriber");
    runtime:sleep(5);
    log:printInfo("[UNSUB_VER] Invoking graceful stop");
    check unsubscriptionTestListener.gracefulStop();
    runtime:sleep(5);
    log:printInfo("[UNSUB_VER] Verifying shutdown");
    test:assertTrue(isVerified());
}

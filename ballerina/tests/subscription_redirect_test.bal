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

const string HUB_REDIREC_SUCCESS_URL = "http://127.0.0.1:9193/hub1/redSuccess";
const string HUB_REDIREC_FAILURE_URL = "http://127.0.0.1:9193/hub1/redFailed";

service /hub1 on new http:Listener(9193) {
    isolated resource function post redSuccess(http:Caller caller, http:Request request) returns error? {
        http:Response res = new;
        check caller->redirect(res, http:REDIRECT_TEMPORARY_REDIRECT_307, ["http://localhost:9194/hub2/redSuccess"]);
    }

    isolated resource function post redFailed(http:Caller caller, http:Request request) returns error? {
        http:Response res = new;
        check caller->redirect(res, http:REDIRECT_TEMPORARY_REDIRECT_307, ["http://localhost:9194/hub2/redFailed"]);
    }
}

service /hub2 on new http:Listener(9194) {
    isolated resource function post redSuccess(http:Caller caller, http:Request request) returns error? {
        http:Response res = new;
        check caller->redirect(res, http:REDIRECT_TEMPORARY_REDIRECT_307, ["http://localhost:9195/hub3/redSuccess"]);
    }
    
    isolated resource function post redFailed(http:Caller caller, http:Request request) returns error? {
        http:Response res = new;
        check caller->redirect(res, http:REDIRECT_TEMPORARY_REDIRECT_307, ["http://localhost:9195/hub3/redFailed"]);
    }
}

service /hub3 on new http:Listener(9195) {
    isolated resource function post redSuccess(http:Caller caller, http:Request request) returns error? {
        check caller->respond();
    }

    isolated resource function post redFailed(http:Caller caller, http:Request request) returns error? {
        http:Response res = new;
        check caller->redirect(res, http:REDIRECT_TEMPORARY_REDIRECT_307, ["http://localhost:9196/hub4/redFailed"]);
    }
}

service /hub4 on new http:Listener(9196) {
    isolated resource function post redFailed(http:Caller caller, http:Request request) returns error? {
        check caller->respond();
    }
}

isolated function getServiceConfigwithRediects(string|[string, string] target) returns SubscriberServiceConfiguration {
    return {
        target: target,
        leaseSeconds: 36000,
        callback: CALLBACK,
        httpConfig: {
            followRedirects: {
                enabled: true, 
                maxCount: 2
            }
        }
    };
}

@test:Config { 
    groups: ["subscriptionInitiation"]
}
isolated function testSubscriptionInitiationWithRetrySuccess() returns error? {
    SubscriberServiceConfiguration config = getServiceConfigwithRediects([ HUB_REDIREC_SUCCESS_URL, COMMON_TOPIC ]);
    check subscribe(config, CALLBACK);
}

@test:Config { 
    groups: ["subscriptionInitiation"]
}
isolated function testSubscriptionInitiationWithRetryFailure() returns error? {
    string expectedMsg = "Redirection response received for subscription change request made with " +
                               "followRedirects disabled or after maxCount exceeded: Hub [" + HUB_REDIREC_FAILURE_URL + "], Topic [" +
                               COMMON_TOPIC + "]";
    SubscriberServiceConfiguration config = getServiceConfigwithRediects([ HUB_REDIREC_FAILURE_URL, COMMON_TOPIC ]);
    error? resp = subscribe(config, CALLBACK);
    test:assertTrue(resp is SubscriptionInitiationError);
    if (resp is SubscriptionInitiationError) {
        test:assertEquals(resp.message(), expectedMsg);
    }
}

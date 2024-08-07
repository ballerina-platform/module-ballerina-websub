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

listener http:Listener simpleHttpServiceListener = new (COMMON_HUB_SVC_PORT);

http:Service simpleHttpService = service object {
    isolated resource function get discovery(http:Caller caller, http:Request request) returns error? {
        http:Response response = new;
        response.addHeader("Link", string `<http://127.0.0.1:${COMMON_HUB_SVC_PORT}/common/hub>; rel=\"hub\"`);
        response.addHeader("Link", "<https://sample.topic.com>; rel=\"self\"");
        check caller->respond(response);
    }

    isolated resource function post hub(http:Caller caller, http:Request request) returns error? {
        map<string> params = check request.getFormParams();
        check caller->respond();
        string callbackUrl = params.get(HUB_CALLBACK);
        http:Client httpClient = check new (callbackUrl);
        http:Response response = check httpClient->get("/?hub.mode=subscribe&hub.topic=https://sample.topic.com&hub.challenge=1234");
    }
};

@test:BeforeSuite
function beforeSuiteFunc() returns error? {
    check simpleHttpServiceListener.attach(simpleHttpService, "/common");
    check simpleHttpServiceListener.'start();
}

@test:AfterSuite { }
function afterSuiteFunc() returns error? {
    check simpleHttpServiceListener.gracefulStop();
}

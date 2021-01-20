// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

# The caller remote functions to respond to client requests.
public client class Caller {
    private http:Caller httpCaller;

    isolated function init(http:Caller httpCaller) {
        self.httpCaller = httpCaller;
    }

    # Sends the response to the caller.
    # ```ballerina
    # error? response = caller->respond();
    # ```
    #
    # + message - The response or any payload of type `string`, `xml`, `json`, `byte[]`, `io:ReadableByteChannel`,
    #             or `mime:Entity[]`
    # + return - An `error` on failure or else `()`
    isolated remote function respond(http:ResponseMessage message = ()) returns error? {
        return self.httpCaller->respond(message);
    }
}


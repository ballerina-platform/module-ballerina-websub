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
import ballerina/jballerina.java;

isolated class HttpToWebsubAdaptor {
    isolated function init(SubscriberService serviceObj) {
        externInit(self, serviceObj);
    }

    isolated function getServiceMethodNames() returns string[] = @java:Method {
        'class: "io.ballerina.stdlib.websub.NativeHttpToWebsubAdaptor"
    } external;

    isolated function callOnSubscriptionVerificationMethod(SubscriptionVerification msg)
                                    returns SubscriptionVerificationSuccess|SubscriptionVerificationError|error = @java:Method {
        'class: "io.ballerina.stdlib.websub.NativeHttpToWebsubAdaptor"
    } external;

    isolated function callOnUnsubscriptionVerificationMethod(UnsubscriptionVerification msg)
                                    returns UnsubscriptionVerificationSuccess|UnsubscriptionVerificationError|error = @java:Method {
        'class: "io.ballerina.stdlib.websub.NativeHttpToWebsubAdaptor"
    } external;

    isolated function callOnSubscriptionDeniedMethod(SubscriptionDeniedError msg)
                                    returns Acknowledgement|error? = @java:Method {
        'class: "io.ballerina.stdlib.websub.NativeHttpToWebsubAdaptor"
    } external;

    isolated function callOnEventNotificationMethod(ContentDistributionMessage msg, http:Request request)
                                    returns Acknowledgement|SubscriptionDeletedError|error? = @java:Method {
        'class: "io.ballerina.stdlib.websub.NativeHttpToWebsubAdaptor"
    } external;
}

isolated function externInit(HttpToWebsubAdaptor adaptor, SubscriberService serviceObj) = @java:Method {
    'class: "io.ballerina.stdlib.websub.NativeHttpToWebsubAdaptor"
} external;

isolated function retrieveHttpRequest(ContentDistributionMessage msg) returns http:Request = @java:Method {
    'class: "io.ballerina.stdlib.websub.NativeHttpToWebsubAdaptor"
} external;

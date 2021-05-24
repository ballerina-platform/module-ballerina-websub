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

import ballerina/jballerina.java;

# Wrapper class used to execute sevice methods.
isolated class RequestHandler {
    private final handle ref;

    isolated function init(SubscriberService serviceObj) returns error? {
        self.ref = newRequestHandler(serviceObj);
    }

    isolated function getServiceMethodNames() returns string[] {
        return getServiceMethodNames(self.ref);
    }

    isolated function callOnSubscriptionVerificationMethod(SubscriptionVerification msg) 
                                returns SubscriptionVerificationSuccess|SubscriptionVerificationError|error {
        return callOnSubscriptionVerificationMethod(self.ref, msg);
    }

    isolated function callOnSubscriptionDeniedMethod(SubscriptionDeniedError msg) 
                                returns Acknowledgement|error? {
        return callOnSubscriptionDeniedMethod(self.ref, msg);
    }

    isolated function callOnEventNotificationMethod(ContentDistributionMessage msg) 
                                returns Acknowledgement|SubscriptionDeletedError|error? {
        return callOnEventNotificationMethod(self.ref, msg);
    }
}

isolated function newRequestHandler(SubscriberService serviceObj) returns handle = @java:Constructor {
    'class: "io.ballerina.stdlib.websub.RequestHandler"
} external;

isolated function getServiceMethodNames(handle reference) returns string[] = @java:Method {
    'class: "io.ballerina.stdlib.websub.RequestHandler"
} external;

isolated function callOnSubscriptionVerificationMethod(handle reference, SubscriptionVerification msg) 
                                returns SubscriptionVerificationSuccess|SubscriptionVerificationError|error = @java:Method {
    'class: "io.ballerina.stdlib.websub.RequestHandler"
} external;

isolated function callOnSubscriptionDeniedMethod(handle reference, SubscriptionDeniedError msg) 
                                returns Acknowledgement|error? = @java:Method {
    'class: "io.ballerina.stdlib.websub.RequestHandler"
} external;

isolated function callOnEventNotificationMethod(handle reference, ContentDistributionMessage msg) 
                                returns Acknowledgement|SubscriptionDeletedError|error? = @java:Method {
    'class: "io.ballerina.stdlib.websub.RequestHandler"
} external;

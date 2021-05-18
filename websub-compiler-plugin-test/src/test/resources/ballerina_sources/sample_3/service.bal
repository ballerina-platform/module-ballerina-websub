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

listener websub:Listener simpleListener = new (10003);

@websub:SubscriberServiceConfig{}
service /sample on simpleListener {
    isolated remote function onSubscriptionValidationDenied(websub:SubscriptionDeniedError msg) returns websub:Acknowledgement? {
        log:printDebug("onSubscriptionValidationDenied invoked");
        return websub:ACKNOWLEDGEMENT;
    }

    isolated remote function onSubscriptionVerification(websub:SubscriptionVerification msg)
                        returns websub:SubscriptionVerificationSuccess|websub:SubscriptionVerificationError {
        log:printDebug("onSubscriptionVerification invoked");
        if (msg.hubTopic == "test1") {
            return websub:SUBSCRIPTION_VERIFICATION_ERROR;
        } else {
            return websub:SUBSCRIPTION_VERIFICATION_SUCCESS;
        }
    }
}

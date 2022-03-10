// Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/log;
import ballerina/mime;
import ballerina/websub;

@websub:SubscriberServiceConfig {}
service /subscriber on new websub:Listener(9104) {
    isolated remote function onSubscriptionValidationDenied(readonly & websub:SubscriptionDeniedError msg) returns websub:Acknowledgement? {
        return websub:ACKNOWLEDGEMENT;
    }

    isolated remote function onSubscriptionVerification(readonly & websub:SubscriptionVerification msg)
                        returns websub:SubscriptionVerificationSuccess|websub:SubscriptionVerificationError {
        if (msg.hubTopic == "test1") {
            return websub:SUBSCRIPTION_VERIFICATION_ERROR;
        } else {
            return websub:SUBSCRIPTION_VERIFICATION_SUCCESS;
        }
    }

    remote function onUnsubscriptionVerification(readonly & websub:UnsubscriptionVerification msg)
                    returns websub:UnsubscriptionVerificationSuccess|websub:UnsubscriptionVerificationError {
        if (msg.hubTopic == "test1") {
            return websub:UNSUBSCRIPTION_VERIFICATION_ERROR;
        } else {
            return websub:UNSUBSCRIPTION_VERIFICATION_SUCCESS;
        }
    }

    isolated remote function onEventNotification(readonly & websub:ContentDistributionMessage event)
                        returns websub:Acknowledgement|websub:SubscriptionDeletedError? {
        match event.contentType {
            mime:APPLICATION_FORM_URLENCODED => {
                map<string> content = <map<string>>event.content;
                log:printInfo("URL encoded content received ", content = content);
            }
            _ => {
                log:printDebug("onEventNotification invoked ", contentDistributionMessage = event);
            }
        }

        return websub:ACKNOWLEDGEMENT;
    }
}

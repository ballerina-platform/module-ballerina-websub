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

import ballerina/log;
import ballerina/test;
import ballerina/http;

listener Listener multiServiceListener = new(9096);

@SubscriberServiceConfig {} 
service /subscriberOne on multiServiceListener {
    remote function onSubscriptionValidationDenied(SubscriptionDeniedError msg) returns Acknowledgement? {
        log:printDebug("onSubscriptionValidationDenied invoked");
        Acknowledgement ack = {
                  headers: {"header1": "value"},
                  body: {"formparam1": "value1"}
        };
        return ack;
    }

    remote function onSubscriptionVerification(SubscriptionVerification msg)
                        returns SubscriptionVerificationSuccess|SubscriptionVerificationError {
        log:printDebug("onSubscriptionVerification invoked");
        if (msg.hubTopic == "test1") {
            return error SubscriptionVerificationError("Hub topic not supported");
        } else {
            return {};
        }
      }

    remote function onEventNotification(ContentDistributionMessage event) 
                        returns Acknowledgement|SubscriptionDeletedError? {
        log:printDebug("onEventNotification invoked ", contentDistributionMessage = event);
        return {};
    }
}

@SubscriberServiceConfig {} 
service /subscriberTwo on multiServiceListener {
    remote function onSubscriptionValidationDenied(SubscriptionDeniedError msg) returns Acknowledgement? {
        log:printDebug("onSubscriptionValidationDenied invoked");
        Acknowledgement ack = {
                  headers: {"header1": "value"},
                  body: {"formparam1": "value1"}
        };
        return ack;
    }

    remote function onSubscriptionVerification(SubscriptionVerification msg)
                        returns SubscriptionVerificationSuccess|SubscriptionVerificationError {
        log:printDebug("onSubscriptionVerification invoked");
        if (msg.hubTopic == "test1") {
            return error SubscriptionVerificationError("Hub topic not supported");
        } else {
            return {};
        }
      }

    remote function onEventNotification(ContentDistributionMessage event) 
                        returns Acknowledgement|SubscriptionDeletedError? {
        log:printDebug("onEventNotification invoked ", contentDistributionMessage = event);
        return {};
    }
}

http:Client clientEp1 = checkpanic new("http://localhost:9096/subscriberOne");

@test:Config { 
    groups: ["multiServiceListener"]
}
function testOnSubscriptionValidationWithMultiServiceListener() returns @tainted error? {
    http:Request request = new;

    http:Response response = check clientEp1->get("/?hub.mode=denied&hub.reason=justToTest", request);
    test:assertEquals(response.statusCode, 200);
}
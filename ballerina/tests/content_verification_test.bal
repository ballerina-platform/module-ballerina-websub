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
import ballerina/mime;
import ballerina/http;
import ballerina/crypto;
import ballerina/test;

final string hashKey = "testKey";
isolated boolean urlEncodedContentVerified = false;
isolated boolean jsonContentVerified = false;
isolated boolean xmlContentVerified = false;

isolated function updateJsonContentVerified(boolean state) {
    lock {
        jsonContentVerified = state;
    }
}

isolated function isJsonContentVerified() returns boolean {
    lock {
        return jsonContentVerified;
    }
}

isolated function updateXmlContentVerified(boolean state) {
    lock {
        xmlContentVerified = state;
    }
}

isolated function isXmlContentVerified() returns boolean {
    lock {
        return xmlContentVerified;
    }
}

isolated function updateUrlEncodedContentVerified(boolean state) {
    lock {
        urlEncodedContentVerified = state;
    }
}

isolated function isUrlEncodedContentVerified() returns boolean {
    lock {
        return urlEncodedContentVerified;
    }
}

@SubscriberServiceConfig {
    secret: hashKey,
    unsubscribeOnShutdown: false
} 
service /subscriber on new Listener(9098) {
    remote function onEventNotification(ContentDistributionMessage event) 
                        returns Acknowledgement|SubscriptionDeletedError? {
        log:printInfo("[VERIFICATION] onEventNotification invoked ", contentDistributionMessage = event);
        match event.contentType {
            mime:APPLICATION_FORM_URLENCODED => {
                updateUrlEncodedContentVerified(true);
            }
            mime:APPLICATION_JSON => {
                updateJsonContentVerified(true);
            }
            mime:APPLICATION_XML => {
                updateXmlContentVerified(true);
            }
            _ => { }
        }
        
        return ACKNOWLEDGEMENT;
    }
}

final http:Client contentVerificationClient = check new("http://localhost:9098/subscriber");

@test:Config {
    groups: ["contentVerification"]
 }
isolated function testOnEventNotificationSuccessForContentVerification() returns error? {
    http:Request request = new;
    json payload =  {"action":"publish","mode":"remote-hub"};
    byte[] payloadHash = check retrievePayloadSignature(hashKey, payload);
    request.setHeader("X-Hub-Signature", string `sha256=${payloadHash.toBase16()}`);
    request.setPayload(payload);
    http:Response response = check contentVerificationClient->post("/", request);
    test:assertEquals(response.statusCode, 202);
    test:assertTrue(isJsonContentVerified());
}


@test:Config {
    groups: ["contentVerification"]
}
isolated function testOnEventNotificationSuccessXmlForContentVerification() returns error? {
    http:Request request = new;
    xml payload = xml `<body><action>publish</action></body>`;
    byte[] payloadHash = check retrievePayloadSignature(hashKey, payload);
    request.setHeader("X-Hub-Signature", string `sha256=${payloadHash.toBase16()}`);
    request.setPayload(payload);
    http:Response response = check contentVerificationClient->post("/", request);
    test:assertEquals(response.statusCode, 202);
    test:assertTrue(isXmlContentVerified());
}

@test:Config {
    groups: ["contentVerification"]
}
isolated function testOnEventNotificationSuccessForUrlEncodedForContentVerification() returns error? {
    http:Request request = new;
    string payload = "param1=value1&param2=value2";
    byte[] payloadHash = check retrievePayloadSignature(hashKey, payload);
    request.setTextPayload(payload);
    request.setHeader("X-Hub-Signature", string `sha256=${payloadHash.toBase16()}`);
    check request.setContentType(mime:APPLICATION_FORM_URLENCODED);
    http:Response response = check contentVerificationClient->post("", request);
    test:assertEquals(response.statusCode, 202);
    test:assertTrue(isUrlEncodedContentVerified());
}

@test:Config {
    groups: ["contentVerification"]
}
function testOnEventNotificationSuccessWithoutContentSignature() returns @tainted error? {
    http:Request request = new;
    string payload = "param1=value1&param2=value2";
    request.setTextPayload(payload);
    check request.setContentType(mime:APPLICATION_FORM_URLENCODED);
    http:Response response = check contentVerificationClient->post("", request);
    test:assertEquals(response.statusCode, 202);
}

@test:Config {
    groups: ["contentVerification"]
}
function testOnEventNotificationSuccessWithEmptySignature() returns @tainted error? {
    http:Request request = new;
    string payload = "param1=value1&param2=value2";
    request.setTextPayload(payload);
    request.setHeader("X-Hub-Signature", "");
    check request.setContentType(mime:APPLICATION_FORM_URLENCODED);
    http:Response response = check contentVerificationClient->post("", request);
    test:assertEquals(response.statusCode, 202);
}

isolated function retrievePayloadSignature(string 'key, string|xml|json|byte[] payload) returns byte[]|error {
    byte[] keyArr = 'key.toBytes();
    if (payload is byte[]) {
        return crypto:hmacSha256(payload, keyArr);
    } else if (payload is string) {
        byte[] inputArr = payload.toBytes();
        return crypto:hmacSha256(inputArr, keyArr);
    } else if (payload is xml) {
        byte[] inputArr = payload.toString().toBytes();
        return crypto:hmacSha256(inputArr, keyArr);   
    } else if (payload is map<string>) {
        byte[] inputArr = payload.toString().toBytes();
        return crypto:hmacSha256(inputArr, keyArr); 
    } else {
        byte[] inputArr = payload.toJsonString().toBytes();
        return crypto:hmacSha256(inputArr, keyArr);
    }
}

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

readonly & string hashKey = "testKey";
boolean urlEncodedContentVerified = false;
boolean jsonContentVerified = false;
boolean xmlContentVerified = false;

@SubscriberServiceConfig {
    secret: hashKey
} 
service /subscriber on new Listener(9098) {
    remote function onEventNotification(ContentDistributionMessage event) 
                        returns Acknowledgement|SubscriptionDeletedError? {
        log:printInfo("[VERIFICATION] onEventNotification invoked ", contentDistributionMessage = event);
        match event.contentType {
            mime:APPLICATION_FORM_URLENCODED => {
                urlEncodedContentVerified = true;
            }
            mime:APPLICATION_JSON => {
                jsonContentVerified = true;
            }
            mime:APPLICATION_XML => {
                xmlContentVerified = true;
            }
            _ => { }
        }
        
        return ACKNOWLEDGEMENT;
    }
}

http:Client contentVerificationClient = check new("http://localhost:9098/subscriber");

@test:Config {
    groups: ["contentVerification"]
 }
function testOnEventNotificationSuccessForContentVerification() returns @tainted error? {
    http:Request request = new;
    json payload =  {"action":"publish","mode":"remote-hub"};
    byte[] payloadHash = check retrievePayloadSignature(hashKey, payload);
    request.setHeader("X-Hub-Signature", string`sha256=${payloadHash.toBase16()}`);
    request.setPayload(payload);
    http:Response response = check contentVerificationClient->post("/", request);
    test:assertEquals(response.statusCode, 202);
    test:assertTrue(jsonContentVerified);
}


@test:Config {
    groups: ["contentVerification"]
}
function testOnEventNotificationSuccessXmlForContentVerification() returns @tainted error? {
    http:Request request = new;
    xml payload = xml `<body><action>publish</action></body>`;
    byte[] payloadHash = check retrievePayloadSignature(hashKey, payload);
    request.setHeader("X-Hub-Signature", string`sha256=${payloadHash.toBase16()}`);
    request.setPayload(payload);
    http:Response response = check contentVerificationClient->post("/", request);
    test:assertEquals(response.statusCode, 202);
    test:assertTrue(xmlContentVerified);
}

@test:Config {
    groups: ["contentVerification"]
}
function testOnEventNotificationSuccessForUrlEncodedForContentVerification() returns @tainted error? {
    http:Request request = new;
    string payload = "param1=value1&param2=value2";
    byte[] payloadHash = check retrievePayloadSignature(hashKey, payload);
    request.setTextPayload(payload);
    request.setHeader("X-Hub-Signature", string`sha256=${payloadHash.toBase16()}`);
    check request.setContentType(mime:APPLICATION_FORM_URLENCODED);
    http:Response response = check contentVerificationClient->post("", request);
    test:assertEquals(response.statusCode, 202);
    test:assertTrue(urlEncodedContentVerified);
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

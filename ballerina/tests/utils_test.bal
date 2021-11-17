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
import ballerina/mime;

const string HASH_KEY = "secret";

@test:Config { 
    groups: ["contentHashRetrieval"]
}
isolated function testContentHashForSha1() returns error? {
    byte[] hashedContent = check retrieveContentHash(SHA1, HASH_KEY, "This is sample content");
    test:assertTrue(hashedContent.length() > 0);
}

@test:Config { 
    groups: ["contentHashRetrieval"]
}
isolated function testContentHashForSha256() returns error? {
    byte[] hashedContent = check retrieveContentHash(SHA_256, HASH_KEY, "This is sample content");
    test:assertTrue(hashedContent.length() > 0);
}

@test:Config { 
    groups: ["contentHashRetrieval"]
}
isolated function testContentHashForSha384() returns error? {
    byte[] hashedContent = check retrieveContentHash(SHA_384, HASH_KEY, "This is sample content");
    test:assertTrue(hashedContent.length() > 0);
}

@test:Config { 
    groups: ["contentHashRetrieval"]
}
isolated function testContentHashForSha512() returns error? {
    byte[] hashedContent = check retrieveContentHash(SHA_512, HASH_KEY, "This is sample content");
    test:assertTrue(hashedContent.length() > 0);
}

@test:Config { 
    groups: ["contentHashRetrieval"]
}
isolated function testContentHashError() returns error? {
    byte[]|error hashedContent = retrieveContentHash("xyz", HASH_KEY, "This is sample content");
    string expectedErrorMsg = "Unrecognized hashning-method [xyz] found";
    if (hashedContent is error) {
        test:assertEquals(hashedContent.message(), expectedErrorMsg);
    } else {
        test:assertFail("Content hash generation not properly working for unidentified hash-method");
    }
}

SubscriberService validSubscriberServiceDeclaration = @SubscriberServiceConfig { target: "http://0.0.0.0:9191/common/discovery", leaseSeconds: 36000, unsubscribeOnShutdown: false } 
                              service object {
    isolated remote function onEventNotification(ContentDistributionMessage event) 
                        returns Acknowledgement|SubscriptionDeletedError? {
        return ACKNOWLEDGEMENT;
    }
};

@test:Config { 
    groups: ["serviceAnnotationRetrieval"]
}
function testSubscriberServiceAnnotationRetrievalSuccess() returns error? {
    SubscriberServiceConfiguration? configuration = retrieveSubscriberServiceAnnotations(validSubscriberServiceDeclaration);
    test:assertTrue(configuration is SubscriberServiceConfiguration, "service annotation retrieval failed for valid service declaration");
}

SubscriberService invalidSubscriberServiceDeclaration = service object {
    isolated remote function onEventNotification(ContentDistributionMessage event) 
                        returns Acknowledgement|SubscriptionDeletedError? {
        return ACKNOWLEDGEMENT;
    }
};

@test:Config { 
    groups: ["serviceAnnotationRetrieval"]
}
function testSubscriberServiceAnnotationRetrievalFailure() returns error? {
    SubscriberServiceConfiguration? configuration = retrieveSubscriberServiceAnnotations(invalidSubscriberServiceDeclaration);
    test:assertTrue(configuration is (), "service annotation retrieval success for invalid service declaration");
}

@test:Config { 
    groups: ["servicePathGenerationEnabled"]
}
isolated function testServicePathGenerationEnabledForEmptyCallbackAndServicePathProvided() {
    boolean shouldGenerate = shouldUseGeneratedServicePath({}, ());
    test:assertTrue(shouldGenerate);
}

@test:Config {
    groups: ["servicePathGenerationEnabled"]
}
isolated function testServicePathGenerationEnabledForCallbackAndEmptyServicePathProvided() {
    boolean shouldGenerate = shouldUseGeneratedServicePath({ callback: "https://sample.com/sub" }, ());
    test:assertFalse(shouldGenerate);
}

@test:Config {
    groups: ["servicePathGenerationEnabled"]
}
isolated function testServicePathGenerationEnabledForNoCallbackAndEmptyServicePathArrProvided() {
    boolean shouldGenerate = shouldUseGeneratedServicePath({}, []);
    test:assertTrue(shouldGenerate);
}

@test:Config {
    groups: ["servicePathGenerationEnabled"]
}
isolated function testServicePathGenerationEnabledForCallbackAndEmptyServicePathArrProvided() {
    boolean shouldGenerate = shouldUseGeneratedServicePath({ callback: "https://sample.com/sub" }, []);
    test:assertFalse(shouldGenerate);
}

@test:Config {
    groups: ["servicePathGenerationEnabled"]
}
isolated function testServicePathGenerationEnabledWithCallbackAppendingAndEmptyServicePathProvided() {
    boolean shouldGenerate = shouldUseGeneratedServicePath({ callback: "https://sample.com/sub", appendServicePath: true }, ());
    test:assertTrue(shouldGenerate);
}

@test:Config {
    groups: ["servicePathGenerationEnabled"]
}
isolated function testServicePathGenerationEnabledWithoutCallbackAppendingAndEmptyServicePathProvided() {
    boolean shouldGenerate = shouldUseGeneratedServicePath({ callback: "https://sample.com/sub" }, ());
    test:assertFalse(shouldGenerate);
}

@test:Config {
    groups: ["servicePathGenerationEnabled"]
}
isolated function testServicePathGenerationEnabledWithCallbackAppendingAndEmptyServicePathArrProvided() {
    boolean shouldGenerate = shouldUseGeneratedServicePath({ callback: "https://sample.com/sub", appendServicePath: true }, []);
    test:assertTrue(shouldGenerate);
}

@test:Config {
    groups: ["servicePathGenerationEnabled"]
}
isolated function testServicePathGenerationEnabledWithoutCallbackAppendingAndEmptyServicePathArrProvided() {
    boolean shouldGenerate = shouldUseGeneratedServicePath({ callback: "https://sample.com/sub" }, []);
    test:assertFalse(shouldGenerate);
}

@test:Config {
    groups: ["servicePathGenerationEnabled"]
}
isolated function testServicePathGenerationEnabledWithCallbackAppendingAndServicePathProvided() {
    boolean shouldGenerate = shouldUseGeneratedServicePath({ callback: "https://sample.com/sub", appendServicePath: true }, "/sub");
    test:assertFalse(shouldGenerate);
}

@test:Config {
    groups: ["servicePathGenerationEnabled"]
}
isolated function testServicePathGenerationEnabledWithCallbackAppendingAndServicePathArrProvided() {
    boolean shouldGenerate = shouldUseGeneratedServicePath({ callback: "https://sample.com/sub", appendServicePath: true }, ["pub", "sub"]);
    test:assertFalse(shouldGenerate);
}

@test:Config {
    groups: ["completeServicePathRetrieval"]
}
isolated function testServicePathRetrievalForEmptyServicePath() {
    var servicePath = retrieveCompleteServicePath(());
    if servicePath is error {
        test:assertEquals(servicePath.message(), "Could not find the generated service path");
    } else {
        test:assertFail("Retrieved a service-path for a errorneous scenario");
    }
}

@test:Config {
    groups: ["completeServicePathRetrieval"]
}
isolated function testServicePathRetrievalForEmptyServicePathArr() {
    var servicePath = retrieveCompleteServicePath([]);
    if servicePath is error {
        test:assertEquals(servicePath.message(), "Could not find the generated service path");
    } else {
        test:assertFail("Retrieved a service-path for a errorneous scenario");
    }
}

@test:Config { 
    groups: ["completeServicePathRetrieval"]
}
isolated function testCompleteServicePathRetrievalWithString() returns error? {
    string expectedServicePath = "subscriber";
    string generatedServicePath = check retrieveCompleteServicePath("subscriber");
    test:assertEquals(generatedServicePath, expectedServicePath, "Generated service-path does not matched expected service-path"); 
}

@test:Config { 
    groups: ["completeServicePathRetrieval"]
}
isolated function testCompleteServicePathRetrievalWithStringArray() returns error? {
    string expectedServicePath = "subscriber/foo/bar";
    string generatedServicePath = check retrieveCompleteServicePath(["subscriber", "foo", "bar"]);
    test:assertEquals(generatedServicePath, expectedServicePath, "Generated service-path does not matched expected service-path"); 
}

@test:Config { 
    groups: ["retrieveCallbackUrl"]
}
isolated function testCallbackUrlRetrievalWithNoCallback() returns error? {
    string expectedCallbackUrl = "http://0.0.0.0:9090/subscriber";
    SubscriberServiceConfiguration config = {};
    string retrievedCallbackUrl = constructCallbackUrl(config, 9090, retrieveInfrdListenerConfig({}), "subscriber", false);
    test:assertEquals(retrievedCallbackUrl, expectedCallbackUrl, "Retrieved callback url does not match expected callback url");
}

@test:Config { 
    groups: ["retrieveCallbackUrl"]
}
isolated function testCallbackUrlRetrievalWithCallbackAppendingDisabled() returns error? {
    string expectedCallbackUrl = "http://0.0.0.0:9090/subscriber";
    SubscriberServiceConfiguration config = {
        callback: "http://0.0.0.0:9090/subscriber"
    };
    string retrievedCallbackUrl = constructCallbackUrl(config, 9090, retrieveInfrdListenerConfig({}), "subscriber", false);
    test:assertEquals(retrievedCallbackUrl, expectedCallbackUrl, "Retrieved callback url does not match expected callback url");
}

@test:Config { 
    groups: ["retrieveCallbackUrl"]
}
isolated function testCallbackUrlRetrievalWithCallbackAppendingEnabled() returns error? {
    string expectedCallbackUrl = "http://0.0.0.0:9090/subscriber/foo";
    SubscriberServiceConfiguration config = {
        callback: "http://0.0.0.0:9090",
        appendServicePath: true
    };
    string retrievedCallbackUrl = constructCallbackUrl(config, 9090, retrieveInfrdListenerConfig({}), "subscriber/foo", false);
    test:assertEquals(retrievedCallbackUrl, expectedCallbackUrl, "Retrieved callback url does not match expected callback url");
}

@test:Config { 
    groups: ["callbackUrlGeneration"]
}
isolated function testCallbackUrlGenerationHttpsWithNoHostConfig() returns error? {
    http:ListenerConfiguration listenerConfig = {
        secureSocket: {
            key: {
                path: "tests/resources/ballerinaKeystore.pkcs12",
                password: "ballerina"
            }
        }
    };
    SubscriberServiceConfiguration config = {};
    string expectedCallbackUrl = "https://0.0.0.0:9090/subscriber";
    string generatedCallbackUrl = constructCallbackUrl(config, 9090, retrieveInfrdListenerConfig(listenerConfig), "subscriber", false);
    test:assertEquals(generatedCallbackUrl, expectedCallbackUrl, "Generated callback url does not match expected callback url");
}

@test:Config { 
    groups: ["callbackUrlGeneration"]
}
isolated function testCallbackUrlGenerationHttpWithNoHostConfig() returns error? {
    http:ListenerConfiguration listenerConfig = {};
    string expectedCallbackUrl = "http://0.0.0.0:9090/subscriber";
    SubscriberServiceConfiguration config = {};
    string generatedCallbackUrl = constructCallbackUrl(config, 9090, retrieveInfrdListenerConfig(listenerConfig), "subscriber", false);
    test:assertEquals(generatedCallbackUrl, expectedCallbackUrl, "Generated callback url does not match expected callback url");
}

@test:Config { 
    groups: ["callbackUrlGeneration"]
}
isolated function testCallbackUrlGenerationHttpsWithHostConfig() returns error? {
    http:ListenerConfiguration listenerConfig = {
        host: "192.168.1.1",
        secureSocket: {
            key: {
                path: "tests/resources/ballerinaKeystore.pkcs12",
                password: "ballerina"
            }
        }
    };
    string expectedCallbackUrl = "https://192.168.1.1:9090/subscriber";
    SubscriberServiceConfiguration config = {};
    string generatedCallbackUrl = constructCallbackUrl(config, 9090, retrieveInfrdListenerConfig(listenerConfig), "subscriber", false);
    test:assertEquals(generatedCallbackUrl, expectedCallbackUrl, "Generated callback url does not match expected callback url");
}

@test:Config { 
    groups: ["callbackUrlGeneration"]
}
isolated function testCallbackUrlGenerationHttpWithHostConfig() returns error? {
    http:ListenerConfiguration listenerConfig = {
        host: "192.168.1.1"
    };
    string expectedCallbackUrl = "http://192.168.1.1:9090/subscriber";
    SubscriberServiceConfiguration config = {};
    string generatedCallbackUrl = constructCallbackUrl(config, 9090, retrieveInfrdListenerConfig(listenerConfig), "subscriber", false);
    test:assertEquals(generatedCallbackUrl, expectedCallbackUrl, "Generated callback url does not match expected callback url");
}

@test:Config { 
    groups: ["callbackUrlGeneration"]
}
isolated function testCallbackUrlForArrayTypeServicePath() returns error? {
    http:ListenerConfiguration listenerConfig = {
        host: "192.168.1.1"
    };
    string expectedCallbackUrl = "http://192.168.1.1:9090/subscriber/foo/bar";
    SubscriberServiceConfiguration config = {};
    string generatedCallbackUrl = constructCallbackUrl(config, 9090, retrieveInfrdListenerConfig(listenerConfig), "subscriber/foo/bar", false);
    test:assertEquals(generatedCallbackUrl, expectedCallbackUrl, "Generated callback url does not match expected callback url");   
}

isolated function retrieveInfrdListenerConfig(http:ListenerConfiguration config) returns http:InferredListenerConfiguration {
    return {
        host: config.host,
        http1Settings: config.http1Settings,
        secureSocket: config.secureSocket,
        httpVersion: config.httpVersion,
        timeout: config.timeout,
        server: config.server,
        requestLimits: config.requestLimits
    };
}

listener Listener utilTestListener = new (9101);

@SubscriberServiceConfig{
    unsubscribeOnShutdown: false
}
service /utilTest1 on utilTestListener {
    isolated remote function onEventNotification(ContentDistributionMessage event) returns Acknowledgement {
        string|http:HeaderNotFoundError headerValue = getHeader(event, "Custom-Header");
        if (headerValue is string) {
            return {
                body: {
                    "Custom-Header": headerValue
                }
            };
        } else {
            return {
                body: {
                    "Message": "Header Not Found"
                }
            };
        }
    }
}

@SubscriberServiceConfig{
    unsubscribeOnShutdown: false
}
service /utilTest2 on utilTestListener {
    isolated remote function onEventNotification(ContentDistributionMessage event) returns Acknowledgement {
        string[]|http:HeaderNotFoundError values = getHeaders(event, "Custom-Header");
        if (values is string[]) {
            string concatenatedValues = string:'join(",", ...values);
            return {
                body: {
                    "Custom-Header": concatenatedValues
                }
            };
        } else {
            return {
                body: {
                    "Message": "Header Not Found"
                }
            };
        }
    }
}

final http:Client headerUtilTestClient1 = check new ("http://localhost:9101/utilTest1");

@test:Config { 
    groups: ["requestHeader"]
}
isolated function testRequestHeaderRetrievalWithStandardHeadeName() returns error? {
    http:Request request = new;
    request.setHeader("Custom-Header", "Custom Header Value");
    request.setTextPayload("This is a sample message");
    http:Response response = check headerUtilTestClient1->post("/", request);
    string payload = check response.getTextPayload();
    map<string> decodedPayload = decodeResponseBody(payload);
    test:assertEquals(response.statusCode, 202);
    test:assertEquals(response.getContentType(), mime:APPLICATION_FORM_URLENCODED);
    test:assertEquals(decodedPayload.get("Custom-Header"), "Custom Header Value");
}

@test:Config { 
    groups: ["requestHeader"]
}
isolated function testRequestHeaderRetrievalWithNonStandardHeadeName() returns error? {
    http:Request request = new;
    request.setHeader("custoM-HeaDer", "Custom Header Value");
    request.setTextPayload("This is a sample message");
    http:Response response = check headerUtilTestClient1->post("/", request);
    string payload = check response.getTextPayload();
    map<string> decodedPayload = decodeResponseBody(payload);
    test:assertEquals(response.statusCode, 202);
    test:assertEquals(response.getContentType(), mime:APPLICATION_FORM_URLENCODED);
    test:assertEquals(decodedPayload.get("Custom-Header"), "Custom Header Value");
}

@test:Config { 
    groups: ["requestHeader"]
}
isolated function testRequestHeaderRetrievalWithoutHeaderValue() returns error? {
    http:Request request = new;
    request.setTextPayload("This is a sample message");
    http:Response response = check headerUtilTestClient1->post("/", request);
    string payload = check response.getTextPayload();
    map<string> decodedPayload = decodeResponseBody(payload);
    test:assertEquals(response.statusCode, 202);
    test:assertEquals(response.getContentType(), mime:APPLICATION_FORM_URLENCODED);
    test:assertEquals(decodedPayload.get("Message"), "Header Not Found");
}

final http:Client headerUtilTestClient2 = check new ("http://localhost:9101/utilTest2");

@test:Config { 
    groups: ["requestHeader"]
}
isolated function testRequestHeadersRetrievalWithStandardHeadeName() returns error? {
    http:Request request = new;
    request.addHeader("Custom-Header", "Val1");
    request.addHeader("Custom-Header", "Val2");
    request.addHeader("Custom-Header", "Val3");
    request.setTextPayload("This is a sample message");
    http:Response response = check headerUtilTestClient2->post("/", request);
    string payload = check response.getTextPayload();
    map<string> decodedPayload = decodeResponseBody(payload);
    test:assertEquals(response.statusCode, 202);
    test:assertEquals(response.getContentType(), mime:APPLICATION_FORM_URLENCODED);
    test:assertEquals(decodedPayload.get("Custom-Header"), "Val1,Val2,Val3");
}

@test:Config { 
    groups: ["requestHeader"]
}
isolated function testRequestHeadersRetrievalWithNonStandardHeadeName() returns error? {
    http:Request request = new;
    request.addHeader("custoM-HeaDer", "Val1");
    request.addHeader("custoM-HeaDer", "Val2");
    request.addHeader("custoM-HeaDer", "Val3");
    request.setTextPayload("This is a sample message");
    http:Response response = check headerUtilTestClient2->post("/", request);
    string payload = check response.getTextPayload();
    map<string> decodedPayload = decodeResponseBody(payload);
    test:assertEquals(response.statusCode, 202);
    test:assertEquals(response.getContentType(), mime:APPLICATION_FORM_URLENCODED);
    test:assertEquals(decodedPayload.get("Custom-Header"), "Val1,Val2,Val3");
}

@test:Config { 
    groups: ["requestHeader"]
}
isolated function testRequestHeadersRetrievalWithoutHeaderValue() returns error? {
    http:Request request = new;
    request.setTextPayload("This is a sample message");
    http:Response response = check headerUtilTestClient2->post("/", request);
    string payload = check response.getTextPayload();
    map<string> decodedPayload = decodeResponseBody(payload);
    test:assertEquals(response.statusCode, 202);
    test:assertEquals(response.getContentType(), mime:APPLICATION_FORM_URLENCODED);
    test:assertEquals(decodedPayload.get("Message"), "Header Not Found");
}

@test:Config { 
    groups: ["httpClientRetrieval"]
}
isolated function testRetrieveHttpClientWithConfig() {
    http:ClientConfiguration httpsConfig = {
        secureSocket: {
            cert: {
                path: "tests/resources/ballerinaTruststore.pkcs12",
                password: "ballerina"
            }
        }
    };
    var clientEp = retrieveHttpClient("https://test.com/sample", httpsConfig);
    test:assertTrue(clientEp is http:Client);
}

@test:Config { 
    groups: ["httpClientRetrieval"]
}
isolated function testRetrieveHttpClientWithoutConfig() {
    var clientEp = retrieveHttpClient("https://test.com/sample", ());
    test:assertTrue(clientEp is http:Client);
}

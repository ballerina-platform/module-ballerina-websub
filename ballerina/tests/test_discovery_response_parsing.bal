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
import ballerina/test;

final string HUB_ONE = "https://hub.ballerina.com";
final string HUB_TWO = "https://two.hub.ballerina.com";
final string HUB_THREE = "https://three.hub.ballerina.com";

final string TOPIC_ONE = "https://topic.ballerina.com";

@test:Config {}
isolated function testTopicAndSingleHubAsSingleLinkHeader() returns error? {
    http:Response response = new;
    response.addHeader("Link", "<" + HUB_ONE + ">; rel=\"hub\", <" + TOPIC_ONE + ">; rel=\"self\"");
    [string, string[]] results = check extractTopicAndHubUrls(response);
    string topic = results[0];
    string[] hubs = results[1];
    test:assertEquals(topic, TOPIC_ONE, msg = "incorrect topic extraction from discovery response");
    test:assertEquals(hubs.length(), 1, msg = "incorrect no. of hubs extracted from discovery response");
    test:assertEquals(hubs[0], HUB_ONE, msg = "incorrect hub extraction from discovery response");
}

@test:Config {
    dependsOn: [testTopicAndSingleHubAsSingleLinkHeader]
}
isolated function testTopicAndSingleHubAsMultipleLinkHeaders() returns error? {
    http:Response response = new;
    response.addHeader("Link", "<" + HUB_ONE + ">; rel=\"hub\"");
    response.addHeader("Link", "<" + TOPIC_ONE + ">; rel=\"self\"");
    [string, string[]] results = check extractTopicAndHubUrls(response);
    string topic = results[0];
    string[] hubs = results[1];
    test:assertEquals(topic, TOPIC_ONE, msg = "incorrect topic extraction from discovery response");
    test:assertEquals(hubs.length(), 1, msg = "incorrect no. of hubs extracted from discovery response");
    test:assertEquals(hubs[0], HUB_ONE, msg = "incorrect hub extraction from discovery response");
}

@test:Config {
    dependsOn: [testTopicAndSingleHubAsMultipleLinkHeaders]
}
isolated function testTopicAndMultipleHubsAsSingleLinkHeader() returns error? {
    http:Response response = new;
    response.addHeader("Link", "<" + HUB_ONE + ">; rel=\"hub\", <" + HUB_TWO + ">; rel=\"hub\", <" + HUB_THREE +
            ">; rel=\"hub\", <" + TOPIC_ONE + ">; rel=\"self\"");

    [string, string[]] results = check extractTopicAndHubUrls(response);
    string topic = results[0];
    string[] hubs = results[1];
    test:assertEquals(topic, TOPIC_ONE, msg = "incorrect topic extraction from discovery response");
    test:assertEquals(hubs.length(), 3, msg = "incorrect no. of hubs extracted from discovery response");
    test:assertEquals(hubs[0], HUB_ONE, msg = "incorrect hub extraction from discovery response");
    test:assertEquals(hubs[1], HUB_TWO, msg = "incorrect hub extraction from discovery response");
    test:assertEquals(hubs[2], HUB_THREE, msg = "incorrect hub extraction from discovery response");
}

@test:Config {
    dependsOn: [testTopicAndMultipleHubsAsSingleLinkHeader]
}
isolated function testTopicAndMultipleHubsAsMultipleLinkHeaders() returns error? {
    http:Response response = new;
    response.addHeader("Link", "<" + HUB_ONE + ">; rel=\"hub\"");
    response.addHeader("Link", "<" + TOPIC_ONE + ">; rel=\"self\"");
    response.addHeader("Link", "<" + HUB_TWO + ">; rel=\"hub\"");
    response.addHeader("Link", "<" + HUB_THREE + ">; rel=\"hub\"");

    [string, string[]] results = check extractTopicAndHubUrls(response);
    string topic = results[0];
    string[] hubs = results[1];
    test:assertEquals(topic, TOPIC_ONE, msg = "incorrect topic extraction from discovery response");
    test:assertEquals(hubs.length(), 3, msg = "incorrect no. of hubs extracted from discovery response");
    test:assertEquals(hubs[0], HUB_ONE, msg = "incorrect hub extraction from discovery response");
    test:assertEquals(hubs[1], HUB_TWO, msg = "incorrect hub extraction from discovery response");
    test:assertEquals(hubs[2], HUB_THREE, msg = "incorrect hub extraction from discovery response");
}

@test:Config {
    dependsOn: [testTopicAndMultipleHubsAsMultipleLinkHeaders]
}
isolated function testMissingTopicWithSingleLinkHeader() returns error? {
    http:Response response = new;
    response.addHeader("Link", "<" + HUB_ONE + ">; rel=\"hub\", <" + TOPIC_ONE + ">; rel=\"not_self\"");
    [string, string[]]|error results = extractTopicAndHubUrls(response);
    if (results is error) {
        string expectedErrMsg = "Hub and/or Topic URL(s) not identified in link header of discovery response";
        test:assertEquals(results.message(), expectedErrMsg, msg = "invalid error message on unavailable topic");
    } else {
        test:assertFail(msg = "expected: {ballerina/websub}WebSubError, not found");
    }
}

@test:Config {
    dependsOn: [testMissingTopicWithSingleLinkHeader]
}
isolated function testMissingTopicWithMultipleLinkHeaders() returns error? {
    http:Response response = new;
    response.addHeader("Link", "<" + HUB_ONE + ">; rel=\"hub\"");
    response.addHeader("Link", "<" + TOPIC_ONE + ">; rel=\"not_self\"");
    [string, string[]]|error results = extractTopicAndHubUrls(response);
    if (results is error) {
        string expectedErrMsg = "Hub and/or Topic URL(s) not identified in link header of discovery response";
        test:assertEquals(results.message(), expectedErrMsg, msg = "invalid error message on unavailable topic");
    } else {
        test:assertFail(msg = "expected: {ballerina/websub}WebSubError, not found");
    }
}

@test:Config {
    dependsOn: [testMissingTopicWithMultipleLinkHeaders]
}
isolated function testMissingHubWithSingleLinkHeader() returns error? {
    http:Response response = new;
    response.addHeader("Link", "<" + HUB_ONE + ">; rel=\"not_hub\", <" + TOPIC_ONE + ">; rel=\"self\"");
    [string, string[]]|error results = extractTopicAndHubUrls(response);
    if (results is error) {
        string expectedErrMsg = "Hub and/or Topic URL(s) not identified in link header of discovery response";
        test:assertEquals(results.message(), expectedErrMsg, msg = "invalid error message on unavailable topic");
    } else {
        test:assertFail(msg = "expected: {ballerina/websub}WebSubError, not found");
    }
}

@test:Config {
    dependsOn: [testMissingHubWithSingleLinkHeader]
}
isolated function testMissingHubWithMultipleLinkHeaders() returns error? {
    http:Response response = new;
    response.addHeader("Link", "<" + HUB_ONE + ">; rel=\"not_hub\"");
    response.addHeader("Link", "<" + TOPIC_ONE + ">; rel=\"self\"");
    [string, string[]]|error results = extractTopicAndHubUrls(response);
    if (results is error) {
        string expectedErrMsg = "Hub and/or Topic URL(s) not identified in link header of discovery response";
        test:assertEquals(results.message(), expectedErrMsg, msg = "invalid error message on unavailable topic");
    } else {
        test:assertFail(msg = "expected: {ballerina/websub}WebSubError, not found");
    }
}

@test:Config {
    dependsOn: [testMissingHubWithMultipleLinkHeaders]
}
isolated function testMissingLinkHeader() returns error? {
    http:Response response = new;
    [string, string[]]|error results = extractTopicAndHubUrls(response);
    if (results is error) {
        string expectedErrMsg = "Link header unavailable in discovery response";
        test:assertEquals(results.message(), expectedErrMsg, msg = "invalid error message on unavailable link headers(s)");
    } else {
        test:assertFail(msg = "expected: {ballerina/websub}WebSubError, not found");
    }
}

@test:Config {
    dependsOn: [testMissingLinkHeader]
}
isolated function testSingleLinkHeaderWithMultipleTopics() returns error? {
    http:Response response = new;
    response.addHeader("Link", "<" + HUB_ONE + ">; rel=\"not_hub\", <" + TOPIC_ONE + ">; rel=\"self\", <" +
            HUB_TWO + ">; rel=\"self\"");
    [string, string[]]|error results = extractTopicAndHubUrls(response);
    if (results is error) {
        string expectedErrMsg = "Link Header contains > 1 self URLs";
        test:assertEquals(results.message(), expectedErrMsg, msg = "invalid error message on > 1 topics");
    } else {
        test:assertFail(msg = "expected: {ballerina/websub}WebSubError, not found");
    }
}

@test:Config {
    dependsOn: [testSingleLinkHeaderWithMultipleTopics]
}
isolated function testMultipleLinkHeadersWithMultipleTopics() returns error? {
    http:Response response = new;
    response.addHeader("Link", "<" + HUB_ONE + ">; rel=\"not_hub\"");
    response.addHeader("Link", "<" + TOPIC_ONE + ">; rel=\"self\"");
    response.addHeader("Link", "<" + HUB_TWO + ">; rel=\"self\"");
    [string, string[]]|error results = extractTopicAndHubUrls(response);
    if (results is error) {
        string expectedErrMsg = "Link Header contains > 1 self URLs";
        test:assertEquals(results.message(), expectedErrMsg, msg = "invalid error message on > 1 topics");
    } else {
        test:assertFail(msg = "expected: {ballerina/websub}WebSubError, not found");
    }
}

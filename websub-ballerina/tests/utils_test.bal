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

const string HASH_KEY = "secret";

@test:Config { 
    groups: ["contentHashRetrieval"]
}
function testContentHashForSha1() returns @tainted error? {
    byte[] hashedContent = check retrieveContentHash("sha1", HASH_KEY, "This is sample content");
    test:assertTrue(hashedContent.length() > 0);
}

@test:Config { 
    groups: ["contentHashRetrieval"]
}
function testContentHashForSha256() returns @tainted error? {
    byte[] hashedContent = check retrieveContentHash("sha256", HASH_KEY, "This is sample content");
    test:assertTrue(hashedContent.length() > 0);
}

@test:Config { 
    groups: ["contentHashRetrieval"]
}
function testContentHashForSha384() returns @tainted error? {
    byte[] hashedContent = check retrieveContentHash("sha384", HASH_KEY, "This is sample content");
    test:assertTrue(hashedContent.length() > 0);
}

@test:Config { 
    groups: ["contentHashRetrieval"]
}
function testContentHashForSha512() returns @tainted error? {
    byte[] hashedContent = check retrieveContentHash("sha512", HASH_KEY, "This is sample content");
    test:assertTrue(hashedContent.length() > 0);
}

@test:Config { 
    groups: ["contentHashRetrieval"]
}
function testContentHashError() returns @tainted error? {
    var hashedContent = retrieveContentHash("xyz", HASH_KEY, "This is sample content");
    string expectedErrorMsg = "Unrecognized hashning-method [xyz] found";
    if (hashedContent is error) {
        test:assertEquals(hashedContent.message(), expectedErrorMsg);
    } else {
        test:assertFail("Content hash generation not properly working for unidentified hash-method");
    }
}
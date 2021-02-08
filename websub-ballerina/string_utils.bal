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

import ballerina/lang.'string as strings;
import ballerina/random;

isolated function generateRandomString(int length) returns string | error {
    int[] codePoints = [];

    int leftLimit = 48; // numeral '0'
    int rightLimit = 122; // letter 'z'
        
    int iterator = 0;
    while (iterator < length) {
        var randomInt = checkpanic random:createIntInRange(leftLimit, rightLimit);
        if (randomInt <= 57 || randomInt >= 65) && (randomInt <= 90 || randomInt >= 97) {
            codePoints.push(randomInt);
            iterator += 1;
        }
    }

    return strings:fromCodePointInts(codePoints);
}
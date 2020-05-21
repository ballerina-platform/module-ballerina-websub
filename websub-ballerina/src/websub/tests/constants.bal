// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

// Integration tests constants
const string CUSTOM_SUB_MOCK_HEADER = "MockHeader";
const string MOCK_HEADER = "MockHeader";
const string CONTENT_TYPE_JSON = "application/json";

const string WEBSUB_TOPIC_ONE = "http://one.websub.topic.com";
const string WEBSUB_TOPIC_TWO = "http://two.websub.topic.com";
const string WEBSUB_TOPIC_THREE = "http://three.websub.topic.com";
const string WEBSUB_TOPIC_FOUR = "http://four.websub.topic.com";
const string WEBSUB_TOPIC_FIVE = "http://one.redir.topic.com";
const string WEBSUB_TOPIC_SIX = "http://two.redir.topic.com";

const string ID_INTENT_VER_REQ_RECEIVED_LOG = "IntentVerificationInvocation";
const string ID_BY_KEY_CREATED_LOG = "DispatchingByKeyCreated";
const string ID_BY_KEY_FEATURE_LOG = "DispatchingByKeyFeatured";
const string ID_BY_HEADER_ISSUE_LOG = "DispatchingByHeaderIssue";
const string ID_BY_HEADER_COMMIT_LOG = "DispatchingByHeaderCommit";
const string ID_BY_HEADER_AND_PAYLOAD_ISSUE_CREATED_LOG = "DispatchingByHeaderAndPayloadKeyCreated";
const string ID_BY_HEADER_AND_PAYLOAD_FEATURE_PULL_LOG = "DispatchingByHeaderAndPayloadKeyFeature";
const string ID_BY_HEADER_AND_PAYLOAD_HEADER_ONLY_LOG = "DispatchingByHeaderAndPayloadKeyForOnlyHeader";
const string ID_BY_HEADER_AND_PAYLOAD_KEY_ONLY_LOG = "DispatchingByHeaderAndPayloadKeyForOnlyKey";
const string ID_REDIRECT_SUBSCRIBER_ONE_LOG = "RedirectSubscriberOneLog";
const string ID_REDIRECT_SUBSCRIBER_TWO_LOG = "RedirectSubscriberTwoLog";

const string INTENT_VER_REQ_RECEIVED_LOG = "Intent verification request received";
const string BY_KEY_CREATED_LOG = "Created Notification Received, action: created";
const string BY_KEY_FEATURE_LOG = "Feature Notification Received, domain: feature";
const string BY_HEADER_ISSUE_LOG = "Issue Notification Received, header value: issue action: deleted";
const string BY_HEADER_COMMIT_LOG = "Commit Notification Received, header value: commit action: created";
const string BY_HEADER_AND_PAYLOAD_ISSUE_CREATED_LOG = "Issue Created Notification Received, header value: issue action: created";
const string BY_HEADER_AND_PAYLOAD_FEATURE_PULL_LOG = "Feature Pull Notification Received, header value: pull domain: feature";
const string BY_HEADER_AND_PAYLOAD_HEADER_ONLY_LOG = "HeaderOnly Notification Received, header value: headeronly action: header_only";
const string BY_HEADER_AND_PAYLOAD_KEY_ONLY_LOG = "KeyOnly Notification Received, header value: key_only action: keyonly";
const string REDIRECT_SUBSCRIBER_ONE_LOG = "Successful redirect subscriber one";
const string REDIRECT_SUBSCRIBER_TWO_LOG = "Successful redirect subscriber two";

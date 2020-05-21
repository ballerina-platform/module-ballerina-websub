// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/io;
import ballerina/lang.'object as lang;
import ballerina/test;

public type WebhookListenerConfiguration record {
    string host = "";
};

public type MockActionEvent record {
    string action;
};

public type MockDomainEvent record {|
    string domain;
    Extra extra;
|};

public type Extra record {|
    string value;
|};

@SubscriberServiceConfig {
    path:"/key"
}
service keyWebhook on new WebhookServerForPayload(23595) {
    resource function onIntentVerification(Caller caller, IntentVerificationRequest verRequest) {
        storeOutput(ID_PROJECT_INTENT_VER_REQ_RECEIVED_LOG, "[Project]Intent verification request received");
        checkpanic caller->accepted();
    }

    resource function onCreated(Notification notification, MockActionEvent event) {
        io:println("[Project]Created Notification Received, action: ", event.action);
    }

    resource function onFeature(Notification notification, MockDomainEvent event) {
        io:println("[Project]Feature Notification Received, domain: ", event.extra.value);
    }

    resource function onStatus(Notification notification, MockActionEvent event) {
        // do nothing - test start up
    }
}

@SubscriberServiceConfig {
    path:"/header"
}
service headerWebhook on new WebhookServerForHeader(23596) {
    resource function onIssue(Notification notification, MockActionEvent event) {
        io:println("[Project]Issue Notification Received, header value: ", notification.getHeader(MOCK_HEADER),
            " action: ", event.action);
    }

    resource function onCommit(Notification notification, MockActionEvent event) {
        io:println("[Project]Commit Notification Received, header value: ", notification.getHeader(MOCK_HEADER),
            " action: ", event.action);
    }

    resource function onStatus(Notification notification, MockActionEvent event) {
        // do nothing - test start up
    }
}

@SubscriberServiceConfig {
    path:"/headerAndPayload"
}
service headerAndPayloadWebhook on new WebhookServerForHeaderAndPayload(23597) {
    resource function onIssueCreated(Notification notification, MockActionEvent event) {
        io:println("[Project]Issue Created Notification Received, header value: ", notification.getHeader(MOCK_HEADER),
            " action: ", event.action);
    }

    resource function onFeaturePull(Notification notification, MockDomainEvent event) {
        io:println("[Project]Feature Pull Notification Received, header value: ", notification.getHeader(MOCK_HEADER),
            " domain: ", event.extra.value);
    }

    resource function onHeaderOnly(Notification notification, MockActionEvent event) {
        io:println("[Project]HeaderOnly Notification Received, header value: ", notification.getHeader(MOCK_HEADER),
            " action: ", event.action);
    }

    resource function onKeyOnly(Notification notification, MockActionEvent event) {
        io:println("[Project]KeyOnly Notification Received, header value: ", notification.getHeader(MOCK_HEADER),
            " action: ", event.action);
    }

    resource function onStatus(Notification notification, MockActionEvent event) {
        // do nothing - test start up
    }
}

/////////////////// Specific Webhook for dispatching by key ///////////////////
public type WebhookServerForPayload object {

    *lang:Listener;

    private Listener websubListener;

    public function __init(int port, WebhookListenerConfiguration? config = ()) {
        ExtensionConfig extensionConfig = {
            topicIdentifier: TOPIC_ID_PAYLOAD_KEY,
            payloadKeyResourceMap: {
                "action" : {
                    "created" : ["onCreated", MockActionEvent],
                    "deleted" : ["onDeleted", MockActionEvent],
                    "statuscheck" : ["onStatus", MockActionEvent]
                },
                "domain" : {
                    "issue" : ["onIssue", MockDomainEvent],
                    "feature" : ["onFeature", MockDomainEvent]
                }
            }
        };
        string host = config is () ? "" : config.host;
        SubscriberListenerConfiguration sseConfig = {
            host: host,
            extensionConfig: extensionConfig
        };
        self.websubListener = new(port, sseConfig);
    }

    public function __attach(service s, string? name = ()) returns error? {
        return self.websubListener.__attach(s, name);
    }

    public function __detach(service s) returns error? {
    }

    public function __start() returns error? {
        return self.websubListener.__start();
    }

    public function __gracefulStop() returns error? {
        return ();
    }

    public function __immediateStop() returns error? {
        return self.websubListener.__immediateStop();
    }
};

/////////////////// Specific Webhook for dispatching by header ///////////////////
public type WebhookServerForHeader object {

    *lang:Listener;

    private Listener websubListener;

    public function __init(int port, WebhookListenerConfiguration? config = ()) {
        ExtensionConfig extensionConfig = {
            topicIdentifier: TOPIC_ID_HEADER,
            topicHeader: MOCK_HEADER,
            headerResourceMap: {
                "issue" : ["onIssue", MockActionEvent],
                "commit" : ["onCommit", MockActionEvent],
                "status" : ["onStatus", MockActionEvent]
            }
        };
        string host = config is () ? "" : config.host;
        SubscriberListenerConfiguration sseConfig = {
            host: host,
            extensionConfig: extensionConfig
        };
        self.websubListener = new(port, sseConfig);
    }

    public function __attach(service s, string? name = ()) returns error? {
        return self.websubListener.__attach(s, name);
    }

    public function __detach(service s) returns error? {
    }

    public function __start() returns error? {
        return self.websubListener.__start();
    }

    public function __gracefulStop() returns error? {
        return ();
    }

    public function __immediateStop() returns error? {
        return self.websubListener.__immediateStop();
    }
};

/////////////////// Specific Webhook for dispatching by header and payload ///////////////////
public type WebhookServerForHeaderAndPayload object {

    *lang:Listener;

    private Listener websubListener;

    public function __init(int port, WebhookListenerConfiguration? config = ()) {
        ExtensionConfig extensionConfig = {
            topicIdentifier: TOPIC_ID_HEADER_AND_PAYLOAD,
            topicHeader: MOCK_HEADER,
            headerResourceMap: {
                "headeronly" : ["onHeaderOnly", MockActionEvent],
                "status" : ["onStatus", MockActionEvent]
            },
            payloadKeyResourceMap: {
                "action" : {
                    "keyonly" : ["onKeyOnly", MockActionEvent]
                },
                "domain" : {
                    "domainkeyonly" : ["onDomainKeyOnly", MockDomainEvent]
                }
            },
            headerAndPayloadKeyResourceMap: {
                "issue" : {
                    "action" : {
                        "created" : ["onIssueCreated", MockActionEvent],
                        "deleted" : ["onIssueDeleted", MockActionEvent]
                    }
                },
                "pull" : {
                    "domain" : {
                        "bugfix" : ["onBugFixPull", MockDomainEvent],
                        "feature" : ["onFeaturePull", MockDomainEvent]
                    }
                }
            }
        };
        string host = config is () ? "" : config.host;
        SubscriberListenerConfiguration sseConfig = {
            host: host,
            extensionConfig: extensionConfig
        };
        self.websubListener = new(port, sseConfig);
    }

    public function __attach(service s, string? name = ()) returns error? {
        return self.websubListener.__attach(s, name);
    }

    public function __detach(service s) returns error? {
    }

    public function __start() returns error? {
        return self.websubListener.__start();
    }

    public function __gracefulStop() returns error? {
        return ();
    }

    public function __immediateStop() returns error? {
        return self.websubListener.__immediateStop();
    }
};

@test:Config {
    dependsOn: ["testDispatchingByHeaderAndPayloadKeyForOnlyKey"]
}
function testProjectOnIntentVerificationInvocation() {
    http:Client clientEndpoint = new ("http://localhost:23595/");
    var response = clientEndpoint->get("/key");
    test:assertEquals(
        fetchOutput(ID_PROJECT_INTENT_VER_REQ_RECEIVED_LOG),
        PROJECT_INTENT_VER_REQ_RECEIVED_LOG,
        msg = "Mismatched"
    );
}

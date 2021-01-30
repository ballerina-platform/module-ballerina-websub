// // Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
// //
// // WSO2 Inc. licenses this file to you under the Apache License,
// // Version 2.0 (the "License"); you may not use this file except
// // in compliance with the License.
// // You may obtain a copy of the License at
// //
// // http://www.apache.org/licenses/LICENSE-2.0
// //
// // Unless required by applicable law or agreed to in writing,
// // software distributed under the License is distributed on an
// // "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// // KIND, either express or implied.  See the License for the
// // specific language governing permissions and limitations
// // under the License.

// import ballerina/lang.'object as lang;
// import ballerina/test;

// public type SpecificSubMockActionEvent record {|
//     string action;
// |};

// public type SpecificSubMockDomainEvent record {|
//     string domain;
// |};

// public type SpecificSubscriber service object {
// };

// public class SpecificSubWebhookServerForPayload {

//     private Listener websubListener;

//     public function init(int port, string? host = ()) {
//         ExtensionConfig extensionConfig = {
//             topicIdentifier: TOPIC_ID_PAYLOAD_KEY,
//             payloadKeyResourceMap: {
//                 "action" : {
//                     "created" : ["onCreated", SpecificSubMockActionEvent],
//                     "deleted" : ["onDeleted", SpecificSubMockActionEvent],
//                     "statuscheck" : ["onStatus", SpecificSubMockActionEvent]
//                 },
//                 "domain" : {
//                     "issue" : ["onIssue", SpecificSubMockDomainEvent],
//                     "feature" : ["onFeature", SpecificSubMockDomainEvent]
//                 }
//             }
//         };
//         SubscriberListenerConfiguration sseConfig = {
//             host: host ?: "",
//             extensionConfig: extensionConfig
//         };
//         self.websubListener = new(port, sseConfig);
//     }

//     public function attach(SpecificSubsriber s, string[]|string? name = ()) returns error? {
//         return self.websubListener.attach(s, name);
//     }

//     public function detach(SpecificSubsriber s) returns error? {
//         return self.websubListener.detach(s);
//     }

//     public function 'start() returns error? {
//         return self.websubListener.'start();
//     }

//     public function gracefulStop() returns error? {
//         return self.websubListener.gracefulStop();
//     }

//     public function immediateStop() returns error? {
//         return self.websubListener.immediateStop();
//     }
// }

// service object keyWebhook1 =
// @SubscriberServiceConfig {
//     //path:"/key"
// }
// //@SpecificSubscriber
// service SpecificSubscriber /key {
//     remote function onOpened(Notification notification, SpecificSubMockActionEvent event) {
//     }

//     remote function onFeature(Notification notification, SpecificSubMockDomainEvent event) {
//     }

//     remote function onStatus(Notification notification, SpecificSubMockActionEvent event) {
//     }

//     remote function onReopened(Notification notification, SpecificSubMockActionEvent event) {
//     }
// };

// service keyWebhook2 =
// @SubscriberServiceConfig {
//     //path:"/key"
// }
// //@SpecificSubscriber
// service SpecificSubscriber /key {
//     remote function onCreated(Notification notification, SpecificSubMockActionEvent event) {
//     }

//     remote function onFeature(Notification notification, SpecificSubMockDomainEvent event) {
//     }

//     remote function onStatus(Notification notification, SpecificSubMockDomainEvent event) {
//     }
// };

// @test:Config {}
// public function testInvalidResourceFunctions() {
//     SpecificSubWebhookServerForPayload l = new(8081);
//     error? err = trap l.attach(keyWebhook1);
//     if !(err is error) {
//         test:assertFail("Expected: panic, but not found");
//     }
// }

// @test:Config {
//     dependsOn: [testInvalidResourceFunctions]
// }
// public function testInvalidParam() {
//     SpecificSubWebhookServerForPayload l = new(8081);
//     error? err = trap l.attach(keyWebhook2);
//     if !(err is error) {
//         test:assertFail("Expected: panic, but not found");
//     }
// }

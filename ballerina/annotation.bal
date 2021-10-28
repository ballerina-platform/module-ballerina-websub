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

import ballerina/http;

# Configuration for a WebSubSubscriber service.
#
# + target - The `string` resource URL for which discovery will be initiated to identify the hub and topic,
#            or a tuple `[hub, topic]` representing a discovered hub and a topic
# + leaseSeconds - The period for which the subscription is expected to be active
# + callback - The callback URL for subscriber-service
# + secret - The secret to be used for authenticated content distribution
# + appendServicePath - This flag notifies whether or not to append service-path to callback-url
# + unsubscribeOnShutdown - This flag notifies whether or not to initiate unsubscription when the service is shutting down
# + httpConfig - The configuration for the hub client used to interact with the discovered/specified hub
# + discoveryConfig - HTTP client configurations for resource discovery
public type SubscriberServiceConfiguration record {|
    string|[string, string] target?;
    int leaseSeconds?;
    string callback?;
    string secret?;
    boolean appendServicePath = false;
    boolean unsubscribeOnShutdown = false;
    http:ClientConfiguration httpConfig?;
    record {|
        string|string[] accept?;
        string|string[] acceptLanguage?;
        http:ClientConfiguration httpConfig?;
    |} discoveryConfig?;
|};

# WebSub Subscriber Configuration for the service, indicating subscription related parameters.
public annotation SubscriberServiceConfiguration SubscriberServiceConfig on service;

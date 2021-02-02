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

# Intent verification request parameter 'hub.challenge' representing the challenge that needs to be echoed by
# susbscribers to verify intent.
const string HUB_CHALLENGE = "hub.challenge";

# Parameter `hub.mode` representing the mode of the request from hub to subscriber or subscriber to hub.
const string HUB_MODE = "hub.mode";

# Subscription change or intent verification request parameter 'hub.topic'' representing the topic relevant to the for
# which the request is initiated.
const string HUB_TOPIC = "hub.topic";

# Subscription change request parameter 'hub.callback' representing the callback to which notification should happen.
const string HUB_CALLBACK = "hub.callback";

# Subscription request parameter 'hub.lease_seconds' representing the period for which the subscription is expected to
# be active.
const string HUB_LEASE_SECONDS = "hub.lease_seconds";

# Subscription denied request parameter 'hub.reason' represents the reason for subscription-denial.
# This is an optional parameter
const string HUB_REASON = "hub.reason";

# Subscription parameter 'hub.secret' representing the secret key to use for authenticated content distribution.
const string HUB_SECRET = "hub.secret";

const string ACCEPT_HEADER = "Accept";
const string ACCEPT_LANGUAGE_HEADER = "Accept-Language";
# `HTTP Content-Type` Header Name, used to include `Content-Type` header value manually to `HTTP Request`.
const string CONTENT_TYPE = "Content-Type";

# `HTTP X-Hub-Signature` Header Name, used to include `X-Hub-Signature` header value manually to `HTTP Request`,
#  value of this `HTTP Header` is used by subscriber to verify whether the content is published by a valid hub.
const string X_HUB_SIGNATURE = "X-Hub-Signature";

# `hub.mode` value indicating "subscribe" mode, used by a hub to notify a subscription verification.
const string MODE_SUBSCRIBE = "subscribe";
# `hub.mode` value indicating "unsubscribe" mode, used by a hub to notify an unsubscription verification.
const string MODE_UNSUBSCRIBE = "unsubscribe";
# `hub.mode` value indicating "denied" mode, used by a hub to notify a subscription denial.
const string MODE_DENIED = "denied";

public type SubscriptionVerification record {
    string hubMode;
    string hubTopic;
    string hubChallenge;
    string? hubLeaseSeconds;
};

public type ContentDistributionMessage record {
    map<string|string[]>? headers = ();
    string? contentType = ();
    json|xml|string|byte[] content;
};

type CommonResponse record {|
    map<string|string[]>? headers = ();
    map<string>? body = ();
|};

public type SubscriptionVerificationSuccess record {
    *CommonResponse;
};

public type Acknowledgement record {
    *CommonResponse;
};

public type RequestQueryParams record {|
    string hubMode = "";
    string hubTopic = "";
    string hubChallenge = "";
    string? hubLeaseSeconds = ();
    string hubReason = "";
|};

# Record representing a WebSub subscription change request.
#
# + topic - The topic for which the subscription/unsubscription request is sent
# + callback - The callback which should be registered/unregistered for the subscription/unsubscription request sent
# + leaseSeconds - The lease period for which the subscription is expected to be active
# + secret - The secret to be used for authenticated content distribution with this subscription
public type SubscriptionChangeRequest record {|
    string topic = "";
    string callback = "";
    int leaseSeconds = 0;
    string secret = "";
|};

# Record representing subscription/unsubscription details if a subscription/unsubscription request is successful.
#
# + hub - The hub at which the subscription/unsubscription was successful
# + topic - The topic for which the subscription/unsubscription was successful
# + response - The response from the hub to the subscription/unsubscription request
public type SubscriptionChangeResponse record {|
    string hub = "";
    string topic = "";
    http:Response response;
|};

isolated function isSuccessStatusCode(int statusCode) returns boolean {
    return (200 <= statusCode && statusCode < 300);
}

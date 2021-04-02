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

# `hub.mode` value indicating "subscribe" mode, used by a hub to notify a subscription verification.
const string MODE_SUBSCRIBE = "subscribe";

# `hub.mode` value indicating "unsubscribe" mode, used by a hub to notify an unsubscription verification.
const string MODE_UNSUBSCRIBE = "unsubscribe";

# `hub.mode` value indicating "denied" mode, used by a hub to notify a subscription denial.
const string MODE_DENIED = "denied";

# HTTP `Accept` Header name used to include `Accept` header value manually to `HTTP Request`
const string ACCEPT_HEADER = "Accept";

# HTTP `Accept-Language` Header name used to include `Accept-Language` header value manually to `HTTP Request`
const string ACCEPT_LANGUAGE_HEADER = "Accept-Language";

# `HTTP Content-Type` Header Name, used to include `Content-Type` header value manually to `HTTP Request`.
const string CONTENT_TYPE = "Content-Type";

# `HTTP X-Hub-Signature` Header Name, used to include `X-Hub-Signature` header value manually to `HTTP Request`,
#  value of this `HTTP Header` is used by subscriber to verify whether the content is published by a valid hub.
const string X_HUB_SIGNATURE = "X-Hub-Signature";

# Common service-path to be used if the path-generation failed
const string COMMON_SERVICE_PATH = "subscriber";

# HMAC Algorithms used for content verification
const string SHA1 = "sha1";
const string SHA_256 = "sha256";
const string SHA_384 = "sha384";
const string SHA_512 = "sha512";

# Record representing the subscription / unsubscription intent verification request-body.
# 
# + hubMode - current hub.mode parameter (subscribe / unsubscribe)
# + hubTopic - topic URL
# + hubChallenge - hub.challenge parameter used for verification
# + hubLeaseSeconds - hub.lease_seconds parameter used to validate the expiration of subscription
public type SubscriptionVerification record {
    string hubMode;
    string hubTopic;
    string hubChallenge;
    string? hubLeaseSeconds;
};

# Record representing the content-distribution request.
# 
# + headers - request headers retrieve from the original `http:Request`
# + contentType - content-type header value of the original `http:Request`
# + content - received content
public type ContentDistributionMessage record {
    map<string|string[]>? headers = ();
    string? contentType = ();
    map<string|string[]>|json|xml|string|byte[] content;
};

# Record representing the common-response to be returned.
# 
# + headers - additional headers to be included in `http:Response`
# + body - content to be included in `http:Response` body
type CommonResponse record {|
    map<string|string[]> headers?;
    map<string> body?;
|};

# Record representing the subscription / unsubscription intent verification success.
public type SubscriptionVerificationSuccess record {
    *CommonResponse;
};

# Record representing the subscription-denial / content-distribution acknowledgement
public type Acknowledgement record {
    *CommonResponse;
};

# Provides a set of configurations for configure the underlying HTTP listener of the WebSub listener.
public type ListenerConfiguration record {|
    *http:ListenerConfiguration;
|};

# Record representing a WebSub subscription change request-body.
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

# Record representing the query-parameters retrieved from the `http:Request`
# 
# + hubMode - value for the hub.mode parameter
# + hubTopic - value for the hub.topic parameter
# + hubChallenge - value for the hub.challenge parameter
# + hubLeaseSeconds - value for the hub.lease_seconds parameter
# + hubReason - value for the hub.reason parameter
type RequestQueryParams record {|
    string hubMode?;
    string hubTopic?;
    string hubChallenge?;
    string? hubLeaseSeconds = ();
    string hubReason?;
|};

# Common Responses to be used in subscriber-service implementation
public final readonly & Acknowledgement ACKNOWLEDGEMENT = {};
public final readonly & SubscriptionVerificationSuccess SUBSCRIPTION_VERIFICATION_SUCCESS = {};
public final SubscriptionVerificationError SUBSCRIPTION_VERIFICATION_ERROR = error SubscriptionVerificationError("Subscription verification failed");
public final SubscriptionDeletedError SUBSCRIPTION_DELETED_ERROR = error SubscriptionDeletedError("Subscription deleted");

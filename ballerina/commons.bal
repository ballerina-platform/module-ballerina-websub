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

# The 'hub.challenge' representing the challenge value in intent verification request that needs to be echoed by
# `subscriber` to verify the intent.
const string HUB_CHALLENGE = "hub.challenge";

# The `hub.mode` parameter representing the mode of the request from the hub to subscriber or subscriber to hub.
const string HUB_MODE = "hub.mode";

# Subscription change or intent verification request parameter 'hub.topic'' representing the topic relevant to the for
# which the request is initiated.
const string HUB_TOPIC = "hub.topic";

# The 'hub.callback'  subscription change request parameter representing the callback to which the notification should happen.
const string HUB_CALLBACK = "hub.callback";

# The 'hub.lease_seconds' subscription request parameter  representing the period for which the subscription is expected to be active.
const string HUB_LEASE_SECONDS = "hub.lease_seconds";

# The 'hub.reason' subscription denied request parameter represents the reason for the subscription denial.
# This is an optional parameter
const string HUB_REASON = "hub.reason";

# The 'hub.secret' subscription parameter representing the secret key to use for authenticated content distribution.
const string HUB_SECRET = "hub.secret";

# The `hub.mode` value indicating the `subscribe` mode used by a hub to notify a subscription verification.
const string MODE_SUBSCRIBE = "subscribe";

# The `hub.mode` value indicating the `unsubscribe` mode used by a hub to notify an unsubscription verification.
const string MODE_UNSUBSCRIBE = "unsubscribe";

# The `hub.mode` value indicating the `denied` mode used by a hub to notify a subscription denial.
const string MODE_DENIED = "denied";

# The HTTP `Accept` Header name used to include the `Accept` header value manually to the `HTTP Request`.
const string ACCEPT_HEADER = "Accept";

# The HTTP `Accept-Language` Header name used to include the `Accept-Language` header value manually to the `HTTP Request`.
const string ACCEPT_LANGUAGE_HEADER = "Accept-Language";

# The HTTP `Content-Type` Header Name used to include the `Content-Type` header value manually to the `HTTP Request`.
const string CONTENT_TYPE = "Content-Type";

# The HTTP `X-Hub-Signature` Header Name used to include the `X-Hub-Signature` header value manually to the `HTTP Request`.
# The value of this `HTTP Header` is used by the subscriber to verify whether the content is published by a valid hub.
const string X_HUB_SIGNATURE = "X-Hub-Signature";

# The common service path to be used if the path generation failed.
const string COMMON_SERVICE_PATH = "subscriber";

# The HMAC Algorithms used for content verification.
const string SHA1 = "sha1";
const string SHA_256 = "sha256";
const string SHA_384 = "sha384";
const string SHA_512 = "sha512";

# Record representing the subscription / unsubscription intent verification request-body.
# 
# + hubMode - The `hub.mode` parameter (subscribe / unsubscribe)
# + hubTopic - The topic URL
# + hubChallenge - The `hub.challenge` parameter used for verification
# + hubLeaseSeconds - The `hub.lease_seconds` parameter used to validate the expiration of subscription
public type SubscriptionVerification record {
    string hubMode;
    string hubTopic;
    string hubChallenge;
    string? hubLeaseSeconds;
};

# Record representing the content-distribution request.
# 
# + headers - Request headers retrieved from the original `HTTP Request`
# + contentType - Content-type header value of the original `HTTP Request`
# + content - The received content
public type ContentDistributionMessage record {
    map<string|string[]>? headers = ();
    string? contentType = ();
    map<string|string[]>|json|xml|string|byte[] content;
};

# Record representing the common-response to be returned.
# 
# + headers - Additional headers to be included in the `http:Response`
# + body - Content to be included in the `http:Response` body
type CommonResponse record {|
    map<string|string[]> headers?;
    map<string> body?;
|};

# Record representing the subscription / unsubscription intent verification success.
public type SubscriptionVerificationSuccess record {
    *CommonResponse;
};

# Record representing the subscription-denial/content-distribution acknowledgement.
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

# Record representing the query parameters retrieved from the `HTTP Request`.
# 
# + hubMode - Value for the `hub.mode` parameter
# + hubTopic - Value for the `hub.topic` parameter
# + hubChallenge - Value for the `hub.challenge` parameter
# + hubLeaseSeconds - Value for the `hub.lease_seconds` parameter
# + hubReason - Value for the `hub.reason` parameter
type RequestQueryParams record {|
    string hubMode?;
    string hubTopic?;
    string hubChallenge?;
    string? hubLeaseSeconds = ();
    string hubReason?;
|};

# Common Responses to be used in the subscriber-service implementation.
public final readonly & Acknowledgement ACKNOWLEDGEMENT = {};
public final readonly & SubscriptionVerificationSuccess SUBSCRIPTION_VERIFICATION_SUCCESS = {};
public final SubscriptionVerificationError SUBSCRIPTION_VERIFICATION_ERROR = error SubscriptionVerificationError("Subscription verification failed");
public final SubscriptionDeletedError SUBSCRIPTION_DELETED_ERROR = error SubscriptionDeletedError("Subscription deleted");

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

const string HUB_CHALLENGE = "hub.challenge";
const string HUB_MODE = "hub.mode";
const string HUB_TOPIC = "hub.topic";
const string HUB_CALLBACK = "hub.callback";
const string HUB_LEASE_SECONDS = "hub.lease_seconds";
const string HUB_REASON = "hub.reason";
const string HUB_SECRET = "hub.secret";
const string MODE_SUBSCRIBE = "subscribe";
const string MODE_UNSUBSCRIBE = "unsubscribe";
const string MODE_DENIED = "denied";

const string ACCEPT_HEADER = "Accept";
const string ACCEPT_LANGUAGE_HEADER = "Accept-Language";
const string CONTENT_TYPE = "Content-Type";
const string X_HUB_SIGNATURE = "X-Hub-Signature";

const string COMMON_SERVICE_PATH = "subscriber";

const string SHA1 = "sha1";
const string SHA_256 = "sha256";
const string SHA_384 = "sha384";
const string SHA_512 = "sha512";

const string HTTP = "http";
const string HTTPS = "https";

# Record representing the subscription intent verification request-body.
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

# Record representing the unsubscription intent verification request-body.
# 
# + hubMode - The `hub.mode` parameter (subscribe / unsubscribe)
# + hubTopic - The topic URL
# + hubChallenge - The `hub.challenge` parameter used for verification
# + hubLeaseSeconds - The `hub.lease_seconds` parameter used to validate the expiration of subscription
public type UnsubscriptionVerification record {
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

# Record representing the subscription intent verification success.
public type SubscriptionVerificationSuccess record {
    *CommonResponse;
};

# Record representing the unsubscription intent verification success.
public type UnsubscriptionVerificationSuccess record {
    *CommonResponse;
};

# Record representing the subscription-denial/content-distribution acknowledgement.
public type Acknowledgement record {
    *CommonResponse;
};

# Provides a set of configurations for configure the underlying HTTP listener of the WebSub listener.
# 
# + gracefulShutdownPeriod - The time period in seconds to wait for unsubscription verification
public type ListenerConfiguration record {|
    *http:ListenerConfiguration;
    decimal gracefulShutdownPeriod = 20;
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

# Common response, which could be used for `websub:Acknowledgement`.
public final readonly & Acknowledgement ACKNOWLEDGEMENT = {};

# Common response, which could be used for `websub:SubscriptionVerificationSuccess`.
public final readonly & SubscriptionVerificationSuccess SUBSCRIPTION_VERIFICATION_SUCCESS = {};

# Common response, which could be used for `websub:SubscriptionVerificationError`.
public final SubscriptionVerificationError SUBSCRIPTION_VERIFICATION_ERROR = error SubscriptionVerificationError("Subscription verification failed");

# Common response, which could be used for `websub:UnsubscriptionVerificationSuccess`.
public final readonly & UnsubscriptionVerificationSuccess UNSUBSCRIPTION_VERIFICATION_SUCCESS = {};

# Common response, which could be used for `websub:UnsubscriptionVerificationError`.
public final UnsubscriptionVerificationError UNSUBSCRIPTION_VERIFICATION_ERROR = error UnsubscriptionVerificationError("Unsubscription verification failed");

# Common response, which could be used for `websub:SubscriptionDeletedError`.
public final SubscriptionDeletedError SUBSCRIPTION_DELETED_ERROR = error SubscriptionDeletedError("Subscription deleted");

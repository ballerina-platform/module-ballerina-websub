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

# Represents a webSub distinct error.
public type Error distinct error<CommonResponse>;

# Represents a websub service execution error.
public type ServiceExecutionError distinct error<CommonResponse>;

# Represents a listener errors.
public type ListenerError distinct Error;

# Represents a resource-discovery failed error.
public type ResourceDiscoveryFailedError distinct Error;

# Represents a subscription-initiation failed error.
public type SubscriptionInitiationError distinct Error;

# Represents a subscription verificatation error.
public type SubscriptionVerificationError distinct Error;

# Represents a unsubscription verificatation error.
public type UnsubscriptionVerificationError distinct Error;

# Represents a subscription-denied error.
public type SubscriptionDeniedError distinct Error;

# Represents the subscription-delete action from the `subscriber`.
public type SubscriptionDeletedError distinct Error;

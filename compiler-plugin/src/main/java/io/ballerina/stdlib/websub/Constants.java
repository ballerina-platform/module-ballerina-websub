/*
 * Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.websub;

/**
 * {@code Constants} contains the public constants to be used.
 */
public interface Constants {
    String PACKAGE_ORG = "ballerina";
    String PACKAGE_NAME = "websub";

    String LISTENER_IDENTIFIER = "Listener";
    String SERVICE_ANNOTATION_NAME = "websub:SubscriberServiceConfig";

    String ON_SUBSCRIPTION_VALIDATION_DENIED = "onSubscriptionValidationDenied";
    String ON_SUBSCRIPTION_VERIFICATION = "onSubscriptionVerification";
    String ON_UNSUBSCRIPTION_VERIFICATION = "onUnsubscriptionVerification";
    String ON_EVENT_NOTIFICATION = "onEventNotification";

    String SUBSCRIPTION_DENIED_ERROR = "websub:SubscriptionDeniedError";
    String SUBSCRIPTION_VERIFICATION = "websub:SubscriptionVerification";
    String UNSUBSCRIPTION_VERIFICATION = "websub:UnsubscriptionVerification";
    String CONTENT_DISTRIBUTION_MESSAGE = "websub:ContentDistributionMessage";
    String ACKNOWLEDGEMENT = "websub:Acknowledgement";
    String SUBSCRIPTION_VERIFICATION_SUCCESS = "websub:SubscriptionVerificationSuccess";
    String SUBSCRIPTION_VERIFICATION_ERROR = "websub:SubscriptionVerificationError";
    String UNSUBSCRIPTION_VERIFICATION_SUCCESS = "websub:UnsubscriptionVerificationSuccess";
    String UNSUBSCRIPTION_VERIFICATION_ERROR = "websub:UnsubscriptionVerificationError";
    String SUBSCRIPTION_DELETED_ERROR = "websub:SubscriptionDeletedError";
    String ERROR = "annotations:error";

    String CALLBACK = "callback";
    String APPEND_SERVICE_PATH = "appendServicePath";

    String OPTIONAL = "?";

    // resource directory structure related constants
    String TARGET_DIR_NAME = "target";
    String BIN_DIR_NAME = "bin";
    String RESOURCES_DIR_NAME = "resources";
    String RESOURCE_DIR_NAME = "resource";
}

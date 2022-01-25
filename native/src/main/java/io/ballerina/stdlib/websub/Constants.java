/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.websub;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;

/**
 * {@code Constants} contains the public constants to be used.
 */
public interface Constants {
    String PACKAGE_ORG = "ballerina";
    String PACKAGE_NAME = "websub";

    String SERVICE_OBJECT = "WEBSUB_SERVICE_OBJECT";
    String HTTP_REQUEST = "HTTP_REQUEST";

    String SERVICE_PATH = "SERVICE_PATH";
    String SERVICE_REGISTRY = "SERVICE_REGISTRY";
    String SERVICE_INFO_REGISTRY = "SERVICE_INFO_REGISTRY";
    String SUBSCRIBER_CONFIG = "SUBSCRIBER_CONFIG";

    String ON_SUBSCRIPTION_VERIFICATION = "onSubscriptionVerification";
    String ON_UNSUBSCRIPTION_VERIFICATION = "onUnsubscriptionVerification";
    String ON_SUBSCRIPTION_VALIDATION_DENIED = "onSubscriptionValidationDenied";
    String ON_EVENT_NOTIFICATION = "onEventNotification";

    String ANN_NAME_HTTP_INTROSPECTION_DOC_CONFIG = "IntrospectionDocConfig";
    BString ANN_FIELD_DOC_NAME = StringUtils.fromString("name");
}

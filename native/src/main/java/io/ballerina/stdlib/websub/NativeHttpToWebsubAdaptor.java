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

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.util.ArrayList;
import java.util.Objects;

import static io.ballerina.stdlib.websub.Constants.HTTP_REQUEST;
import static io.ballerina.stdlib.websub.Constants.SERVICE_OBJECT;
import static io.ballerina.stdlib.websub.Constants.SUBSCRIBER_CONFIG;

/**
 * {@code NativeHttpToWebsubAdaptor} is a wrapper object used for service method execution.
 */
public class NativeHttpToWebsubAdaptor {
    public static void externInit(BObject adaptor, BObject service) {
        adaptor.addNativeData(SERVICE_OBJECT, service);
    }

    @SuppressWarnings("unchecked")
    public static BMap<BString, Object> retrieveSubscriberConfig(BObject httpService) {
        Object config = httpService.getNativeData(SUBSCRIBER_CONFIG);
        if (Objects.nonNull(config)) {
            return (BMap<BString, Object>) config;
        }
        return null;
    }

    public static BArray getServiceMethodNames(BObject adaptor) {
        BObject serviceObj = (BObject) adaptor.getNativeData(SERVICE_OBJECT);
        ArrayList<BString> methodNamesList = new ArrayList<>();
        for (MethodType method : serviceObj.getType().getMethods()) {
            methodNamesList.add(StringUtils.fromString(method.getName()));
        }
        return ValueCreator.createArrayValue(methodNamesList.toArray(BString[]::new));
    }

    public static Object callOnSubscriptionVerificationMethod(Environment env, BObject adaptor,
                                                              BMap<BString, Object> message) {
        BObject serviceObj = (BObject) adaptor.getNativeData(SERVICE_OBJECT);
        return invokeRemoteFunction(env, serviceObj, message,
                "callOnSubscriptionVerificationMethod", "onSubscriptionVerification");
    }
    
    public static Object callOnUnsubscriptionVerificationMethod(Environment env, BObject adaptor,
                                                                BMap<BString, Object> message) {
        BObject serviceObj = (BObject) adaptor.getNativeData(SERVICE_OBJECT);
        return invokeRemoteFunction(env, serviceObj, message,
                "callOnUnsubscriptionVerificationMethod", "onUnsubscriptionVerification");
    }

    public static Object callOnSubscriptionDeniedMethod(Environment env, BObject adaptor, BError message) {
        BObject serviceObj = (BObject) adaptor.getNativeData(SERVICE_OBJECT);
        return invokeRemoteFunction(env, serviceObj, message,
                "callOnSubscriptionDeniedMethod", "onSubscriptionValidationDenied");
    }

    public static Object callOnEventNotificationMethod(Environment env, BObject adaptor,
                                                       BMap<BString, Object> message, BObject bHttpRequest) {
        message.addNativeData(HTTP_REQUEST, bHttpRequest);
        BObject serviceObj = (BObject) adaptor.getNativeData(SERVICE_OBJECT);
        return invokeRemoteFunction(env, serviceObj, message,
                "callOnEventNotificationMethod", "onEventNotification");
    }

    public static BObject retrieveHttpRequest(BMap<BString, Object> message) {
        return (BObject) message.getNativeData(HTTP_REQUEST);
    }

    private static Object invokeRemoteFunction(Environment env, BObject bSubscriberService, Object message,
                                               String parentFunctionName, String remoteFunctionName) {
        Future balFuture = env.markAsync();
        Module module = ModuleUtils.getModule();
        StrandMetadata metadata = new StrandMetadata(module.getOrg(), module.getName(), module.getVersion(),
                parentFunctionName);
        Object[] args = new Object[]{message, true};
        env.getRuntime().invokeMethodAsync(bSubscriberService, remoteFunctionName, null, metadata,
                new SubscriberCallback(balFuture, module), args);
        return null;
    }
}

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
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class SubscriberNativeOperationHandler {
    public static BArray getServiceMethodNames(BObject bSubscriberService) {
        ArrayList<BString> methodNamesList = new ArrayList<>();
        for (MethodType method : bSubscriberService.getType().getMethods()) {
            methodNamesList.add(StringUtils.fromString(method.getName()));
        }
        return ValueCreator.createArrayValue(methodNamesList.toArray(BString[]::new));
    }

    public static Object callOnSubscriptionVerificationMethod(Environment env, BObject bSubscriberService, BMap<BString, Object> message) {
        return invokeRemoteFunction(env, bSubscriberService, message, "callOnSubscriptionVerificationMethod", "onSubscriptionVerification");
    }   

    public static Object callOnSubscriptionDeniedMethod(Environment env, BObject bSubscriberService, BError message) {
        return invokeRemoteFunction(env, bSubscriberService, message, "callOnSubscriptionDeniedMethod", "onSubscriptionValidationDenied"); 
    }

    public static Object callOnEventNotificationMethod(Environment env, BObject bSubscriberService, BMap<BString, Object> message) {
        return invokeRemoteFunction(env, bSubscriberService, message, "callOnEventNotificationMethod", "onEventNotification"); 
    }

    private static Object invokeRemoteFunction(Environment env, BObject bSubscriberService, Object message,
                                               String parentFunctionName, String remoteFunctionName) {
        Module module = ModuleUtils.getModule();
        StrandMetadata metadata = new StrandMetadata(module.getOrg(), module.getName(), module.getVersion(), 
                                                    parentFunctionName);
        CountDownLatch latch = new CountDownLatch(1);
        CallableUnitCallback callback = new CallableUnitCallback(latch);

        Object[] args = new Object[]{message, true};
        env.getRuntime().invokeMethodAsync(bSubscriberService, remoteFunctionName, null, metadata, callback, args);

        try {
            latch.await();
        } catch (InterruptedException e) {
            // Ignore
        }
        return callback.getResult();
    }
}
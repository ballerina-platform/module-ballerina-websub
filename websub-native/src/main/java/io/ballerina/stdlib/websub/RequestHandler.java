package io.ballerina.stdlib.websub;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.util.ArrayList;

import static io.ballerina.runtime.api.utils.StringUtils.fromString;

/**
 * {@code RequestHandler} is a wrapper object used for service method execution.
 */
public class RequestHandler {
    public static void attachService(BObject serviceObj, BObject handlerObj) {
        handlerObj.addNativeData("WEBSUB_SERVICE_OBJECT", serviceObj);
    }

    public static BArray getServiceMethodNames(BObject bSubscriberService) {
        ArrayList<BString> methodNamesList = new ArrayList<>();
        for (MethodType method : bSubscriberService.getType().getMethods()) {
            methodNamesList.add(StringUtils.fromString(method.getName()));
        }
        return ValueCreator.createArrayValue(methodNamesList.toArray(BString[]::new));
    }

    public static Object callOnSubscriptionVerificationMethod(Environment env, BObject handlerObj,
                                                              BMap<BString, Object> message) {
        BObject bSubscriberService = (BObject) handlerObj.getNativeData("WEBSUB_SERVICE_OBJECT");
        return invokeRemoteFunction(env, bSubscriberService, message,
                "callOnSubscriptionVerificationMethod", "onSubscriptionVerification");
    }

    public static Object callOnSubscriptionDeniedMethod(Environment env, BObject handlerObj, BError message) {
        BObject bSubscriberService = (BObject) handlerObj.getNativeData("WEBSUB_SERVICE_OBJECT");
        return invokeRemoteFunction(env, bSubscriberService, message,
                "callOnSubscriptionDeniedMethod", "onSubscriptionValidationDenied");
    }

    public static Object callOnEventNotificationMethod(Environment env, BObject handlerObj,
                                                       BMap<BString, Object> message) {
        BObject bSubscriberService = (BObject) handlerObj.getNativeData("WEBSUB_SERVICE_OBJECT");
        return invokeRemoteFunction(env, bSubscriberService, message,
                "callOnEventNotificationMethod", "onEventNotification");
    }

    private static Object invokeRemoteFunction(Environment env, BObject bSubscriberService, Object message,
                                               String parentFunctionName, String remoteFunctionName) {
        Future balFuture = env.markAsync();
        Module module = ModuleUtils.getModule();
        StrandMetadata metadata = new StrandMetadata(module.getOrg(), module.getName(), module.getVersion(),
                parentFunctionName);
        Object[] args = new Object[]{message, true};
        env.getRuntime().invokeMethodAsync(bSubscriberService, remoteFunctionName, null, metadata, new Callback() {
            @Override
            public void notifySuccess(Object result) {
                balFuture.complete(result);
            }

            @Override
            public void notifyFailure(BError bError) {
                BString errorMessage = fromString("service method invocation failed: " + bError.getErrorMessage());
                BError invocationError = ErrorCreator.createError(module, "ServiceExecutionError",
                        errorMessage, bError, null);
                balFuture.complete(invocationError);
            }
        }, args);
        return null;
    }
}

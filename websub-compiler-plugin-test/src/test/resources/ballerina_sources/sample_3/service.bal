import ballerina/websub;
import ballerina/log;

listener websub:Listener simpleListener = new (10003);

@websub:SubscriberServiceConfig{}
service /sample on simpleListener {
    isolated remote function onSubscriptionValidationDenied(websub:SubscriptionDeniedError msg) returns websub:Acknowledgement? {
        log:printDebug("onSubscriptionValidationDenied invoked");
        return websub:ACKNOWLEDGEMENT;
    }

    isolated remote function onSubscriptionVerification(websub:SubscriptionVerification msg)
                        returns websub:SubscriptionVerificationSuccess|websub:SubscriptionVerificationError {
        log:printDebug("onSubscriptionVerification invoked");
        if (msg.hubTopic == "test1") {
            return websub:SUBSCRIPTION_VERIFICATION_ERROR;
        } else {
            return websub:SUBSCRIPTION_VERIFICATION_SUCCESS;
        }
    }
}

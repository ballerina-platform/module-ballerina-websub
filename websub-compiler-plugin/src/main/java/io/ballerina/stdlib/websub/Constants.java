package io.ballerina.stdlib.websub;

/**
 * {@code Constants} contains the public constants to be used.
 */
public interface Constants {
    String PACKAGE_ORG = "ballerina";
    String PACKAGE_NAME = "websub";

    String LISTENER_IDENTIFIER = "Listener";
    String SERVICE_ANNOTATION_NAME = "SubscriberServiceConfig";

    String ON_SUBSCRIPTION_VALIDATION_DENIED = "onSubscriptionValidationDenied";
    String ON_SUBSCRIPTION_VERIFICATION = "onSubscriptionVerification";
    String ON_EVENT_NOTIFICATION = "onEventNotification";

    String SUBSCRIPTION_DENIED_ERROR = "websub:SubscriptionDeniedError";
    String SUBSCRIPTION_VERIFICATION = "websub:SubscriptionVerification";
    String CONTENT_DISTRIBUTION_MESSAGE = "websub:ContentDistributionMessage";
    String ACKNOWLEDGEMENT = "websub:Acknowledgement";
    String SUBSCRIPTION_VERIFICATION_SUCCESS = "websub:SubscriptionVerificationSuccess";
    String SUBSCRIPTION_VERIFICATION_ERROR = "websub:SubscriptionVerificationError";
    String SUBSCRIPTION_DELETED_ERROR = "websub:SubscriptionDeletedError";

    String OPTIONAL = "?";
}

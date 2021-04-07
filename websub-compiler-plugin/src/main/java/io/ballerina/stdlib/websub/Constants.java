package io.ballerina.stdlib.websub;

/**
 * {@code Constants} contains the public constants to be used.
 */
public interface Constants {
    public static final String MODULE_NAME = "websub";
    public static final String SERVICE_ANNOTATION_NAME = "SubscriberServiceConfig";

    public static final String ON_SUBSCRIPTION_VALIDATION_DENIED = "onSubscriptionValidationDenied";
    public static final String ON_SUBSCRIPTION_VERIFICATION = "onSubscriptionVerification";
    public static final String ON_EVENT_NOTIFICATION = "onEventNotification";

    public static final String SUBSCRIPTION_DENIED_ERROR = "websub:SubscriptionDeniedError";
    public static final String SUBSCRIPTION_VERIFICATION = "websub:SubscriptionVerification";
    public static final String CONTENT_DISTRIBUTION_MESSAGE = "websub:ContentDistributionMessage";
    public static final String ACKNOWLEDGEMENT = "websub:Acknowledgement";
    public static final String SUBSCRIPTION_VERIFICATION_SUCCESS = "websub:SubscriptionVerificationSuccess";
    public static final String SUBSCRIPTION_VERIFICATION_ERROR = "websub:SubscriptionVerificationError";
    public static final String SUBSCRIPTION_DELETED_ERROR = "websub:SubscriptionDeletedError";
    public static final String OPTIONAL = "?";
}

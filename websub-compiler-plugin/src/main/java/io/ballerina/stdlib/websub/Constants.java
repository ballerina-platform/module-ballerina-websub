package io.ballerina.stdlib.websub;

/**
 * {@code Constants} contains the public constants to be used.
 */
public interface Constants {
    public static final String MODULE_NAME = "websub";
    public static final String ON_SUBSCRIPTION_VALIDATION_DENIED = "onSubscriptionValidationDenied";
    public static final String ON_SUBSCRIPTION_VERIFICATION = "onSubscriptionVerification";
    public static final String ON_EVENT_NOTIFICATION = "onEventNotification";

    public static final String WEBSUB_101 = "WEBSUB_100";

    public static final int DATAGRAM_DATA_SIZE = 8192;
    public static final String READ_ONLY_BYTE_ARRAY = "(byte[] & readonly)";
    public static final String READ_ONLY_DATAGRAM = "(udp:Datagram & readonly)";
    public static final String BYTE_ARRAY = "byte[]";
    public static final String ERROR = "Error";
    public static final String UDP = "udp";
    public static final String ON_ERROR = "";
    public static final String CALLER = "";
    public static final String ON_BYTES = "";
    public static final String ON_DATAGRAM = "";
    public static final String DATAGRAM_RECORD = "";
}

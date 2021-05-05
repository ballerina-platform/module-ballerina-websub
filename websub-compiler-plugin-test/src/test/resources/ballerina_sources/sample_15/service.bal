import ballerina/websub as foo;

listener foo:Listener securedListener = new(9090,
    secureSocket = {
        key: {
            certFile: "../resources/public.crt",
            keyFile: "../resources/private.key"
        }
    }
);

@foo:SubscriberServiceConfig{}
service /sample on securedListener {
    remote function onEventNotification(foo:ContentDistributionMessage event)
                        returns foo:Acknowledgement|foo:SubscriptionDeletedError? {
        return foo:ACKNOWLEDGEMENT;
    }
}

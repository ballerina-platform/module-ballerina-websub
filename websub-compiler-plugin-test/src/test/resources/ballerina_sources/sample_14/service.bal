import ballerina/websub as foo;

listener foo:Listener simpleListener = new (10014);

@foo:SubscriberServiceConfig{}
service /sample on simpleListener {
    function onEventNotification(foo:ContentDistributionMessage event)
                        returns foo:Acknowledgement|foo:SubscriptionDeletedError? {
        return foo:ACKNOWLEDGEMENT;
    }
}

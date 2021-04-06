import ballerina/websub;

listener websub:Listener simpleListener = new (10002);

@websub:SubscriberServiceConfig{}
service /sample on simpleListener {
    function onEventNotification(websub:ContentDistributionMessage event)
                        returns websub:Acknowledgement|websub:SubscriptionDeletedError? {
        return websub:ACKNOWLEDGEMENT;
    }
}
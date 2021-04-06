import ballerina/websub;

@websub:SubscriberServiceConfig{}
service /sample on new websub:Listener(10001) {
    remote function onEventNotification(websub:ContentDistributionMessage event) 
                        returns websub:Acknowledgement|websub:SubscriptionDeletedError? {
        return websub:ACKNOWLEDGEMENT;
    }
}
import ballerina/websub;

listener websub:Listener simpleListener = new (10001);

@websub:SubscriberServiceConfig{}
service /sample1 on new websub:Listener(10001) {
    remote function onEventNotification(websub:ContentDistributionMessage event) 
                        returns websub:Acknowledgement|websub:SubscriptionDeletedError? {
        return websub:ACKNOWLEDGEMENT;
    }
}

@websub:SubscriberServiceConfig{}
service /sample2 on new websub:Listener(10001) {
    remote function onEventNotification(websub:ContentDistributionMessage event) 
                        returns websub:Acknowledgement|websub:SubscriptionDeletedError? {
        return websub:ACKNOWLEDGEMENT;
    }
}

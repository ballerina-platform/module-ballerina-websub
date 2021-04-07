import ballerina/websub;

public type SecondaryMsgType record {
    websub:ContentDistributionMessage msg;
    string timeStamp;
};

@websub:SubscriberServiceConfig{}
service /sample on new websub:Listener(10005) {
    remote function onEventNotification(websub:ContentDistributionMessage|SecondaryMsgType event) 
                        returns websub:Acknowledgement|websub:SubscriptionDeletedError? {
        return websub:ACKNOWLEDGEMENT;
    }
}

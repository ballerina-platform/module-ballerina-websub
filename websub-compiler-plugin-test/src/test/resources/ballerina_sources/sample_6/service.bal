import ballerina/websub;

public type SimpleObj record {
    string name;
    string timeStamp;
};

@websub:SubscriberServiceConfig{}
service /sample on new websub:Listener(10006) {
    remote function onEventNotification(websub:ContentDistributionMessage event, SimpleObj metaData) 
                        returns websub:Acknowledgement|websub:SubscriptionDeletedError? {
        return websub:ACKNOWLEDGEMENT;
    }
}

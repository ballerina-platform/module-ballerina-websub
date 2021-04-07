import ballerina/websub;

public type SpecialReturnType record {|
    string name;
    string description;
|};

@websub:SubscriberServiceConfig{}
service /sample on new websub:Listener(10008) {
    remote function onEventNotification(websub:ContentDistributionMessage event) 
            returns websub:Acknowledgement|websub:SubscriptionDeletedError|SpecialReturnType? {
        return websub:ACKNOWLEDGEMENT;
    }
}

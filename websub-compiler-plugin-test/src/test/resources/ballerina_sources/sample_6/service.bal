import ballerina/websub;
import ballerina/log;

public type SimpleObj record {
    string name;
    string timeStamp;
};

@websub:SubscriberServiceConfig{}
service /sample on new websub:Listener(10005) {
    remote function onEventNotification(websub:ContentDistributionMessage event, SimpleObj metaData) 
                        returns websub:Acknowledgement|websub:SubscriptionDeletedError? {
        log:printInfo("onEventNotification invoked ", contentDistributionMessage = event);
        return {};
    }
}

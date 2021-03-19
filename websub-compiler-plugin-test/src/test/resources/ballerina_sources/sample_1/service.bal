import ballerina/websub;
import ballerina/log;

@websub:SubscriberServiceConfig{}
service /sample on new websub:Listener(10001) {
    remote function onEventNotification(websub:ContentDistributionMessage event) 
                        returns websub:Acknowledgement|websub:SubscriptionDeletedError? {
        log:printInfo("onEventNotification invoked ", contentDistributionMessage = event);
        return {};
    }
}
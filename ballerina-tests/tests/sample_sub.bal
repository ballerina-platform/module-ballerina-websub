import ballerina/websub;

@websub:SubscriberServiceConfig {}
service /ss on new websub:Listener(10001) {
    remote function onEventNotification(websub:ContentDistributionMessage message) returns error? {
        
    }
}

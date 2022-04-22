import ballerina/websub;

@websub:SubscriberServiceConfig {

}
service /foo on new websub:Listener(9090) {
    remote function onEventNotification(websub:ContentDistributionMessage message) returns websub:Acknowledgement|websub:SubscriptionDeletedError|error? {
        
    }
}

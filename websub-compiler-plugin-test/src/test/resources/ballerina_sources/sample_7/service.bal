import ballerina/websub;

@websub:SubscriberServiceConfig{}
service /sample on new websub:Listener(10007) {
    remote function onEventNotification() returns websub:Acknowledgement|websub:SubscriptionDeletedError? {
        return websub:ACKNOWLEDGEMENT;
    }
}

import ballerina/websub;

service /sample on new websub:Listener(100011) {
    remote function onEventNotification(websub:ContentDistributionMessage event) {
        return;
    }
}

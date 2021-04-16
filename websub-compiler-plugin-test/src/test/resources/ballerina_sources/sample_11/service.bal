import ballerina/websub;

listener websub:Listener simpleListener = new websub:Listener(100011);

service /sample on simpleListener {
    remote function onEventNotification(websub:ContentDistributionMessage event) {
        return;
    }
}

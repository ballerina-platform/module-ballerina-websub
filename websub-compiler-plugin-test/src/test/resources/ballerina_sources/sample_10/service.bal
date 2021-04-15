import ballerina/http;
import ballerina/websub;

@http:ServiceConfig {
}
service /sample on new websub:Listener(100010) {
    remote function onEventNotification(websub:ContentDistributionMessage event) returns websub:Acknowledgement? {
        return;
    }
}

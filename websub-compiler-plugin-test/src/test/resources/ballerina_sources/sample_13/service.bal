import ballerina/http;
import ballerina/websub;

listener http:Listener httpListener = new http:Listener(10012);
websub:ListenerConfiguration listenerConfig = {
    secureSocket: {
        key: {
            path: "tests/resources/ballerinaKeystore.pkcs12",
            password: "ballerina"
        }
    }
};

@websub:SubscriberServiceConfig {
}
service /sample on new websub:Listener(httpListener, listenerConfig) {
    remote function onEventNotification(websub:ContentDistributionMessage event) {
        return;
    }
}

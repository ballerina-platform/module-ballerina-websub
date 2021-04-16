import ballerina/http;
import ballerina/websub;

listener http:Listener httpListener = new http:Listener(10012);

listener websub:Listener simpleListener = new(httpListener, {
    secureSocket: {
        key: {
            path: "tests/resources/ballerinaKeystore.pkcs12",
            password: "ballerina"
        }
    }
});

@websub:SubscriberServiceConfig {
}
service /sample on simpleListener {
    remote function onEventNotification(websub:ContentDistributionMessage event) {
        return;
    }
}

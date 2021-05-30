import ballerina/websub;
import ballerina/log;
import ballerina/jballerina.java;

isolated service class WebSubService {
    *websub:SubscriberService;

    public isolated function init(CustomWebhookService 'service) returns error? {
        self.externInit('service);
    }

    isolated remote function onEventNotification(websub:ContentDistributionMessage event)
                        returns websub:Acknowledgement|websub:SubscriptionDeletedError|error? {
        log:printInfo("onEventNotification invoked ", contentDistributionMessage = event);
        if (event.content is json) {
            Payload payload = check event.content.cloneWithType(Payload);
            string eventType = payload["eventType"];
            json eventData = payload["eventData"];
            log:printInfo("Received data ", data = eventData);
            match (eventType) {
                "start" => {
                    StartupMessage message = check eventData.cloneWithType(StartupMessage);
                    var response = self.callOnStartupMethod(message);
                    if (response is StartupError) {
                        return error websub:SubscriptionDeletedError(response.message());
                    }
                }
                "notify" => {
                    EventNotification message = check eventData.cloneWithType(EventNotification);
                    var response = self.callOnEventMethod(message);
                }
                _ => {}
            }
        }

        return {};
    }

    isolated function externInit(CustomWebhookService 'service) = @java:Method {
        'class: "io.ballerinax.webhook.NativeWebhookAdaptor"
    } external;

    isolated function callOnStartupMethod(StartupMessage msg)
                                    returns Acknowledgement|StartupError? = @java:Method {
        'class: "io.ballerinax.webhook.NativeWebhookAdaptor"
    } external;

    isolated function callOnEventMethod(EventNotification msg)
                                    returns Acknowledgement? = @java:Method {
        'class: "io.ballerinax.webhook.NativeWebhookAdaptor"
    } external;
}

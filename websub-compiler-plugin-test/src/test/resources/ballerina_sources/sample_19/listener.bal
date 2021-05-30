import ballerina/websub;
import ballerina/http;

public class Listener {
    private websub:Listener subscriberListener;
    private websub:SubscriberService? subscriberService;

    public isolated function init(int|http:Listener listenTo, *websub:ListenerConfiguration config) returns error? {
        self.subscriberListener = check new(listenTo, config);
        self.subscriberService = ();
    }

    public isolated function attach(CustomWebhookService s, string[]|string? name = ()) returns error? {
        websub:SubscriberServiceConfiguration? configuration = retrieveSubscriberServiceAnnotations(s);
        if (configuration is websub:SubscriberServiceConfiguration) {
            self.subscriberService = check new WebSubService(s);
            check self.subscriberListener.attachWithConfig(<websub:SubscriberService>self.subscriberService, configuration, name);
        } else {
            return error ListenerError("Could not find the required service-configurations");
        }
    }

    public isolated function detach(CustomWebhookService s) returns error? {
        check self.subscriberListener.detach(<websub:SubscriberService>self.subscriberService);
    }

    public isolated function 'start() returns error? {
        check self.subscriberListener.'start();
    }

    public isolated function gracefulStop() returns error? {
        return self.subscriberListener.gracefulStop();
    }

    public isolated function immediateStop() returns error? {
        return self.subscriberListener.immediateStop();
    }
}

isolated function retrieveSubscriberServiceAnnotations(CustomWebhookService serviceType) returns websub:SubscriberServiceConfiguration? {
    typedesc<any> serviceTypedesc = typeof serviceType;
    return serviceTypedesc.@websub:SubscriberServiceConfig;
}

import ballerina/websub;
import ballerina/log;

listener websub:Listener simpleListener = new (10004);

@websub:SubscriberServiceConfig{}
service /sample on simpleListener {
    remote function onEventNotification(websub:ContentDistributionMessage event)
                        returns websub:Acknowledgement|websub:SubscriptionDeletedError? {
        log:printInfo("onEventNotification invoked ", contentDistributionMessage = event);
        return {};
    }

    remote function onNewEvent(map<string> params) returns websub:Acknowledgement {
        return websub:ACKNOWLEDGEMENT;
    }
}
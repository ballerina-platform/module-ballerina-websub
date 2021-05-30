public type Payload record {|
    string eventType;
    json eventData;
|};
public type StartupMessage record {
    string hubName;
    string subscriberId;
};

public type EventNotification record {
    string hubName;
    string eventId;
    string message;
};

type CommonResponse record {|
    map<string|string[]> headers?;
    map<string> body?;
|};

public type Acknowledgement record {
    *CommonResponse;
};

public type Error distinct error<CommonResponse>;
public type ListenerError distinct Error;
public type StartupError distinct Error;

public type CustomWebhookService service object {
    remote function onStartup(StartupMessage message) returns Acknowledgement|StartupError?;

    remote function onEvent(EventNotification message) returns Acknowledgement?;
};

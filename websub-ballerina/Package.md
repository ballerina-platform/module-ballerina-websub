## Module Overview

This module contains an implementation of the W3C [**WebSub**](https://www.w3.org/TR/websub/) recommendation, which facilitates a push-based content delivery/notification mechanism between publishers and subscribers.

This implementation supports introducing WebSub Subscriber, A party interested in receiving update notifications for particular topics.

### Basic flow with WebSub
1. The subscriber discovers from the publisher, the topic it needs to subscribe to and the hub(s) that deliver notifications on updates of the topic.

2. The subscriber sends a subscription request to one or more discovered hub(s) specifying the discovered topic along 
 with other subscription parameters such as:
    - The callback URL to which content is expected to be delivered.
    - (Optional) The lease period (in seconds) the subscriber wants the subscription to stay active.
    - (Optional) A secret to use for [authenticated content distribution](https://www.w3.org/TR/websub/#signing-content).
  
3. The hub sends an intent verification request to the specified callback URL. If the response indicates 
verification
 (by echoing a challenge specified in the request) by the subscriber, the subscription is added for the topic at the 
 hub.
   
4. The publisher notifies the hub of updates to the topic and the content to deliver is identified.

5. The hub delivers the identified content to the subscribers of the topic.

### Features

#### Subscriber

This module allows introducing a WebSub Subscriber Service with `onSubscriptionVerification`, which accepts HTTP GET requests for intent verification, and `onEventNotification`, which accepts HTTP POST requests for notifications. The WebSub Subscriber Service provides the following capabilities:
 - When the service is started a subscription request is sent for a hub/topic combination, either specified as annotations or discovered based on the resource URL specified as an annotation.
 - If `onSubscriptionVerification` is not specified, intent verification will be done automatically against the topic specified as an annotation or discovered based on the resource URL specified as an annotation.
 - If a secret is specified for the subscription, signature validation will be done for authenticated content distribution.

    ```ballerina
    @websub:SubscriberServiceConfig {	
        path: "/websub",	
        subscribeOnStartUp: true,	
        target: ["<HUB_URL>", "<TOPIC_URL>"],	
        leaseSeconds: 3600,	
        secret: "<SECRET>"	
    }	
    service websubSubscriber on websubEP {	
        remote function onEventNotification(websub:ContentDistributionMessage event) {	
            //...
        }	
    }
    ```
   
   > Explicit intent verification can be done by introducing an `onSubscriptionVerification` resource function.
 
    ```ballerina
    remote function onSubscriptionVerification(websub:SubscriptionVerification msg)
                            returns websub:SubscriptionVerificationSuccess|SubscriptionVerificationError { 	
        // Insert the logic to build subscription/unsubscription intent verification response	
        // and return the value.
    }
   ```
 
Functions are made available on the `websub:SubscriptionVerification` to build a subscription or unsubscription 
verification response, specifying the topic to verify intent against:
```ballerina
var result = check msg.verifySubscription("http://localtopic.com");
```

### [Temporarily Disabled in Alpha] Introducing Specific Subscriber Services (Webhook Callback Services)

Ballerina's WebSub subscriber service listener can be extended to introduce specific Webhooks.
 
Instead of the single `onNotification` resource, you can introduce multiple resources to accept content delivery requests using specific subscriber services. These resources will correspond to the content delivery requests that will 
 be delivered with respect to a particular topic.
 
For example, assume a scenario in which you receive notifications either when an issue is opened or when an issue is closed by subscribing to a particular topic in an issue tracking system. With a custom subscriber service listener, which extends the 
generic WebSub subscriber service listener, you can allow two resources to accept content delivery requests (e.g., `onIssueOpened` and `onIssueClosed`) instead of the `onNotification` resource.

These resources will accept two parameters:
1. The generic `websub:Notification` record as the first parameter
2. A custom record corresponding to the expected (JSON) payload of the notification (e.g., `IssueCreatedEvent`,
`IssueClosedEvent`)

You can introduce a specific service as such by extending the generic subscriber service listener, specifying a 
mapping between the expected notifications and the resources that requests need to be dispatched to.

The mapping can be based on one of the following indicators of a notification request. 
(Requests will then be dispatched based on the value of the indicator in the request and a pre-defined mapping.)

- A request header 

    Dispatching will be based on the value of the request header specified as `topicHeader`. 
    
    ```ballerina
    websub:ExtensionConfig extensionConfig = {
        topicIdentifier: websub:TOPIC_ID_HEADER,
        topicHeader: "<HEADER_TO_CONSIDER>",
        headerResourceMap: {
            "issueOpened": ["onIssueOpened", IssueOpenedEvent],
            "issueClosed": ["onIssueClosed", IssueClosedEvent]
        }
    };
    ```
    
    The `"issueOpened": ["onIssueOpened", IssueOpenedEvent]` entry indicates that when the value of the
    `<HEADER_TO_CONSIDER>` header is `issueOpened`, dispatching should happen to a resource named `onIssueOpened`. 
    
    The first parameter of this resource will be the generic `websub:Notification` record, and the second parameter will 
    be a custom `IssueOpenedEvent` record mapping the JSON payload received when an issue is created.

- The payload: the value of a particular key in the JSON payload

    Dispatching will be based on the value in the request payload of one of the map keys specified in the 
    `payloadKeyResourceMap` map.
    
    ```ballerina
    websub:ExtensionConfig extensionConfig = {
        topicIdentifier: websub:TOPIC_ID_PAYLOAD_KEY,
        payloadKeyResourceMap: {
            "<PAYLOAD_KEY_TO_CONSIDER>": {
                "issueOpened": ["onIssueOpened", IssueOpenedEvent],
                "issueClosed": ["onIssueClosed", IssueClosedEvent]
            }
        }
    };
    ```
    
    The `"issueOpened": ["onIssueOpened", IssueOpenedEvent]` entry indicates that when the value for the JSON payload
     key `<PAYLOAD_KEY_TO_CONSIDER>` is `issueOpened`, dispatching should happen to a resource named `onIssueOpened`.
    
    The first parameter of this resource will be the generic `websub:Notification` record, and the second parameter will 
    be a custom `IssueOpenedEvent` record, mapping the JSON payload received when an issue is created.  

- A request header and the payload (combination of the above two)

    Dispatching will be based on both a request header and the payload as specified in the `headerAndPayloadKeyResourceMap`. 
    Also, you can introduce a `headerResourceMap` and/or a `payloadKeyResourceMap` as additional mappings.
     
    ```ballerina
    websub:ExtensionConfig extensionConfig = {
        topicIdentifier: websub:TOPIC_ID_HEADER_AND_PAYLOAD,
        topicHeader: "<HEADER_TO_CONSIDER>",
        headerAndPayloadKeyResourceMap: {
            "issue" : {
                "<PAYLOAD_KEY_TO_CONSIDER>" : {
                    "opened": ["onIssueOpened", IssueOpenedEvent],
                    "closed": ["onIssueClosed", IssueClosedEvent]
                }
            }
        }
    };
    ```
    The `"opened": ["onIssueOpened", IssueOpenedEvent]` entry indicates that when the value of the
    `<HEADER_TO_CONSIDER>` header is `issue` and the value of the `<PAYLOAD_KEY_TO_CONSIDER>` JSON payload key is `opened`, 
    dispatching should happen to a resource named `onIssueOpened`.

    The first parameter of this resource will be the generic `websub:Notification` record and the second parameter will 
    be a custom `IssueOpenedEvent` record, mapping the JSON payload received when an issue is created. 
     
#### [Temporarily Disabled in Alpha] The Specific Subscriber Service

In order to introduce a specific subscriber service, a new Ballerina `listener` needs to be introduced. This `listener` should wrap the generic `ballerina/websub:Listener` and include the extension configuration described above.

The following example is for a service provider that
- allows registering webhooks to receive notifications when an issue is opened or assigned
- includes a header named "Event-Header" in each content delivery request indicating what event the notification is 
for (e.g., "onIssueOpened" when an issue is opened and "onIssueAssigned" when an issue is assigned)

```ballerina
import ballerina/lang.'object as objects;
import ballerina/websub;

// Introduce a record mapping the JSON payload received when an issue is opened.
public type IssueOpenedEvent record {
    int id;
    string title;
    string openedBy;
}; 

// Introduce a record mapping the JSON payload received when an issue is assigned.
public type IssueAssignedEvent record {
    int id;
    string assignedTo;
}; 

// Introduce a new `listener` wrapping the generic `ballerina/websub:Listener` 
public class WebhookListener {

    *objects:Listener;

    private websub:Listener websubListener;

    public function init(int port) {
        // Introduce the extension config, based on the mapping details.
        websub:ExtensionConfig extensionConfig = {
            topicIdentifier: websub:TOPIC_ID_HEADER,
            topicHeader: "Event-Header",
            headerResourceMap: {
                "issueOpened": ["onIssueOpened", IssueOpenedEvent],
                "issueAssigned": ["onIssueAssigned", IssueAssignedEvent]
            }
        };
        
        // Set the extension config in the generic `websub:Listener` config.
        websub:SubscriberListenerConfiguration sseConfig = {
            extensionConfig: extensionConfig
        };
            
        // Initialize the wrapped generic listener.
        self.websubListener = new(port, sseConfig);
    }

    public function __attach(service s, string? name = ()) returns error?  {
        return self.websubListener.__attach(s, name);
    }

    public function __start() returns error? {
        return self.websubListener.__start();
    }
    
    public function __detach(service s) returns error? {
        return self.websubListener.__detach(s);
    }
    
    public function __immediateStop() returns error? {
        return self.websubListener.__immediateStop();
    }

    public function __gracefulStop() returns error? {
        return self.websubListener.__gracefulStop();
    }
};    
```

A service can be introduced for the above service provider as follows.
```ballerina
import ballerina/io;
import ballerina/log;
import ballerina/websub;

@websub:SubscriberServiceConfig {
    path: "/subscriber",
    subscribeOnStartUp: false
}
service specificSubscriber on new WebhookListener(8080) {
    resource function onIssueOpened(websub:Notification notification, IssueOpenedEvent issueOpened) {
        log:print(io:sprintf("Issue opened: ID: %s, Title: %s", issueOpened.id, issueOpened.title));
    }
    
    resource function onIssueAssigned(websub:Notification notification, IssueAssignedEvent issueAssigned) {
        log:print(io:sprintf("Issue ID %s assigned to %s", issueAssigned.id, issueAssigned.assignedTo));
    }
}
```

For a step-by-step guide on introducing custom subscriber services, see the ["Create Webhook Callback Services"](https://ballerina.io/learn/how-to-extend-ballerina/#create-webhook-callback-services) section of "How to Extend Ballerina". 
 

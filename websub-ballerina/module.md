## Module Overview

This module provides an implementation for WebSub Subscriber Service.

[**WebSub**](https://www.w3.org/TR/websub/) is a common mechanism for communication between publisher of any kind of Web content and their subscribers, based on HTTP web hooks. Subscription requests are relayed through hubs, which validate and verify the request. Hubs then distribute new and updated content to subscribers when it becomes available. WebSub was previously known as PubSubHubbub.

A WebSub Subscriber is an implementation that discovers the `hub` and `topic URL` given a `resource URL`, subscribes to updates at the hub, and accepts content distribution requests from the `hub`.

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

#### Subscribing

WebSub Subscriber provides mechanism to subscribe in a `hub` to a given `topic URL`. 
```ballerina
@websub:SubscriberServiceConfig {
    target: ["<HUB_URL>", "<TOPIC_URL>"], 
    leaseSeconds: 36000
} 
service /subscriber on new websub:Listener(9090) {
    remote function onSubscriptionValidationDenied(websub:SubscriptionDeniedError msg) returns websub:Acknowledgement? {
        // implement subscription validation denied logic here
        return websub:ACKNOWLEDGEMENT;
    }

    remote function onSubscriptionVerification(websub:SubscriptionVerification msg)
                        returns websub:SubscriptionVerificationSuccess|websub:SubscriptionVerificationError {
        // implement subscription intent verification logic here
        return websub:SUBSCRIPTION_VERIFICATION_SUCCESS;
    }

    remote function onEventNotification(websub:ContentDistributionMessage event) 
                        returns websub:Acknowledgement|websub:SubscriptionDeletedError? {
        // implement on event notification logic here
        return websub:ACKNOWLEDGEMENT;
    }
}
```

#### Resource Discovery

WebSub Subscriber also provides the mechanism to discover `hub` and `topic URL` resources dynamically via provided `resource URL` and initiate subscription.
```ballerina
@websub:SubscriberServiceConfig {
    target: "RESOURCE_URL", 
    leaseSeconds: 36000
} 
service /subscriber on new websub:Listener(9090) {
    remote function onEventNotification(websub:ContentDistributionMessage event) 
                        returns websub:Acknowledgement|websub:SubscriptionDeletedError? {
        // implement on event notification logic here
        return websub:ACKNOWLEDGEMENT;
    }

    // other remote methods are optional to be implemented
}
```

#### Dynamic URI generation

Service path for a WebSub Subscriber is optional. WebSub Subscriber service has the capability to generate service path dyanmically.
```ballerina
@websub:SubscriberServiceConfig {
    target: "RESOURCE_URL", 
    leaseSeconds: 36000
} 
service on new websub:Listener(9090) {
    remote function onEventNotification(websub:ContentDistributionMessage event) 
                        returns websub:Acknowledgement|websub:SubscriptionDeletedError? {
        // implement on event notification logic here
        return websub:ACKNOWLEDGEMENT;
    }

    // other remote methods are optional to be implemented
}
```
## Package Overview

This package contains an implementation of the W3C [**WebSub**](https://www.w3.org/TR/websub/) recommendation, which facilitates a push-based content delivery/notification mechanism between publishers and subscribers.

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

This package allows introducing a WebSub Subscriber Service with `onSubscriptionVerification`, which accepts HTTP GET requests for intent verification, `onSubscriptionValidationDenied` which accepts subscription denied response from hub and `onEventNotification`, which accepts HTTP POST requests for notifications. The WebSub Subscriber Service provides the following capabilities:
 - When the service is started a subscription request is sent for a hub/topic combination, either specified as annotations or discovered based on the resource URL specified as an annotation.
 - If `onSubscriptionVerification` is not specified, intent verification will be done automatically against the topic specified as an annotation or discovered based on the resource URL specified as an annotation.
 - If `onSubscriptionValidationDenied` is not specified, subscriber service will respond to the incoming request automatically.
 - If `target` is not specified the initial subscription will not happen on service startup.
 - If a `secret` is specified for the subscription, signature validation will be done for authenticated content distribution.

    ```ballerina
    websub:ListenerConfiguration listenerConfigs = {
        secureSocket: {
            key: {
                certFile: "../resource/path/to/public.crt",
                keyFile: "../resource/path/to/private.key"
            }
        }
    };

    listener websub:Listener sslEnabledListener = new(9095, listenerConfigs);

    @websub:SubscriberServiceConfig {
        target: ["<HUB_URL>", "<TOPIC_URL>"], 
        leaseSeconds: 36000,
        secret: "<SECRET>"
    } 
    service /subscriber on sslEnabledListener {
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
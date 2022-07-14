Ballerina WebSub Library
===================

  [![Build](https://github.com/ballerina-platform/module-ballerina-websub/actions/workflows/build-timestamped-master.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-websub/actions/workflows/build-timestamped-master.yml)
  [![Trivy](https://github.com/ballerina-platform/module-ballerina-websub/actions/workflows/trivy-scan.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-websub/actions/workflows/trivy-scan.yml)  
  [![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerina-websub.svg)](https://github.com/ballerina-platform/module-ballerina-websub/commits/master)
  [![Github issues](https://img.shields.io/github/issues/ballerina-platform/ballerina-standard-library/module/websub.svg?label=Open%20Issues)](https://github.com/ballerina-platform/ballerina-standard-library/labels/module%2Fwebsub)
  [![codecov](https://codecov.io/gh/ballerina-platform/module-ballerina-websub/branch/master/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerina-websub)

This library provides APIs for a WebSub Subscriber Service.

[**WebSub**](https://www.w3.org/TR/websub/) is a common mechanism for communication between publishers of any kind of Web content and their subscribers, based on HTTP webhooks. Subscription requests are relayed through hubs, which validate and verify the request. Hubs then distribute new and updated content to subscribers when it becomes available. WebSub was previously known as PubSubHubbub.

[**WebSub Subscriber**](https://www.w3.org/TR/websub/#subscriber) is an implementation that discovers the `hub` and `topic URL` of a given `resource URL`, subscribes to updates at the hub, and accepts content distribution requests from the `hub`.

### Basic flow with WebSub

1. The subscriber discovers (from the publisher) the topic it needs to subscribe to and the hub(s) that deliver notifications on the updates of the topic.

2. The subscriber sends a subscription request to one or more discovered hub(s) specifying the discovered topic along 
 with the other subscription parameters such as:
    - The callback URL to which the content is expected to be delivered.
    - (Optional) The lease period (in seconds) the subscriber wants the subscription to stay active.
    - (Optional) A secret to use for the [authenticated content distribution](https://www.w3.org/TR/websub/#signing-content).
  
3. The hub sends an intent verification request to the specified callback URL. If the response indicates 
the verification
 (by echoing a challenge specified in the request) by the subscriber, the subscription is added for the topic at the 
 hub.
   
4. The publisher notifies the hub of the updates to the topic and the content to deliver is identified.

5. The hub delivers the identified content to the subscribers of the topic.

#### Subscribe to a `hub`

* The WebSub Subscriber provides the mechanism to subscribe in a `hub` to a given `topic URL`. 
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

    remote function onUnsubscriptionVerification(websub:UnsubscriptionVerification msg)
                        returns websub:UnsubscriptionVerificationSuccess|websub:UnsubscriptionVerificationError {
        // implement unsubscription intent verification logic here
        return websub:UNSUBSCRIPTION_VERIFICATION_SUCCESS;
    }

    remote function onEventNotification(websub:ContentDistributionMessage event) 
                        returns websub:Acknowledgement|websub:SubscriptionDeletedError? {
        // implement on event notification logic here
        return websub:ACKNOWLEDGEMENT;
    }
}
```

#### Resource discovery

* The WebSub Subscriber also provides the mechanism to discover the `hub` and `topic URL` resources dynamically via the provided `resource URL` and initiates the subscription.
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

* The service path for a WebSub Subscriber is optional. The WebSub Subscriber service has the capability to generate the service path dynamically.
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

#### Run a `websub:SubscriberService` locally

* [**ngrok**](https://ngrok.com/) is a TCP Tunneling software, which is used to expose services running locally in the public network.
* If you want to run the subscriber service in your local machine, you could use **ngrok** to expose it to the public network.
* First, [download and install](https://ngrok.com/download) **ngrok**.
* Run the following command to expose the local port `9090` to the public network via `HTTPS`. For information, see the [ngrok documentation](https://ngrok.com/docs#http-bind-tls)).
```bash
ngrok http -bind-tls=true 9090
```
* Extract the public URL provided by **ngrok** and provide it as the callback URL for the subscriber service.
```ballerina
@websub:SubscriberServiceConfig {
    target: "RESOURCE_URL", 
    leaseSeconds: 36000,
    callback: "<NGROK_PUBLIC_URL>",
    appendServicePath: true
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

#### Unsubscribe from the `hub`

* The WebSub Subscriber has the capability to initiate unsubscription flow on Subscriber termination.
```ballerina
websub:ListenerConfiguration listenerConfigs = {
    gracefulShutdownPeriod: 15
};

@websub:SubscriberServiceConfig {
    target: ["https://sample.hub.com", "https://sample.topic1.com"], 
    leaseSeconds: 36000,
    // By default this is set to `false`, hence subscriber on default mode would not initiate unsubscription flow
    unsubscribeOnShutdown: true
}
service /subscriber on new websub:Listener(9090, listenerConfigs)  {
    isolated remote function onEventNotification(websub:ContentDistributionMessage event) 
                        returns websub:Acknowledgement {
        // implement logic here
        return websub:ACKNOWLEDGEMENT;
    }

    // other remote methods are optional to be implemented
}
``` 

#### Return errors from remote methods

* Remote functions in `websub:SubscriberService` can return `error` type.
```ballerina
@websub:SubscriberServiceConfig {
    target: "RESOURCE_URL", 
    leaseSeconds: 36000,
    callback: "<NGROK_PUBLIC_URL>",
    appendServicePath: true
} 
service on new websub:Listener(9090) {
    remote function onEventNotification(websub:ContentDistributionMessage event) 
                        returns websub:Acknowledgement|websub:SubscriptionDeletedError|error? {
        boolean isValidRequest = check validateRequest(event);
        if isValidRequest {
            // implement on event notification logic here
            return websub:ACKNOWLEDGEMENT;
        }
    
    }

    // other remote methods are optional to be implemented
}

function validateRequest(websub:ContentDistributionMessage event) returns boolean|error {
    // validation logic 
}
```

* For each remote method `error` return has a different meaning. Following table depicts the meaning inferred from `error` returned from all available remote methods.

| Method        | Interpreted meaning for Error Return |
| ----------- | ---------------- |
| onSubscriptionValidationDenied | Successfull acknowledgement|
| onSubscriptionVerification | Subscription verification failure|
| onUnsubscriptionVerification | Unsubscription verification failure|
| onEventNotification | Successfull acknowledgement|

## Issues and projects 

Issues and Projects tabs are disabled for this repository as this is part of the Ballerina Standard Library. To report bugs, request new features, start new discussions, view project boards, etc., go to the [Ballerina Standard Library parent repository](https://github.com/ballerina-platform/ballerina-standard-library).

This repository only contains the source code for the package.

## Build from the source

### Set up the prerequisites

* Download and install Java SE Development Kit (JDK) version 11 (from one of the following locations).

   * [Oracle](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
   
   * [OpenJDK](https://adoptium.net/)
   
        > **Note:** Set the JAVA_HOME environment variable to the path name of the directory into which you installed JDK.
     
### Build the source

Execute the commands below to build from source.

1. To build the package:
```        
        ./gradlew clean build
```

2. To build the package without the tests:
```
        ./gradlew clean build -x test
```

3. To debug the package tests:

        ./gradlew clean build -Pdebug=<port>

4. To run a group of tests
    ```
    ./gradlew clean test -Pgroups=<test_group_names>
    ```

5. To debug with Ballerina language:
    ```
    ./gradlew clean build -PbalJavaDebug=<port>
    ```

6. Publish the generated artifacts to the local Ballerina central repository:
    ```
    ./gradlew clean build -PpublishToLocalCentral=true
    ```

7. Publish the generated artifacts to the Ballerina central repository:
    ```
    ./gradlew clean build -PpublishToCentral=true
    ```

## Contribute to Ballerina

As an open source project, Ballerina welcomes contributions from the community.

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful links

* For more information, go to the [`websub` module](https://lib.ballerina.io/ballerina/websub/latest).
* For example demonstrations of the usage, go to [Ballerina By Examples](https://ballerina.io/learn/by-example/).
* Chat live with us via our [Slack channel](https://ballerina.io/community/slack/).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
* View the [Ballerina performance test results](performance/benchmarks/summary.md).

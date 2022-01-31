# Specification: Ballerina WebSub Library

_Owners_: @shafreenAnfar @chamil321 @ayeshLK    
_Reviewers_: @shafreenAnfar    
_Created_: 2022/01/31  
_Updated_:   
_Issue_: [#786](https://github.com/ballerina-platform/ballerina-standard-library/issues/786)

# Introduction

This is the specification for WebSub standard library which is used to implement WebSub compliant `subscriber` services
using [Ballerina programming language](https://ballerina.io/), which is an open-source programming language for the 
cloud that makes it easier to use, combine, and create network services.  

# Content
1. [Overview](#1-overview)
2. [Subscriber](#2-subscriber)
    * 2.1. [Listener](#21-listener)
      * 2.1.1. [Configuration](#211-configuration)
      * 2.1.2. [Initialization](#212-initialization)
      * 2.1.3. [Methods](#213-methods)
    * 2.2 [Subscriber Service](#22-subscriber-service)
      * 2.2.1. [Methods](#221-methods)
        * 2.2.1.1. [onSubscriptionValidationDenied](#2211-onsubscriptionvalidationdenied)
        * 2.2.1.2. [onSubscriptionVerification](#2212-onsubscriptionverification)
        * 2.2.1.3. [onUnsubscriptionVerification](#2213-onunsubscriptionverification)
        * 2.2.1.1. [onEventNotification](#2214-oneventnotification)
      * 2.2.2. [Annotation](#222-annotation)

## 1. Overview

[WebSub](https://www.w3.org/TR/websub/) is a real-time content delivery protocol over HTTP(S) and it is a specification
which evolved from [PubSubHubbub](https://github.com/pubsubhubbub/PubSubHubbub).

WebSub specification describes three main roles:
- Publisher: Advertises a `topic` and `hub` URL on one or more resource URLs.
- Subscriber: Discovers the `hub` and `topic` URL given a resource URL, subscribes to updates at the `hub`, and accepts
  content distribution requests from the `hub`.
- Hub: Handles subscription requests and distributes the content to subscribers when the corresponding topic URL has
  been updated.

`WebSub` is a library which is derived from the WebSub specification which could be used by developers to implement
WebSub compliant `subscriber` services.  

## 2. Subscriber

WebSub `subscriber` will subscribe to `hub` to receive content updates for a `topic`.  

It has the following capabilities:
* Discover the `hub` and the `topic` given a resource URL
* Subscribe to content updates for a `topic` in a `hub`
* Accept content distribution requests from the `hub`

The `subscriber` is designed in the form of `listener` and `Subscriber Service`.
- `websub:Listener`: A listener end-point to which `websub:SubscriberService` could be attached.
- `websub:SubscriberService`: An API service, which receives WebSub events.

### 2.1. Listener

The `websub:Listener` opens the given port and attaches the provided `websub:SubscriberService` object to the given
service-path. `websub:Listener` can be initialized either by providing a port with listener configurations or by
providing an `http:Listener`.

#### 2.1.1. Configuration

When initializing a `websub:Listener`, following configurations could be provided.
```ballerina
# Provides a set of configurations for configure the underlying HTTP listener of the WebSub listener.
# 
# + gracefulShutdownPeriod - The time period in seconds to wait for unsubscription verification
public type ListenerConfiguration record {|
    *http:ListenerConfiguration;
    decimal gracefulShutdownPeriod = 20;
|};
```

For more details on the available configurations please refer [`http:ListenerConfiguration`](https://lib.ballerina.io/ballerina/http/latest/records/ListenerConfiguration).

#### 2.1.2. Initialization

The `websub:Listener` could be initialized by providing either a port with `websub:ListenerConfiguration` or by
providing an `http:Listener`.
```ballerina
# Initiliazes `websub:Listener` instance.
# ```ballerina
# listener websub:Listener websubListenerEp = check new (9090);
# ```
#
# + listenTo - Port number or a `http:Listener` instance
# + config - Custom `websub:ListenerConfiguration` to be provided to underlying HTTP Listener
# + return - The `websub:Listener` or an `websub:Error` if the initialization failed
public isolated function init(int|http:Listener listenTo, *ListenerConfiguration config) returns websub:Error? 
```

#### 2.1.3. Methods

Following APIs should be available in the `websub:Listener` to dynamically attach `websub:SubscriberService` objects to 
it.
```ballerina
# Attaches the provided `websub:SubscriberService` to the `websub:Listener`.
# ```ballerina
# check websubListenerEp.attach('service, "/subscriber");
# ```
# 
# + service - The `websub:SubscriberService` object to attach
# + name - The path of the Service to be hosted
# + return - An `websub:Error`, if an error occurred during the service attaching process or else `()`
public isolated function attach(websub:SubscriberService 'service, string[]|string? name = ()) returns websub:Error?
```

Following APIs should be available in the `websub:Listener` to dynamically attach `websub:SubscriberService` objects 
along with `websub:SubscriberServiceConfiguration`. This is useful when the `subscriber` is implemented using 
`websub:SubscriberService` service class.
```ballerina
# Attaches the provided Service to the `websub:Listener` with custom `websub:SubscriberServiceConfiguration`.
# ```ballerina
# check websubListenerEp.attachWithConfig('service, {
#    target: "http://0.0.0.0:9191/common/discovery",
#    leaseSeconds: 36000
# }, "/subscriber");
# ```
# 
# + service - The `websub:SubscriberService` object to attach
# + configuration - Custom `websub:SubscriberServiceConfiguration` which should be incorporated into the provided Service 
# + name - The path of the Service to be hosted
# + return - An `websub:Error`, if an error occurred during the service attaching process or else `()`
public isolated function attachWithConfig(websub:SubscriberService 'service, websub:SubscriberServiceConfiguration configuration, string[]|string? name = ()) returns websub:Error?
```

Following APIs should be available in the `websub:Listener` to dynamically detach `websub:SubscriberService` objects 
from it.
```ballerina
# Detaches the provided `websub:SubscriberService` from the `websub:Listener`.
# ```ballerina
# check websubListenerEp.detach('service);
# ```
# 
# + service - The `websub:SubscriberService` object to be detached
# + return - An `websub:Error`, if an error occurred during the service detaching process or else `()`
public isolated function detach(websub:SubscriberService 'service) returns websub:Error?
```

Following APIs should be available to dynamically start the `websub:Listener`.
```ballerina
# Starts the registered service programmatically..
# ```ballerina
# check websubListenerEp.'start();
# ```
# 
# + return - An `websub:Error`, if an error occurred during the listener starting process or else `()`
public isolated function 'start() returns websub:Error?
```

Following APIs should be available to dynamically stop the `websub:Listener`.
```ballerina
# Stops the service listener gracefully. Already-accepted requests will be served before connection closure.
# ```ballerina
# check websubListenerEp.gracefulStop();
# ```
# 
# + return - An `websub:Error`, if an error occurred during the listener stopping process or else `()`
public isolated function gracefulStop() returns websub:Error?

# Stops the service listener immediately.
# ```ballerina
# check websubListenerEp.immediateStop();
# ```
# 
# + return - An `websub:Error`, if an error occurred during the listener stopping process or else `()`
public isolated function immediateStop() returns websub:Error?
```

### 2.2. Subscriber Service

`websub:SubscriberService` is responsible for handling the received events. Underlying `http:Service` will receive the 
original request, and then it will trigger the WebSub dispatcher which will invoke the respective remote method with the 
event details.

Following is the type-definition for `websub:SubscriberService`.
```ballerina
public type SubscriberService distinct service object {
    // Sample GET request hub.mode=denied&hub.reason=unauthorized
    // Sample 200 OK response
    remote function onSubscriptionValidationDenied(websub:SubscriptionDeniedError msg)
        returns websub:Acknowledgement|error?;

    // Sample GET request hub.mode=subscribe&hub.topic=test&hub.challenge=1234
    // Sample 200 OK response with text payload containing received `hub.challenge` parameter or 404 NOT FOUND
    remote function onSubscriptionVerification(websub:SubscriptionVerification msg)
        returns websub:SubscriptionVerificationSuccess|websub:SubscriptionVerificationError|error;

    // Sample GET request hub.mode=unsubscribe&hub.topic=test&hub.challenge=1234
    // Sample 200 OK response with text payload containing received `hub.challenge` parameter or 404 NOT FOUND
    remote function onUnsubscriptionVerification(websub:UnsubscriptionVerification msg)
        returns websub:UnsubscriptionVerificationSuccess|websub:UnsubscriptionVerificationError|error;

    // Sample POST request with string/json/xml payload
    // Sample 202 ACCEPTED response or 410 GONE
    remote function onEventNotification(websub:ContentDistributionMessage event)
        returns websub:Acknowledgement|websub:SubscriptionDeletedError|error?;
};
```

#### 2.2.1. Methods

##### 2.2.1.1. onSubscriptionValidationDenied

This remote method is invoked when the `hub` sends a request to notify that the subscription request is denied.
```ballerina
# Notifies that the subscription request is denied by the `hub`.
# 
# + msg - Details related to the subscription denial
# + return - `error` if there is any error when processing the reuqest or else `websub:Acknowledgement` or `()`
remote function onSubscriptionValidationDenied(websub:SubscriptionDeniedError msg) returns websub:Acknowledgement|error?;
```

##### 2.2.1.2. onSubscriptionVerification

This remote method is invoked when the `hub` sends a subscription verification request to the `subscriber`.
```ballerina
# Verifies the subscription attempt.
# 
# + msg - Details related to the subscription verificaiton
# + return - `websub:SubscriptionVerificationSuccess` if the subscription is verified successfully, 
#           `websub:SubscriptionVerificationError` if the subscription verification is unsuccessful or else `error` if 
#           there is an exception while executing the method
remote function onSubscriptionVerification(websub:SubscriptionVerification msg) 
    returns websub:SubscriptionVerificationSuccess|websub:SubscriptionVerificationError|error;
```

##### 2.2.1.3. onUnsubscriptionVerification

This remote method is invoked when the `hub` sends the unsubscription verification request to the `subscriber`.
```ballerina
# Verifies the unsubscription attempt.
# 
# + msg - Details related to the unsubscription verificaiton
# + return - `websub:UnsubscriptionVerificationSuccess` if the unsubscription is verified successfully, 
#           `websub:UnsubscriptionVerificationError` if the unsubscription verification is unsuccessful or else `error` if 
#           there is an exception while executing the method
remote function onUnsubscriptionVerification(websub:UnsubscriptionVerification msg) 
    returns websub:UnsubscriptionVerificationSuccess|websub:UnsubscriptionVerificationError|error;
```

##### 2.2.1.4. onEventNotification

This remote method is invoked when the `hub` sends the content-distribution request to the `subscriber`.
```ballerina
# Notifies the content distribution.
# 
# + msg - Received content distribution message
# + return - `websub:Acknowledgement` if the content received successfully, `websub:SubscriptionDeletedError` if the 
#           subscriber does not need any content updates in the future, `error` if  there is an exception while 
#           executing the method or else `()`
remote function onEventNotification(websub:ContentDistributionMessage event) 
    returns websub:Acknowledgement|websub:SubscriptionDeletedError|error?;
```

#### 2.2.2. Annotation 

Apart from the listener level configurations a `subscriber` will require few additional configurations. Hence, there 
should be `websub:SubscriberServiceConfig` a service-level-annotation for `websub:SubscriberService` which contains
`websub:SubscriberServiceConfiguration` record.
```ballerina
# Configuration for a WebSubSubscriber service.
#
# + target - The `string` resource URL for which discovery will be initiated to identify the hub and topic,
#            or a tuple `[hub, topic]` representing a discovered hub and a topic
# + leaseSeconds - The period for which the subscription is expected to be active
# + callback - The callback URL for subscriber-service
# + secret - The secret to be used for authenticated content distribution
# + appendServicePath - This flag notifies whether or not to append service-path to callback-url
# + unsubscribeOnShutdown - This flag notifies whether or not to initiate unsubscription when the service is shutting down
# + httpConfig - The configuration for the hub client used to interact with the discovered/specified hub
# + discoveryConfig - HTTP client configurations for resource discovery
public type SubscriberServiceConfiguration record {|
    string|[string, string] target?;
    int leaseSeconds?;
    string callback?;
    string secret?;
    boolean appendServicePath = false;
    boolean unsubscribeOnShutdown = false;
    http:ClientConfiguration httpConfig?;
    record {|
        string|string[] accept?;
        string|string[] acceptLanguage?;
        http:ClientConfiguration httpConfig?;
    |} discoveryConfig?;
|};
```
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

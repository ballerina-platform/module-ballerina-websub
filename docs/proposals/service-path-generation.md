# Proposal: Compile time unique service path generation for `websub:SubscriberService`

_Owners_: @shafreenAnfar @chamil321 @ayeshLK  
_Reviewers_: @shafreenAnfar @chamil321  
_Created_: 2021/11/02  
_Updated_: 2021/11/02  
_Issue_: [#2253](https://github.com/ballerina-platform/ballerina-standard-library/issues/2253)  

## Summary

Unique service path generation for `websub:SubscriberService` should happen at compile time.  

## Goals

* Compile time unique service path generation for `websub:SubscriberService`

## Motivation

As per the WebSub specification subscriber callback URL could be used as an identity of a `subscriber` at the `hub` level and it should be unguessable and unique for a subscription [1] [2]

Since the developer should have the control over the callback URL to be used when subscribing to a `hub`, the current ballerina websub framework has an optional configuration which could be used to provide a callback URL. It also has the feature to generate the callback URL  based on the provided service path and the host configurations if the callback URL is not provided as a configuration. 

In a scenario where both callback URL and service path is not provided by the developer, websub framework will generate a unique service path and construct the callback URL using the generated service path and provided host configurations. But currently unique service path generation happens at runtime and hence there will be different callback URLs constructed each time developer runs the subscriber service. Since the callback URL is used as an identity of a `subscriber` at the `hub` level, generating different callback URLs for the same `subscriber` would be problematic. 

If we could generate the unique service path at compile time, then the subscriber service will have the same callback URL each time the developer runs it.

## Description

As mentioned in the goals section the purpose of this proposal is to introduce compile time unique service path generation for `websub:SubscriberService`. 

The key attributes of the suggested changes are as follows,
- Service path should be generated only if
  - service path and callback URL is not provided for the `websub:SubscriberService`
  - callback URL is provided, `appendServicePath` configuration is enabled and service path is not provided
- WebSub compiler plugin should generate the unique service path for the `websub:SubscriberService`
- Generated service path should be saved in relation to service ID in `service-info.csv`
- `service-info.csv` file should be added to `resources/ballerina/websub` directory inside the executable JAR as well as the thin JAR
- If there is an error while generating the service path, then it should result in a compile time error since this feature is required to generate a callback URL and without it subscriber service could not be used

Following are a sample scenarios where unique service path should be generated.

1. Service path and Callback URL is not provided
```ballerina
@websub:SubscriberServiceConfig {
    target: ["https://sample.hub.com", "https://sample.topic1.com"], 
    leaseSeconds: 36000
}
service on subListener {
    isolated remote function onEventNotification(websub:ContentDistributionMessage event) 
                        returns websub:Acknowledgement {
        // implement logic here
        return websub:ACKNOWLEDGEMENT;
    }
}
```

2. callback URL is provided, `appendServicePath` configuration is enabled and service path is not provided
```ballerina
@websub:SubscriberServiceConfig {
    target: ["https://sample.hub.com", "https://sample.topic1.com"],
    callback: "https://sample.subscriber.com",
    appendServicePath: true 
    leaseSeconds: 36000
}
service on subListener {
    isolated remote function onEventNotification(websub:ContentDistributionMessage event) 
                        returns websub:Acknowledgement {
        // implement logic here
        return websub:ACKNOWLEDGEMENT;
    }
}
```

Following is a sample for generated service path.
```
/PkgS2xml97
```

With the proposed change there will be no API additions or removals.

## Testing

- Integration test should be added to ballerina distribution

## Future Work

The proposed solution uses a separate file to store the generated service path against the service id due to the limation of compiler plugin code injection. Once this limitation is fixed this generated path can be injected to the service path or attach directly to the service. If so this separate file is not needed.

## References

[1] - [https://www.w3.org/TR/websub/#definitions](https://www.w3.org/TR/websub/#definitions)  
[2] - [https://www.w3.org/TR/websub/#subscriber-sends-subscription-request](https://www.w3.org/TR/websub/#subscriber-sends-subscription-request)

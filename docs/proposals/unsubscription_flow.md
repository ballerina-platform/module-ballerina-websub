# Proposal: Implement Subscriber Unsubscription Flow

_Owners_: @shafreenAnfar @chamil321 @ayeshLK  
_Reviewers_: @shafreenAnfar    
_Created_: 2021/10/09  
_Updated_: 2021/10/09  
_Issue_: [#1843](https://github.com/ballerina-platform/ballerina-standard-library/issues/1843)

## Summary
`websub:SubscriberService` should initiate unsubscription flow when graceful stop invoked in `websub:Listener`.

## Goals
* Implement unsubscription flow for `websub:SubscriberService`

## Motivation
As per the WebSub Specification a `subscriber` should be able to subscribe to a particular `topic` in a specific `hub`. As the counterpart `subscriber` should be able to unsubscribe from a previously subscribed `topic` in a specific `hub`. Even though the spec does not clearly define when this unsubscription should happen, it is obvious that this should happen whenever `subscriber` is terminated.

In the current ballerina `websub` package, `websub:SubscriberService` will initiate a subscription request to the `hub` based on the provided configurations when it is initialized. But, since we do not have an unsubscription flow, it is observed that when we terminate a `subscriber`, developer can not re-start the subscriber hence the subscription is already there in the `hub`.

Therefore, implementing an unsubscription flow for `websub:SubcriberService` will improve the user experience of the ballerina `websub` framework.

## Description
As mentioned in the Goals section the purpose of this proposal is to integrate missing unsubscription flow to `websub:SubscriberService`.

The key functionalities expected when implementing unsubscription flow is as follows,
- Configuration should be introduced to manually disable auto-unsubscribe on shutdown
- Configuration should be introduced to have a time-out for the unsubscriptoin flow completion
- Introduce `onUnsubscriptionVerification` optional API to the `websub:SubscriberService`
- Unsubscription flow should initiate whenever graceful stop is invoked in `websub:Listener`
- If multiple `websub:SubscriberService` instances are attached to one `websub:Listener`, all the `subscriber` instances should initiate unsubscription on listener shutdown
- Unsubscription flow should initiate only if `graceful stop` is invoked, and for `immediate stop` this will not be executed

Following is an example for `websub:SubscriberService` declaration with proposed solution:
```ballerina
websub:ListenerConfiguration listenerConfigs = {
    gracefulShutdownPeriod: 15
};

@websub:SubscriberServiceConfig {
    target: ["https://sample.hub.com", "https://sample.topic1.com"], 
    leaseSeconds: 36000,
    unsubscribeOnShutdown: true
}
service /subscriber on new websub:Listener(9090, listenerConfigs)  {
    isolated remote function onEventNotification(websub:ContentDistributionMessage event) 
                        returns websub:Acknowledgement {
        // implement logic here
        return websub:ACKNOWLEDGEMENT;
    }
}
```

### API Additions
- Following new remote method will be introduced to `websub:SubscriberService` declaration:
```ballerina
// This will be an optional API to be implemented by the developer. 
// If this is not implemented then framework will reply automatically.
remote function onUnsubscriptionVerification(websub:UnsubscriptionVerification verification)
                        returns websub:UnsubscriptionVerificationSuccess|websub:UnsubscriptionVerificationError {
     // implement additional logic here 
}
```

- Following new records/errors will be introduced to ballerina `websub` package:
```ballerina
# Record representing the unsubscription intent verification request-body.
#
# + hubMode - The `hub.mode` parameter (unsubscribe)
# + hubTopic - The topic URL
# + hubChallenge - The `hub.challenge` parameter used for verification
# + hubLeaseSeconds - The `hub.lease_seconds` parameter used to validate the expiration of subscription
public type UnsubscriptionVerification record {
    string hubMode;
    string hubTopic;
    string hubChallenge;
    string? hubLeaseSeconds;
};

# Record representing the unsubscription intent verification success.
public type UnsubscriptionVerificationSuccess record {
    *CommonResponse;
};

# Represents an unsubscription verification error.
public type UnsubscriptionVerificationError distinct Error;
```

- `gracefulShutdownPeriod` configuration should be added to `websub:ListenerConfiguration`.
- `unsubscribeOnShutdown` configuration should be added to `websub:SubscriberServiceConfig`.

### Default Values
Since we are introducing two new configurations, we would use following default values:
```ballerina
// This will be turned on by default since every `subscriber` should be unsubscribe on termination.
unsubscribeOnShutdown = true

// This is the default timeout period to unsubscription verification. 
// If unsubscription verification happens before this time period then listener will not wait further more.
gracefulShutdownPeriod = 20 seconds
```

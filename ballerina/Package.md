## Package Overview

This package provides APIs for a WebSub Subscriber Service.

[**WebSub**](https://www.w3.org/TR/websub/) is a common mechanism for communication between publishers of any kind of Web content and their subscribers, based on HTTP webhooks. Subscription requests are relayed through hubs, which validate and verify the request. Hubs then distribute new and updated content to subscribers when it becomes available. WebSub was previously known as PubSubHubbub.

[**WebSub Subscriber**](https://www.w3.org/TR/websub/#subscriber) is an implementation that discovers the `hub` and `topic URL` of a given `resource URL`, subscribes to updates at the hub, and accepts content distribution requests from the `hub`.

## Report Issues

To report bugs, request new features, start new discussions, view project boards, etc., go to the <a target="_blank" href="https://github.com/ballerina-platform/ballerina-standard-library">Ballerina standard library parent repository</a>.

## Useful Links

* Chat live with us via our <a target="_blank" href="https://ballerina.io/community/slack/">Slack channel</a>.
* Post all technical questions on Stack Overflow with the <a target="_blank" href="https://stackoverflow.com/questions/tagged/ballerina">#ballerina</a> tag.

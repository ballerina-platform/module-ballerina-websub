Ballerina WebSub Library
===================

  [![Build](https://github.com/ballerina-platform/module-ballerina-websub/actions/workflows/build-timestamped-master.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-websub/actions/workflows/build-timestamped-master.yml)
  [![Trivy](https://github.com/ballerina-platform/module-ballerina-websub/actions/workflows/trivy-scan.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-websub/actions/workflows/trivy-scan.yml)  
  [![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerina-websub.svg)](https://github.com/ballerina-platform/module-ballerina-websub/commits/master)
  [![Github issues](https://img.shields.io/github/issues/ballerina-platform/ballerina-standard-library/module/websub.svg?label=Open%20Issues)](https://github.com/ballerina-platform/ballerina-standard-library/labels/module%2Fwebsub)
  [![codecov](https://codecov.io/gh/ballerina-platform/module-ballerina-websub/branch/master/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerina-websub)

This package provides APIs for a WebSub Subscriber Service.

[**WebSub**](https://www.w3.org/TR/websub/) is a common mechanism for communication between publishers of any kind of Web content and their subscribers, based on HTTP webhooks. Subscription requests are relayed through hubs, which validate and verify the request. Hubs then distribute new and updated content to subscribers when it becomes available. WebSub was previously known as PubSubHubbub.

[**WebSub Subscriber**](https://www.w3.org/TR/websub/#subscriber) is an implementation that discovers the `hub` and `topic URL` of a given `resource URL`, subscribes to updates at the hub, and accepts content distribution requests from the `hub`.

For more information, go to the [`websub` module](https://ballerina.io/learn/api-docs/ballerina/websub/index.html).

For example demonstrations of the usage, go to [Ballerina By Examples](https://ballerina.io/learn/by-example/).
## Issues and Projects

Issues and Projects tabs are disabled for this repository as this is part of the Ballerina Standard Library. To report bugs, request new features, start new discussions, view project boards, etc., go to the [Ballerina Standard Library parent repository](https://github.com/ballerina-platform/ballerina-standard-library).

This repository only contains the source code for the package.

## Building from the Source

### Setting Up the Prerequisites

* Download and install Java SE Development Kit (JDK) version 11 (from one of the following locations).

   * [Oracle](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
   
   * [OpenJDK](https://adoptopenjdk.net/)
   
        > **Note:** Set the JAVA_HOME environment variable to the path name of the directory into which you installed JDK.
     
### Building the Source

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

## Contributing to Ballerina

As an open source project, Ballerina welcomes contributions from the community.

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of Conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful Links

* Chat live with us via our [Slack channel](https://ballerina.io/community/slack/).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
* View the [Ballerina performance test results](performance/benchmarks/summary.md).

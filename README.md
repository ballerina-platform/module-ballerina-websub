Ballerina WebSub Library
===================

  [![Build](https://github.com/ballerina-platform/module-ballerina-websub/actions/workflows/build-timestamped-master.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-websub/actions/workflows/build-timestamped-master.yml)
  [![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerina-websub.svg)](https://github.com/ballerina-platform/module-ballerina-websub/commits/master)
  [![Github issues](https://img.shields.io/github/issues/ballerina-platform/ballerina-standard-library/module/websub.svg?label=Open%20Issues)](https://github.com/ballerina-platform/ballerina-standard-library/labels/module%2Fwebsub)
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
  [![codecov](https://codecov.io/gh/ballerina-platform/module-ballerina-websub/branch/master/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerina-websub)

The WebSub package is one of the standard library packages of the <a target="_blank" href="https://ballerina.io/">Ballerina</a> language.

This package provides the capability to easily implement a WebSub compliant **Subscriber Service**.

 For more information, go to [The WebSub Module](https://ballerina.io/learn/api-docs/ballerina/websub/index.html).
## Issues and Projects

Issues and Projects tabs are disabled for this repository as this is part of the Ballerina Standard Library. To report bugs, request new features, start new discussions, view project boards, etc. please visit Ballerina Standard Library [parent repository](https://github.com/ballerina-platform/ballerina-standard-library).

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

* Discuss about code changes of the Ballerina project in [ballerina-dev@googlegroups.com](mailto:ballerina-dev@googlegroups.com).
* Chat live with us via our [Slack channel](https://ballerina.io/community/slack/).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
* View the [Ballerina performance test results](performance/benchmarks/summary.md).

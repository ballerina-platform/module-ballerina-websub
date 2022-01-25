# Changelog
This file contains all the notable changes done to the Ballerina WebSub package through the releases.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- [WebSub/WebSubHub should support `readonly` parameters for remote methods](https://github.com/ballerina-platform/ballerina-standard-library/issues/2604)

## [2.1.0] - 2021-12-14

### Fixed
- [Notify user of Subscription denial from the `hub` when `onSubscriptionDenied` is not implemented](https://github.com/ballerina-platform/ballerina-standard-library/issues/2448)  

## [2.0.1] - 2021-11-19

### Changed
- [Generated unique-service-path should not be changed after compilation](https://github.com/ballerina-platform/ballerina-standard-library/issues/1813)
- [Mark WebSub Service type as distinct](https://github.com/ballerina-platform/ballerina-standard-library/issues/2398)

## [2.0.0] - 2021-10-09

### Fixed
- [WebSub subscriber will not unsubscribe from the `hub` when terminated](https://github.com/ballerina-platform/ballerina-standard-library/issues/1843)

## [0.2.0.beta.2]  - 2021-07-06

### Fixed
- [Log error when return from the remote method leads to an error](https://github.com/ballerina-platform/ballerina-standard-library/issues/1450)
- [WebSubHub Compiler Plugin does not allow additional methods inside service declaration](https://github.com/ballerina-platform/ballerina-standard-library/issues/1417)
- [Util method to retrieve HTTP Headers from `onEventNotification` payload](https://github.com/ballerina-platform/ballerina-standard-library/issues/1484)
- [Return only module specific errors from public APIs](https://github.com/ballerina-platform/ballerina-standard-library/issues/1487)

## [1.2.0-beta.1] - 2021-05-06

### Changed
- [Make HTTP Service Class Isolated in WebSub](https://github.com/ballerina-platform/ballerina-standard-library/issues/1389)

### Fixed
- [Fix the listener initialization with inline configs compiler plugin error](https://github.com/ballerina-platform/ballerina-standard-library/issues/1304)

## [1.2.0-alpha8] - 2021-04-22
### Added
- [Add compiler plugin to validate wesub:SubscriberService.](https://github.com/ballerina-platform/ballerina-standard-library/issues/1099)

## [1.2.0-alpha7] - 2021-04-02
### Added
- [Add reusable WebSub common responses.](https://github.com/ballerina-platform/ballerina-standard-library/issues/1183)

### Fixed
- [Fix issue in url-encoded content retrieval.](https://github.com/ballerina-platform/ballerina-standard-library/issues/1106)
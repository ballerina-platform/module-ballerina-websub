//// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
////
//// WSO2 Inc. licenses this file to you under the Apache License,
//// Version 2.0 (the "License"); you may not use this file except
//// in compliance with the License.
//// You may obtain a copy of the License at
////
//// http://www.apache.org/licenses/LICENSE-2.0
////
//// Unless required by applicable law or agreed to in writing,
//// software distributed under the License is distributed on an
//// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//// KIND, either express or implied.  See the License for the
//// specific language governing permissions and limitations
//// under the License.
//
//import ballerina/auth;
//import ballerina/http;
//import ballerina/log;
//import ballerina/runtime;
//
//const string WEBSUB_PERSISTENCE_TOPIC_ONE = "http://one.persistence.topic.com";
//const string WEBSUB_PERSISTENCE_TOPIC_TWO = "http://two.persistence.topic.com";
//const string WEBSUB_ADV_TOPIC_ONE = "http://one.adv.websub.topic.com";
//
//auth:InboundBasicAuthProvider basicAuthProvider = new;
//http:BasicAuthHandler basicAuthHandler = new(basicAuthProvider);
//
//Hub advWebSubHub = startAdvHubAndRegisterTopic();
//
//listener http:Listener advPublisherServiceEP = new http:Listener(23082);
//
//http:BasicAuthHandler outboundBasicAuthHandler = new(new auth:OutboundBasicAuthProvider({
//                                                         username: "anne",
//                                                         password: "abc"
//                                                     }));
//
//PublisherClient advWebsubHubClientEP = new (advWebSubHub.publishUrl, {
//    auth: {
//        authHandler: outboundBasicAuthHandler
//    },
//    secureSocket: {
//        trustStore: {
//            path: "src/websub/tests/resources/security/ballerinaTruststore.p12",
//            password: "ballerina"
//        }
//    }
//});
//
//http:BasicAuthHandler authnFailingHandler = new(new auth:OutboundBasicAuthProvider({
//                                                         username: "anne",
//                                                         password: "cba"
//                                               }));
//
//PublisherClient authnFailingClient = new (advWebSubHub.publishUrl, {
//    auth: {
//        authHandler: authnFailingHandler
//    },
//    secureSocket: {
//        trustStore: {
//            path: "src/websub/tests/resources/security/ballerinaTruststore.p12",
//            password: "ballerina"
//        }
//    }
//});
//
//http:BasicAuthHandler authzFailingHandler = new(new auth:OutboundBasicAuthProvider({
//                                                         username: "peter",
//                                                         password: "pqr"
//                                               }));
//
//PublisherClient authzFailingClient = new (advWebSubHub.publishUrl, {
//    auth: {
//        authHandler: authzFailingHandler
//    },
//    secureSocket: {
//        trustStore: {
//            path: "src/websub/tests/resources/security/ballerinaTruststore.p12",
//            password: "ballerina"
//        }
//    }
//});
//
//service advPublisher on advPublisherServiceEP {
//    @http:ResourceConfig {
//        methods: ["GET", "HEAD"]
//    }
//    resource function discover(http:Caller caller, http:Request req) {
//        http:Response response = new;
//        // Add a link header indicating the hub and topic
//        addWebSubLinkHeader(response, [advWebSubHub.subscriptionUrl], WEBSUB_PERSISTENCE_TOPIC_ONE);
//        var err = caller->accepted(response);
//        if (err is error) {
//            log:printError("Error responding on discovery", err);
//        }
//    }
//
//    @http:ResourceConfig {
//        methods: ["POST"],
//        path: "/notify/{subscriber}"
//    }
//    resource function notify(http:Caller caller, http:Request req, string subscriber) {
//        var payload = req.getJsonPayload();
//        if (payload is error) {
//            panic <error> payload;
//        }
//
//        checkAdvSubscriberAvailability(WEBSUB_PERSISTENCE_TOPIC_ONE, "http://localhost:" + subscriber + "/websub");
//        var err = advWebSubHub.publishUpdate(WEBSUB_PERSISTENCE_TOPIC_ONE, <@untainted> <json> payload);
//        if (err is error) {
//            log:printError("Error publishing update directly", err);
//        }
//
//        http:Response response = new;
//        err = caller->accepted(response);
//        if (err is error) {
//            log:printError("Error responding on notify request", err);
//        }
//    }
//}
//
//service advPublisherTwo on advPublisherServiceEP {
//    @http:ResourceConfig {
//        methods: ["GET", "HEAD"]
//    }
//    resource function discover(http:Caller caller, http:Request req) {
//        http:Response response = new;
//        // Add a link header indicating the hub and topic
//        addWebSubLinkHeader(response, [advWebSubHub.subscriptionUrl], WEBSUB_PERSISTENCE_TOPIC_TWO);
//        var err = caller->accepted(response);
//        if (err is error) {
//            log:printError("Error responding on discovery", err);
//        }
//    }
//
//    @http:ResourceConfig {
//        methods: ["POST"]
//    }
//    resource function notify(http:Caller caller, http:Request req) {
//        var payload = req.getJsonPayload();
//        if (payload is error) {
//            panic <error> payload;
//        }
//
//        checkAdvSubscriberAvailability(WEBSUB_PERSISTENCE_TOPIC_TWO, "http://localhost:23383/websubTwo");
//        var err = advWebSubHub.publishUpdate(WEBSUB_PERSISTENCE_TOPIC_TWO, <@untainted> <json> payload);
//        if (err is error) {
//            log:printError("Error publishing update directly", err);
//        }
//
//        http:Response response = new;
//        err = caller->accepted(response);
//        if (err is error) {
//            log:printError("Error responding on notify request", err);
//        }
//    }
//}
//
//service publisherThree on advPublisherServiceEP {
//    @http:ResourceConfig {
//        methods: ["GET", "HEAD"]
//    }
//    resource function discover(http:Caller caller, http:Request req) {
//        http:Response response = new;
//        // Add a link header indicating the hub and topic
//        addWebSubLinkHeader(response, [advWebSubHub.subscriptionUrl], WEBSUB_ADV_TOPIC_ONE);
//        var err = caller->accepted(response);
//        if (err is error) {
//            log:printError("Error responding on discovery", err);
//        }
//    }
//
//    @http:ResourceConfig {
//        methods: ["POST"]
//    }
//    resource function notify(http:Caller caller, http:Request req) {
//        var payload = req.getJsonPayload();
//        if (payload is error) {
//            panic <error> payload;
//        }
//        checkAdvSubscriberAvailability(WEBSUB_ADV_TOPIC_ONE, "http://localhost:23484/websubFour");
//
//        string publishErrorMessagesConcatenated = "";
//
//        var err = advWebsubHubClientEP->publishUpdate(WEBSUB_ADV_TOPIC_ONE, <@untainted> <json> payload);
//        if (err is error) {
//            publishErrorMessagesConcatenated += err.detail()?.message ?: "";
//            log:printError("Error publishing update remotely", err);
//        }
//
//        err = authnFailingClient->publishUpdate(WEBSUB_ADV_TOPIC_ONE, <@untainted> <json> payload);
//        if (err is error) {
//            publishErrorMessagesConcatenated += err.detail()?.message ?: "";
//            log:printError("Error publishing update remotely", err);
//        }
//
//        err = authzFailingClient->publishUpdate(WEBSUB_ADV_TOPIC_ONE, <@untainted> <json> payload);
//        if (err is error) {
//            publishErrorMessagesConcatenated += err.detail()?.message ?: "";
//            log:printError("Error publishing update remotely", err);
//        }
//
//        err = caller->accepted(<@untainted> publishErrorMessagesConcatenated);
//        if (err is error) {
//            log:printError("Error responding on notify request", err);
//        }
//    }
//}
//
//service helperService on advPublisherServiceEP {
//    @http:ResourceConfig {
//        methods: ["POST"]
//    }
//    resource function restartHub(http:Caller caller, http:Request req) {
//        checkpanic advWebSubHub.stop();
//        advWebSubHub = startAdvHubAndRegisterTopic();
//        checkpanic caller->accepted();
//    }
//}
//
//function startAdvHubAndRegisterTopic() returns Hub {
//    Hub internalHub = startWebSubHub();
//    var err = internalHub.registerTopic(WEBSUB_PERSISTENCE_TOPIC_ONE);
//    if (err is error) {
//        log:printError("Error registering topic", err);
//    }
//    err = internalHub.registerTopic(WEBSUB_PERSISTENCE_TOPIC_TWO);
//    if (err is error) {
//        log:printError("Error registering topic", err);
//    }
//    err = internalHub.registerTopic(WEBSUB_ADV_TOPIC_ONE);
//    if (err is error) {
//        log:printError("Error registering topic", err);
//    }
//    return internalHub;
//}
//
//function startWebSubHub() returns Hub {
//    var result = startHub(new http:Listener(23191, config =  {
//                auth: {
//                    authHandlers: [basicAuthHandler]
//                },
//                secureSocket: {
//                    keyStore: {
//                        path: "src/websub/tests/resources/security/ballerinaKeystore.p12",
//                        password: "ballerina"
//                    },
//                    trustStore: {
//                        path: "src/websub/tests/resources/security/ballerinaTruststore.p12",
//                        password: "ballerina"
//                    }
//                }
//            }), "/websub", "/hub",
//                serviceAuth = {enabled:true},
//                subscriptionResourceAuth = {enabled:true, scopes:["subscribe"]},
//                publisherResourceAuth = {enabled:true, scopes:["publish"]},
//                hubConfiguration = { remotePublish : { enabled : true }}
//    );
//    if (result is Hub) {
//        return result;
//    } else if (result is HubStartedUpError) {
//        return result.startedUpHub;
//    } else {
//        panic result;
//    }
//}
//
//function checkAdvSubscriberAvailability(string topic, string callback) {
//    int count = 0;
//    boolean subscriberAvailable = false;
//    while (!subscriberAvailable && count < 60) {
//        SubscriberDetails[] topicDetails = advWebSubHub.getSubscribers(topic);
//        if (isAdvSubscriberAvailable(topicDetails, callback)) {
//            return;
//        }
//        runtime:sleep(1000);
//        count += 1;
//    }
//}
//
//function isAdvSubscriberAvailable(SubscriberDetails[] topicDetails, string callback) returns boolean {
//    foreach var detail in topicDetails {
//        if (detail.callback == callback) {
//            return true;
//        }
//    }
//    return false;
//}

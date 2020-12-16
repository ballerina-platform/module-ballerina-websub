
// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/http;
import ballerina/log;
import ballerina/websub;
import ballerina/test;

websub:Hub WebSubHub = startHubAndRegisterTopic();
listener http:Listener httpListener = new (24080);

service /publisherService on httpListener {
    resource function get discoveryWithAcceptAndAcceptLanguage(http:Caller caller, http:Request req) {
        http:Response response = new;
        string media_type = req.getHeader("Accept");
        string language_type = req.getHeader("Accept-Language");
        if (media_type == "application/json" && language_type == "de-DE") {
            websub:addWebSubLinkHeader(response, [WebSubHub.subscriptionUrl], WEBSUB_TOPIC_ONE);
            response.statusCode = 202;
            var result = caller->respond(response);
            if (result is error) {
                log:printError("Error responding ", err = result);
            }
        } else {
            response.statusCode = 406;
            var res = caller->respond(response);
        }
    }

    //Publisher need accept header only and agree with application/json as Accept header
    resource function get discoveryAcceptHeaderOnly(http:Caller caller, http:Request req) {
        http:Response response = new;
        string media_type = req.getHeader("Accept");
        if (media_type == "application/json") {
            websub:addWebSubLinkHeader(response, [WebSubHub.subscriptionUrl], WEBSUB_TOPIC_ONE);
            response.statusCode = 202;
            var result = caller->respond(response);
            if (result is error) {
                log:printError("Error responding ", err = result);
            }
        } else {
            response.statusCode = 406;
            var res = caller->respond(response);
        }
    }

    //Publisher need accept language header only and agree with de-DE as Accept-Language header
    resource function get discoveryAcceptLanguageOnly(http:Caller caller, http:Request req) {
        http:Response response = new;
        string language_type = req.getHeader("Accept-Language");
        if (language_type == "de-DE") {
            websub:addWebSubLinkHeader(response, [WebSubHub.subscriptionUrl], WEBSUB_TOPIC_ONE);
            response.statusCode = 202;
            var result = caller->respond(response);
            if (result is error) {
                log:printError("Error responding ", err = result);
            }
        } else {
            response.statusCode = 406;
            var res = caller->respond(response);
        }
    }
}

@test:Config{}
//SubscriberServiceConfig's accept and acceptLanguage field values match with publisher's acceptable Accept and Accept-Language header values.
function testMatchAcceptAndAcceptLanguage() {
    http:Client clientEndpoint = new("http://localhost:24080");
    http:Request req = new;
    req.setHeader("Accept", "application/json");
    req.setHeader("Accept-Language", "de-DE");
    var result = clientEndpoint->get("/publisherService/discoveryWithAcceptAndAcceptLanguage", req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode, http:STATUS_ACCEPTED, msg = "unsupported content type or media type.");
}

@test:Config{
     dependsOn:["testMatchAcceptAndAcceptLanguage"]
}
//SubscriberServiceConfig's accept and acceptLanguage field values mismatch with publisher's acceptable Accept and Accept-Language header values.
function testMisMatchAcceptAndAcceptLanguage() {
    http:Client clientEndpoint = new("http://localhost:24080");
    http:Request req = new;
    req.setHeader("Accept", "text/html");
    req.setHeader("Accept-Language", "de-US");
    var result = clientEndpoint->get("/publisherService/discoveryWithAcceptAndAcceptLanguage", req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode, http:STATUS_NOT_ACCEPTABLE, msg = "content type is supported by publisher.");
}

@test:Config{
     dependsOn:["testMisMatchAcceptAndAcceptLanguage"]
}
//SubscriberServiceConfig's accept field value match with publisher's acceptable Accept header values.
function testMatchAcceptOnly() {
    http:Client clientEndpoint = new("http://localhost:24080");
    http:Request req = new;
    req.setHeader("Accept", "application/json");
    var result = clientEndpoint->get("/publisherService/discoveryAcceptHeaderOnly", req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode, http:STATUS_ACCEPTED, msg = "accept header mismatch.");
}

@test:Config{
     dependsOn:["testMatchAcceptOnly"]
}
//SubscriberServiceConfig accept field value mismatch with publisher's acceptable Accept header value.
function testMisMatchAcceptOnly() {
    http:Client clientEndpoint = new("http://localhost:24080");
    http:Request req = new;
    req.setHeader("Accept", "text/html");
    var result = clientEndpoint->get("/publisherService/discoveryAcceptHeaderOnly", req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode, http:STATUS_NOT_ACCEPTABLE, msg = "accept header match.");
}

@test:Config{
     dependsOn:["testMisMatchAcceptOnly"]
}
//SubscriberServiceConfig acceptLanguage field value match with publisher's acceptable Accept-Language header value.
function testMatchAcceptLanguageOnly() {
    http:Client clientEndpoint = new("http://localhost:24080");
    http:Request req = new;
    req.setHeader("Accept-Language", "de-DE");
    var result = clientEndpoint->get("/publisherService/discoveryAcceptLanguageOnly", req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode, http:STATUS_ACCEPTED, msg = "accept language header mismatch.");
}

@test:Config{
     dependsOn:["testMatchAcceptLanguageOnly"]
}
//SubscriberServiceConfig acceptLanguage field value mismatch with publisher's acceptable Accept-Language header value.
function testMisMatchAcceptLanguageOnly() {
    http:Client clientEndpoint = new("http://localhost:24080");
    http:Request req = new;
    req.setHeader("Accept-Language", "de-US");
    var result = clientEndpoint->get("/publisherService/discoveryAcceptLanguageOnly", req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode, http:STATUS_NOT_ACCEPTABLE, msg = "accept language header match.");
}

@test:Config{
    dependsOn:["testMisMatchAcceptLanguageOnly"]
}
//SubscriberServiceConfig doesn't contain accept and acceptLanguage fields.Publisher need Accept and Accept-Language headers from discovery request.
function testMissingHeaders() {
    http:Client clientEndpoint = new("http://localhost:24080");
    http:Request req = new;
    var result = clientEndpoint->get("/publisherService/discoveryWithAcceptAndAcceptLanguage", req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode, http:STATUS_INTERNAL_SERVER_ERROR, msg = "Both Accept and Accept-Language headers available");
}


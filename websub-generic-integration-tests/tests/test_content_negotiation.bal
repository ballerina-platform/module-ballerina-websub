import ballerina/http;
import ballerina/log;
import ballerina/websub;
import ballerina/test;

websub:Hub WebSubHub = startHubAndRegisterTopic();
listener http:Listener httpListener = new (24080);

service publisherService on httpListener {

    @http:ResourceConfig {
           methods: ["GET", "HEAD"]
    }
    resource function discoveryWithAcceptAndAcceptLanguage(http:Caller caller, http:Request req) {
        http:Response response = new;
        string media_type=req.getHeader("Accept");
        string language_type=req.getHeader("Accept-Language");
        if(media_type =="application/json" && language_type == "de-DE"){
            websub:addWebSubLinkHeader(response, [WebSubHub.subscriptionUrl],WEBSUB_TOPIC_ONE);
            response.statusCode = 202;
            var result = caller->respond(response);
            if (result is error) {
                log:printError("Error responding ", result);
            }

        }else{
            response.statusCode=406;
            var res=caller->respond(response);
        }

    }

    resource function discoveryAcceptHeaderOnly(http:Caller caller, http:Request req) {
        http:Response response = new;
        string media_type=req.getHeader("Accept");
        if(media_type =="application/json"){
            websub:addWebSubLinkHeader(response, [WebSubHub.subscriptionUrl],WEBSUB_TOPIC_ONE);
            response.statusCode = 202;
            var result = caller->respond(response);
            if (result is error) {
                log:printError("Error responding ", result);
            }

        } else {
            response.statusCode=406;
            var res=caller->respond(response);
        }
    }

    //SubscriberServiceConfig consists of acceptLanguage field only.Publisher also need accept language header only
    //and agree with de-DE as Accept-Language header

    resource function discoveryAcceptLanguageOnly(http:Caller caller, http:Request req) {
        http:Response response = new;
        string language_type=req.getHeader("Accept-Language");
        if(language_type == "de-DE") {
            websub:addWebSubLinkHeader(response, [WebSubHub.subscriptionUrl],WEBSUB_TOPIC_ONE);
            response.statusCode = 202;
            var result = caller->respond(response);
            if(result is error) {
                log:printError("Error responding ", result);
            }
        } else {
            response.statusCode=406;
            var res=caller->respond(response);
        }
    }
}


@test:Config{}

//subscriberServiceConfig accept and acceptLanguage field values match with publisher's acceptable Accept and Accept-Language header values.

function testMatchAcceptAndAcceptLanguage(){
    http:Client clientEndpoint=new("http://localhost:24080");
    http:Request req= new;
    req.setHeader("Accept","application/json");
    req.setHeader("Accept-Language","de-DE");
    var result=clientEndpoint->get("/publisherService/discoveryWithAcceptAndAcceptLanguage",req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode,http:STATUS_ACCEPTED,msg="unsupported content type or media type.");
}

@test:Config{
     dependsOn:["testMatchAcceptAndAcceptLanguage"]
}

//subscriberServiceConfig accept and acceptLanguage field values mismatch with publisher's acceptable Accept and Accept-Language header values.

function testMisMatchAcceptAndAcceptLanguage(){
    http:Client clientEndpoint=new("http://localhost:24080");
    http:Request req= new;
    req.setHeader("Accept","text/html");
    req.setHeader("Accept-Language","de-US");
    var result=clientEndpoint->get("/publisherService/discoveryWithAcceptAndAcceptLanguage",req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode,http:STATUS_NOT_ACCEPTABLE ,msg="content type is supported by publisher.");
}


@test:Config{
     dependsOn:["testMisMatchAcceptAndAcceptLanguage"]
}

//subscriberServiceConfig accept field value match with publisher's acceptable accept header value.

function testMatchAcceptOnly(){
    http:Client clientEndpoint=new("http://localhost:24080");
    http:Request req= new;
    req.setHeader("Accept","application/json");
    var result=clientEndpoint->get("/publisherService/discoveryAcceptHeaderOnly",req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode,http:STATUS_ACCEPTED,msg="accept header mismatch.");
}

@test:Config{
     dependsOn:["testMatchAcceptOnly"]
}

//subscriberServiceConfig accept field value mismatch with publisher's acceptable accept header value.

function testMisMatchAcceptOnly(){
    http:Client clientEndpoint=new("http://localhost:24080");
    http:Request req= new;
    req.setHeader("Accept","text/html");
    var result=clientEndpoint->get("/publisherService/discoveryAcceptHeaderOnly",req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode,http:STATUS_NOT_ACCEPTABLE ,msg="accept header match.");
}

@test:Config{
     dependsOn:["testMisMatchAcceptOnly"]
}

//subscriberServiceConfig acceptLanguage field value match with publisher's acceptable Accept-Language header value.

function testMatchAcceptLanguageOnly(){
    http:Client clientEndpoint=new("http://localhost:24080");
    http:Request req= new;
    req.setHeader("Accept-Language","de-DE");
    var result=clientEndpoint->get("/publisherService/discoveryAcceptLanguageOnly",req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode,http:STATUS_ACCEPTED,msg="accept language header mismatch.");
}

@test:Config{
     dependsOn:["testMatchAcceptLanguageOnly"]
}

//subscriberServiceConfig acceptLanguage field value mismatch with publisher's acceptable Accept-Language header value.

function testMisMatchAcceptLanguageOnly(){
    http:Client clientEndpoint=new("http://localhost:24080");
    http:Request req= new;
    req.setHeader("Accept-Language","de-US");
    var result=clientEndpoint->get("/publisherService/discoveryAcceptLanguageOnly",req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode,http:STATUS_NOT_ACCEPTABLE,msg="accept language header match.");

}

@test:Config{
    dependsOn:["testMisMatchAcceptLanguageOnly"]
}

//SubscriberServiceConfig provide application/json as the accept field value and it agree with publisher's acceptable Accept header value.
//But publisher require both Accept and Accept-Language headers.

function testMissingOneHeader(){
    http:Client clientEndpoint=new("http://localhost:24080");
    http:Request req=new;
    req.setHeader("Accept","application/json");
    var result=clientEndpoint->get("/publisherService/discoveryWithAcceptAndAcceptLanguage",req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode,http:STATUS_INTERNAL_SERVER_ERROR,msg="Both Accept and Accept-Language headers available");
}





import ballerina/http;
import ballerina/log;
import ballerina/websub;
import ballerina/test;


listener http:Listener httpListener = new (24080);
final string TOPIC = "http://websubpubtopic.com";
websub:Hub webSubHub = startHubAndRegisterTopic();


service publisher on httpListener {

    @http:ResourceConfig {
           methods: ["GET", "HEAD"]
    }

    //subscriberServiceConfig consists of both accept and acceptLanguage headers.Publisher agree with text/html as accept header
    //and de-DE as accept language header.All other accepts headers and accept languages discarded by publisher
    resource function DiscoveryWithAcceptAndAcceptLanguage(http:Caller caller, http:Request req) {

        http:Response response = new;
        string content_type=req.getHeader("Accept");
        string media_type=req.getHeader("Accept-Language");
        if(content_type =="text/html" && media_type == "de-DE"){
                websub:addWebSubLinkHeader(response, [webSubHub.subscriptionUrl],TOPIC);
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

     //subscriberServiceConfig consists of both accept header only.Publisher agree with text/html as accept header
     //and all other accepts headers and accept languages discarded by publisher

     resource function DiscoveryAcceptHeaderOnly(http:Caller caller, http:Request req) {

            http:Response response = new;
            string content_type=req.getHeader("Accept");
            if(content_type =="application/json"){
                    websub:addWebSubLinkHeader(response, [webSubHub.subscriptionUrl],TOPIC);
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

          //subscriberServiceConfig consists of accept language header only.Publisher agree with de-DE as accept language header
          //and all other accepts headers and accept languages discarded by publisher

         resource function discoveryAcceptLanguageOnly(http:Caller caller, http:Request req) {

                    http:Response response = new;
                    string content_type=req.getHeader("Accept-Language");
                    if(content_type =="de-DE"){
                            websub:addWebSubLinkHeader(response, [webSubHub.subscriptionUrl],TOPIC);
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

         //subscriberServiceConfig hasn't accept or accept language headers .Publisher agree with text/html as  accept header
         //and de-DE as accept language header.
         resource function DiscoveryMissingAcceptHeader(http:Caller caller, http:Request req) {

                    http:Response response = new;
                    string content_type=req.getHeader("Accept-Language");
                    if(content_type =="de-DE"){
                            websub:addWebSubLinkHeader(response, [webSubHub.subscriptionUrl],TOPIC);
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


}

function startHubAndRegisterTopic() returns websub:Hub {
    var hubStartUpResult = websub:startHub(new http:Listener(24081),"/websub", "/hub");
    websub:Hub? hubVar = ();
    if hubStartUpResult is websub:HubStartupError {
        panic hubStartUpResult;
    } else {
        hubVar = hubStartUpResult is websub:HubStartedUpError ? hubStartUpResult["startedUpHub"] : hubStartUpResult;
    }
    websub:Hub internalHub = <websub:Hub>hubVar;
    var result = internalHub.registerTopic(TOPIC);
    if (result is error) {
        log:printError("Error registering topic", result);
    }
    return internalHub;
}


@test:Config{}

//subscriberServiceConfig accept and acceptLanguage values match with publisher's acceptable forms.
function testDiscoveryMatchAcceptAndAcceptLanguage(){

    http:Client clientEndpoint=new("http://localhost:24080");
    http:Request req= new;
    req.setHeader("Accept","text/html");
    req.setHeader("Accept-Language","de-DE");
    var result=clientEndpoint->get("/publisher/DiscoveryWithAcceptAndAcceptLanguage",req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode,http:STATUS_ACCEPTED,msg="unsupported content type or media type.");

}

@test:Config{
     dependsOn:["testDiscoveryMatchAcceptAndAcceptLanguage"]
}
//subscriberServiceConfig accept and acceptLanguage values mismatch with publisher's acceptable forms.
function testDiscoveryMisMatchAcceptAndAcceptLanguage(){

    http:Client clientEndpoint=new("http://localhost:24080");
    http:Request req= new;
    req.setHeader("Accept","application/json");
    req.setHeader("Accept-Language","de-US");
    var result=clientEndpoint->get("/publisher/DiscoveryWithAcceptAndAcceptLanguage",req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode,http:STATUS_NOT_ACCEPTABLE ,msg="content type is supported by publisher.");

}



@test:Config{
     dependsOn:["testDiscoveryMisMatchAcceptAndAcceptLanguage"]
}
//subscriberServiceConfig accept value match with publisher's accept header.
function testDiscoveryMatchAcceptOnly(){

    http:Client clientEndpoint=new("http://localhost:24080");
    http:Request req= new;
    req.setHeader("Accept","application/json");
    var result=clientEndpoint->get("/publisher/DiscoveryAcceptHeaderOnly",req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode,http:STATUS_ACCEPTED,msg="accept header mismatch.");

}

@test:Config{
     dependsOn:["testDiscoveryMatchAcceptOnly"]
}
//subscriberServiceConfig accept value mismatch with publisher's accept header.
function testDiscoveryMisMatchAcceptOnly(){

    http:Client clientEndpoint=new("http://localhost:24080");
    http:Request req= new;
    req.setHeader("Accept","text/html");
    var result=clientEndpoint->get("/publisher/DiscoveryAcceptHeaderOnly",req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode,http:STATUS_NOT_ACCEPTABLE ,msg="accept header match.");

}


@test:Config{
     dependsOn:["testDiscoveryMisMatchAcceptOnly"]
}
//subscriberServiceConfig acceptLanguage value match with publisher's accept language header.
function testDiscoveryMatchAcceptLanguageOnly(){

    http:Client clientEndpoint=new("http://localhost:24080");
    http:Request req= new;
    req.setHeader("Accept-Language","de-DE");
    var result=clientEndpoint->get("/publisher/discoveryAcceptLanguageOnly",req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode,http:STATUS_ACCEPTED,msg="accept language header mismatch.");

}

@test:Config{
     dependsOn:["testDiscoveryMatchAcceptLanguageOnly"]
}

//subscriberServiceConfig acceptLanguage value mismatch with publisher's accept language header.
function testDiscoveryMisMatchAcceptLanguageOnly(){

    http:Client clientEndpoint=new("http://localhost:24080");
    http:Request req= new;
    req.setHeader("Accept-Language","de-US");
    var result=clientEndpoint->get("/publisher/discoveryAcceptLanguageOnly",req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode,http:STATUS_NOT_ACCEPTABLE,msg="accept language header match.");

}

@test:Config{
     dependsOn:["testDiscoveryMatchAcceptLanguageOnly"]
}
//subscriberServiceConfig hasn't accept or acceptLanguage fields.
function testDiscoveryMissingHeaders(){

    http:Client clientEndpoint=new("http://localhost:24080");
    http:Request req= new;
    var result=clientEndpoint->get("/publisher/DiscoveryMissingAcceptHeader",req);
    HttpResponseDetails responseDetails = fetchHttpResponse(result);
    test:assertEquals(responseDetails.statusCode,http:STATUS_INTERNAL_SERVER_ERROR,msg="Headers provided correctly.");

}




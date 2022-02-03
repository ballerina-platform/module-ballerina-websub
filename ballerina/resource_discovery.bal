// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/lang.'string as strings;
import ballerina/regex;

# Represents resource-discovery service which identify the `hub` and `topic` from `resource-URL`.
public client class DiscoveryService {
    private string resourceUrl;
    private http:Client discoveryClientEp;

    # Initiliazes the `websub:DiscoveryService` endpoint.
    # ```ballerina
    # websub:DiscoveryService discoveryServiceEp = check new ("https://sample.discovery.com");
    # ```
    # 
    # + resourceUrl - User-provided resource URL
    # + publisherClientConfig - Optional `http:ClientConfiguration` to be used in the underlying `http:Client`
    # + return - The `websub:DiscoveryService` or an `websub:Error` if the initialization failed
    public isolated function init(string discoveryUrl, http:ClientConfiguration? config) returns Error? {
        self.resourceUrl = discoveryUrl;
        self.discoveryClientEp = check retrieveHttpClient(self.resourceUrl, config);
    }

    # Discovers the URLs of the hub and topic defined by a resource URL.
    # ```ballerina
    # [string, string] discoveryDetails = check discoveryServiceEp->discoverResourceUrls(expectedMediaTypes, expectedLanguageTypes);
    # ```
    # 
    # + expectedMediaTypes - The expected media types for the subscriber client
    # + expectedLanguageTypes - The expected language types for the subscriber client
    # + return - A `(hub, topic)` as a `(string, string)` if successful or else an `websub:ResourceDiscoveryFailedError` if not
    remote isolated function discoverResourceUrls(string?|string[] expectedMediaTypes, string?|string[] expectedLanguageTypes) 
                                        returns [string, string]|ResourceDiscoveryFailedError {    
        map<string|string[]> headers = {};
        if expectedMediaTypes is string {
            headers[ACCEPT_HEADER] = expectedMediaTypes;
        }
    
        if expectedMediaTypes is string[] {
            string acceptMeadiaTypesString = expectedMediaTypes[0];
            foreach int expectedMediaTypeIndex in 1 ... (expectedMediaTypes.length() - 1) {
                acceptMeadiaTypesString = acceptMeadiaTypesString.concat(", ", expectedMediaTypes[expectedMediaTypeIndex]);
            }
            headers[ACCEPT_HEADER] = acceptMeadiaTypesString;
        }
    
        if expectedLanguageTypes is string {
            headers[ACCEPT_LANGUAGE_HEADER] = expectedLanguageTypes;
        }
    
        if expectedLanguageTypes is string[] {
            string acceptLanguageTypesString = expectedLanguageTypes[0];
            foreach int expectedLanguageTypeIndex in 1 ... (expectedLanguageTypes.length() - 1) {
                acceptLanguageTypesString = acceptLanguageTypesString.concat(", ", expectedLanguageTypes[expectedLanguageTypeIndex]);
            }
            headers[ACCEPT_LANGUAGE_HEADER] = acceptLanguageTypesString;
        }
        
        http:Response|error discoveryResponse = self.discoveryClientEp->get("", headers);
        if discoveryResponse is http:Response {
            var topicAndHubs = extractTopicAndHubUrls(discoveryResponse);
            if topicAndHubs is [string, string[]] {
                string topic = "";
                string[] hubs = [];
                [topic, hubs] = topicAndHubs;
                return [hubs[0], topic]; // guaranteed by `extractTopicAndHubUrls` for hubs to have length > 0
            } else {
                return error ResourceDiscoveryFailedError(topicAndHubs.message());
            }
        } else {
            return error ResourceDiscoveryFailedError("Error occurred with WebSub discovery for Resource URL [" + self.resourceUrl + "]: " +
                            (<error>discoveryResponse).message());
        }                                       
    }
}

isolated function extractTopicAndHubUrls(http:Response response) returns [string, string[]]|error {
    string[] linkHeaders = [];
    if response.hasHeader("Link") {
        linkHeaders = check response.getHeaders("Link");
    }
    
    if response.statusCode == http:STATUS_NOT_ACCEPTABLE {
        return error ResourceDiscoveryFailedError("Content negotiation failed.Accept and/or Accept-Language headers mismatch");
    }

    if linkHeaders.length() == 0 {
        return error ResourceDiscoveryFailedError("Link header unavailable in discovery response");
    }

    int hubIndex = 0;
    string[] hubs = [];
    string topic = "";
    string[] linkHeaderConstituents = [];
    if linkHeaders.length() == 1 {
        linkHeaderConstituents = regex:split(linkHeaders[0], ",");
    } else {
        linkHeaderConstituents = linkHeaders;
    }

    foreach var link in linkHeaderConstituents {
        string[] linkConstituents = regex:split(link, ";");
        if linkConstituents[1] != "" {
            string url = linkConstituents[0].trim();
            url = regex:replaceAll(url, "<", "");
            url = regex:replaceAll(url, ">", "");
            if strings:includes(linkConstituents[1], "rel=\"hub\"") {
                hubs[hubIndex] = url;
                hubIndex += 1;
            } else if strings:includes(linkConstituents[1], "rel=\"self\"") {
                if topic != "" {
                    return error ResourceDiscoveryFailedError("Link Header contains > 1 self URLs");
                } else {
                    topic = url;
                }
            }
        }
    }

    if hubs.length() > 0 && topic != "" {
        return [topic, hubs];
    }
    return error ResourceDiscoveryFailedError("Hub and/or Topic URL(s) not identified in link header of discovery response");
}

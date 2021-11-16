// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/log;
import ballerina/jballerina.java;
import ballerina/lang.runtime;
import ballerina/time;

# Represents a Subscriber Service listener endpoint.
public class Listener {
    private http:Listener httpListener;
    private http:InferredListenerConfiguration listenerConfig;
    private int port;
    private decimal gracefulShutdownPeriod;

    # Initiliazes `websub:Listener` instance.
    # ```ballerina
    # listener websub:Listener websubListenerEp = check new (9090);
    # ```
    #
    # + listenTo - Port number or a `http:Listener` instance
    # + config - Custom `websub:ListenerConfiguration` to be provided to underlying HTTP Listener
    # + return - The `websub:Listener` or an `websub:Error` if the initialization failed
    public isolated function init(int|http:Listener listenTo, *ListenerConfiguration config) returns Error? {
        if listenTo is int {
            http:ListenerConfiguration httpListenerConfig = retrieveHttpListenerConfig(config);
            http:Listener|error httpListener = new(listenTo, httpListenerConfig);
            if httpListener is http:Listener {
                self.httpListener = httpListener;
            } else {
                return error Error("Listener initialization failed", httpListener);
            }
        } else {
            self.httpListener = listenTo;
        }
        self.listenerConfig = self.httpListener.getConfig();
        self.port = self.httpListener.getPort();
        self.gracefulShutdownPeriod = config.gracefulShutdownPeriod;
        return self.externInit();
    }

    isolated function externInit() returns Error? = @java:Method {
        'class: "io.ballerina.stdlib.websub.NativeWebSubListenerAdaptor"
    } external;

    # Attaches the provided `websub:SubscriberService` to the `websub:Listener`.
    # ```ballerina
    # check websubListenerEp.attach('service, "/subscriber");
    # ```
    # 
    # + service - The `websub:SubscriberService` object to attach
    # + name - The path of the Service to be hosted
    # + return - An `websub:Error`, if an error occurred during the service attaching process or else `()`
    public isolated function attach(SubscriberService 'service, string[]|string? name = ()) returns Error? {
        SubscriberServiceConfiguration? serviceConfig = retrieveSubscriberServiceAnnotations('service);
        if serviceConfig is SubscriberServiceConfiguration {
            error? result = self.executeAttach('service, serviceConfig, name);
            if (result is error) {
                return error Error("Error occurred while attaching the service: ", result);
            }
        } else {
            return error ListenerError("Could not find the required service-configurations");
        }
    }

    # Attaches the provided Service to the `websub:Listener` with custom `websub:SubscriberServiceConfiguration`.
    # ```ballerina
    # check websubListenerEp.attachWithConfig('service, {
    #    target: "http://0.0.0.0:9191/common/discovery",
    #    leaseSeconds: 36000
    # }, "/subscriber");
    # ```
    # 
    # + service - The `websub:SubscriberService` object to attach
    # + configuration - Custom `websub:SubscriberServiceConfiguration` which should be incorporated into the provided Service 
    # + name - The path of the Service to be hosted
    # + return - An `websub:Error`, if an error occurred during the service attaching process or else `()`
    public isolated function attachWithConfig(SubscriberService 'service, SubscriberServiceConfiguration configuration,
                                            string[]|string? name = ()) returns Error? {
        error? result = self.executeAttach('service, configuration, name);
        if (result is error) {
            return error Error("Error occurred while attaching the service", result);
        }
    }

    isolated function executeAttach(SubscriberService 'service, SubscriberServiceConfiguration serviceConfig,
                                    string[]|string? name = ()) returns error? {
        boolean generateServicePath = shouldUseGeneratedServicePath(serviceConfig, name);
        string[]|string? servicePath = generateServicePath ? check self.retrieveGeneratedServicePath('service): name;
        string completeSevicePath = check retrieveCompleteServicePath(servicePath);
        string callback = constructCallbackUrl(serviceConfig, self.port, self.listenerConfig,
                                                completeSevicePath, generateServicePath);
        HttpToWebsubAdaptor adaptor = new ('service);
        HttpService httpService = check new (adaptor, callback, serviceConfig?.secret);
        check self.httpListener.attach(httpService, completeSevicePath);
        self.externAttach(completeSevicePath, 'service, httpService, serviceConfig);
    }

    isolated function retrieveGeneratedServicePath(SubscriberService subscriberService) returns string|Error = @java:Method {
        'class: "io.ballerina.stdlib.websub.NativeWebSubListenerAdaptor"
    } external;

    isolated function externAttach(string servicePath, SubscriberService subscriberService,
                                    HttpService httpService, SubscriberServiceConfiguration config) = @java:Method {
        'class: "io.ballerina.stdlib.websub.NativeWebSubListenerAdaptor"
    } external;

    # Detaches the provided `websub:SubscriberService` from the `websub:Listener`.
    # ```ballerina
    # check websubListenerEp.detach('service);
    # ```
    # 
    # + service - The `websub:SubscriberService` object to be detached
    # + return - An `websub:Error`, if an error occurred during the service detaching process or else `()`
    public isolated function detach(SubscriberService 'service) returns Error? {
        HttpService? currentHttpService = self.detachHttpService('service);
        if currentHttpService is HttpService {
            error? result = self.httpListener.detach(currentHttpService);
            if (result is error) {
                return error Error("Error occurred while detaching the service", result);
            }
        }
    }

    isolated function detachHttpService(SubscriberService subscriberService) returns HttpService? = @java:Method {
        'class: "io.ballerina.stdlib.websub.NativeWebSubListenerAdaptor"
    } external;

    # Starts the registered service programmatically..
    # ```ballerina
    # check websubListenerEp.'start();
    # ```
    # 
    # + return - An `websub:Error`, if an error occurred during the listener starting process or else `()`
    public isolated function 'start() returns Error? {
        if self.listenerConfig.secureSocket is () {
            log:printWarn("HTTPS is recommended but using HTTP");
        }

        error? listenerError = self.httpListener.'start();
        if (listenerError is error) {
            return error Error("Error occurred while starting the service", listenerError);
        }

        HttpService[]? attachedServices = self.retrieveAttachedServices();
        if attachedServices is HttpService[] {
            foreach HttpService 'service in attachedServices {
                error? result = 'service.initiateSubscription();
                if result is error {
                    string errorDetails = result.message();
                    string errorMsg = string `Subscription initiation failed due to: ${errorDetails}`;
                    return error SubscriptionInitiationError(errorMsg);
                }
            }
        }
    }

    # Stops the service listener gracefully. Already-accepted requests will be served before connection closure.
    # ```ballerina
    # check websubListenerEp.gracefulStop();
    # ```
    # 
    # + return - An `websub:Error`, if an error occurred during the listener stopping process or else `()`
    public isolated function gracefulStop() returns Error? {
        HttpService[]? attachedServices = self.retrieveAttachedServices();
        if attachedServices is HttpService[] {
            foreach HttpService 'service in attachedServices {
                error? result = 'service.initiateUnsubscription();
                if result is error {
                    log:printWarn("Unsubscription initiation failed", result);
                }
            }
            self.waitForVerification(attachedServices);
        }

        error? result = self.httpListener.gracefulStop();
        if (result is error) {
            return error Error("Error occurred while stopping the service", result);
        }
    }

    isolated function retrieveAttachedServices() returns HttpService[]? = @java:Method {
        'class: "io.ballerina.stdlib.websub.NativeWebSubListenerAdaptor"
    } external;

    isolated function waitForVerification(HttpService[] availableServices) {
        time:Utc timeout = time:utcAddSeconds(time:utcNow(), self.gracefulShutdownPeriod);
        // wait until verification for all the subscribers are completed or verification time-out expires
        while !self.isVerificationCompleted(availableServices) && time:utcDiffSeconds(timeout, time:utcNow()) > 0D {
            runtime:sleep(2);
        }
    }

    isolated function isVerificationCompleted(HttpService[] availableServices) returns boolean {
        return availableServices
                .reduce(isolated function (boolean v1, HttpService s1) returns boolean => v1 && s1.isUnsubscriptionVerified(), true);
    }

    # Stops the service listener immediately.
    # ```ballerina
    # check websubListenerEp.immediateStop();
    # ```
    # 
    # + return - An `websub:Error`, if an error occurred during the listener stopping process or else `()`
    public isolated function immediateStop() returns Error? {
        error? result = self.httpListener.immediateStop();
        if (result is error) {
            return error Error("Error occurred while stopping the service", result);
        }
    }
}

isolated function retrieveHttpListenerConfig(ListenerConfiguration config) returns http:ListenerConfiguration {
    return {
        host: config.host,
        http1Settings: config.http1Settings,
        secureSocket: config.secureSocket,
        httpVersion: config.httpVersion,
        timeout: config.timeout,
        server: config.server,
        requestLimits: config.requestLimits
    };
}

# Retrieves the `websub:SubscriberServiceConfig` annotation values
# ```ballerina
# websub:SubscriberServiceConfiguration? config = retrieveSubscriberServiceAnnotations('service);
# ```
# 
# + serviceType - Current `websub:SubscriberService` object
# + return - Provided `websub:SubscriberServiceConfiguration` or else `()`
isolated function retrieveSubscriberServiceAnnotations(SubscriberService serviceType) returns SubscriberServiceConfiguration? {
    typedesc<any> serviceTypedesc = typeof serviceType;
    return serviceTypedesc.@SubscriberServiceConfig;
}

isolated function constructCallbackUrl(SubscriberServiceConfiguration subscriberConfig, int port, 
                                       http:InferredListenerConfiguration listenerConfig, string completeSevicePath, 
                                       boolean logGeneratedServicePath) returns string {
    string? providedCallback = subscriberConfig?.callback;
    if providedCallback is string {
        if subscriberConfig.appendServicePath {
            string generatedCallback = string `${providedCallback}/${completeSevicePath}`;
            if logGeneratedServicePath {
                log:printInfo("Autogenerated callback ", URL = generatedCallback);
            }
            return generatedCallback;
        } else {
            return providedCallback;
        }
    } else {
        string host = listenerConfig.host;
        string protocol = listenerConfig.secureSocket is () ? HTTP : HTTPS;
        string generatedCallback = string `${protocol}://${host}:${port.toString()}/${completeSevicePath}`;
        if logGeneratedServicePath {
            log:printInfo("Autogenerated callback ", URL = generatedCallback);
        }
        return generatedCallback;
    }
}

isolated function shouldUseGeneratedServicePath(SubscriberServiceConfiguration subscriberConfig,
                                                string[]|string? name) returns boolean {
    if subscriberConfig?.callback is () && isEmptyServicePath(name) {
        return true;
    }
    return subscriberConfig?.callback is string 
            && subscriberConfig.appendServicePath 
            && isEmptyServicePath(name);
}

isolated function isEmptyServicePath(string[]|string? name) returns boolean {
    return name is () || (name is string[] && name.length() == 0);
}

isolated function retrieveCompleteServicePath(string[]|string? servicePath) returns string|Error {
    if servicePath is () {
        return error Error("Could not find the generated service path");
    } else if servicePath is string {
        return servicePath.startsWith("/") ? servicePath.substring(1): servicePath;
    } else {
        if servicePath.length() == 0 {
            return error Error("Could not find the generated service path");
        } else {
            return strings:'join("/", ...servicePath);
        }
    }
}

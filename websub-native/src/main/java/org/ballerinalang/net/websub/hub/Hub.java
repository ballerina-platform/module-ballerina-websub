/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.net.websub.hub;

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.net.websub.BallerinaConnectorException;
import org.ballerinalang.net.websub.BallerinaWebSubException;
import org.ballerinalang.net.websub.broker.BallerinaBroker;
import org.ballerinalang.net.websub.broker.BallerinaBrokerByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.ballerinalang.net.websub.WebSubSubscriberConstants.STRUCT_WEBSUB_BALLERINA_HUB;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.WEBSUB_PACKAGE_ID;

/**
 * The Ballerina WebSub Hub.
 *
 * @since 0.965.0
 */
public class Hub {
    private static final Logger logger = LoggerFactory.getLogger(Hub.class);

    private static Hub instance = new Hub();
    private BallerinaBroker brokerInstance = null;
    private BObject hubObject = null;
    private String publishUrl;
    private String subscribeUrl;
    private boolean hubTopicRegistrationRequired;
    private volatile boolean started = false;

    // TODO: 9/23/18 make CopyOnWriteArrayList?
    private List<String> topics = new ArrayList<>();
    private List<HubSubscriber> subscribers = new ArrayList<>();
    private ClassLoader classLoader = this.getClass().getClassLoader();

    private String basePath = "/";
    private String publishResourcePath = "/publish";
    private String subscribeResourcePath = "/";
    private static final String HUB_SERVICE = "hub_service";

    private static final String SLASH = "/";
    private Runtime runtime;
    private BObject bridge;

    public static Hub getInstance() {
        return instance;
    }

    private Hub() {
    }

    public String getPublishUrl() {
        return publishUrl;
    }

    public BObject getHubObject() {
        return hubObject;
    }

    public void registerTopic(String topic) throws BallerinaWebSubException {
        if (isTopicRegistered(topic)) {
            throw new BallerinaWebSubException("Topic registration not allowed at the Hub: topic already exists");
        } else if (topic == null || topic.isEmpty()) {
            throw new BallerinaWebSubException("Topic unavailable/invalid for registration at Hub");
        }
        topics.add(topic);
    }

    public void unregisterTopic(String topic) throws BallerinaWebSubException {
        if (topic == null || !isTopicRegistered(topic)) {
            throw new BallerinaWebSubException("Topic unavailable/invalid for unregistration at Hub");
        }
        topics.remove(topic);
    }

    public boolean isTopicRegistered(String topic) {
        return topics.contains(topic);
    }

    /**
     * Method to add a subscription to the topic on MB.
     * @param topic     the topic to which the subscription should be added
     * @param callback  the callback registered for the particular subscription
     * @param subscriptionDetails the subscription details
     */
    public void registerSubscription(String topic, String callback,
                                     BMap<BString, Object> subscriptionDetails) {
        if (!started) {
            //TODO: Revisit to check if this needs to be returned as an error, currently not required since this check
            // is performed at Ballerina level
            logger.error("Hub Service not started: subscription failed");
        } else if (!topics.contains(topic) && hubTopicRegistrationRequired) {
            logger.warn("Subscription request ignored for unregistered topic[" + topic + "]");
        } else {
            HubSubscriber subscriberToAdd = new HubSubscriber(null, null, topic, callback, null, null);
            if (getSubscribers().contains(subscriberToAdd)) {
                unregisterSubscription(topic, callback);
            }
            String queue = UUID.randomUUID().toString();

            subscriberToAdd = new HubSubscriber(runtime, queue, topic, callback, subscriptionDetails, bridge);
            brokerInstance.addSubscription(topic, subscriberToAdd);
            getSubscribers().add(subscriberToAdd);
        }
    }

    /**
     * Method to remove a subscription to the topic on MB.
     *
     * @param topic     the topic to which the subscription should was added
     * @param callback  the callback registered for the particular subscription
     */
    public void unregisterSubscription(String topic, String callback) {
        if (!started) {
            logger.error("Hub Service not started: unsubscription failed.");
            return;
        }
        HubSubscriber subscriberToUnregister = new HubSubscriber(null, "", topic, callback, null, null);
        if (!getSubscribers().contains(subscriberToUnregister)) {
            if (callback.endsWith("/")) {
                unregisterSubscription(topic, callback.substring(0, callback.length() - 1));
            }
            return;
        } else {
            List<HubSubscriber> currentSubscriberList = getSubscribers();
            for (HubSubscriber subscriber: currentSubscriberList) {
                if (subscriber.equals(subscriberToUnregister)) {
                    subscriberToUnregister = subscriber;
                    break;
                }
            }
        }
        brokerInstance.removeSubscription(subscriberToUnregister);
        getSubscribers().remove(subscriberToUnregister);
    }

    /**
     * Method to publish to a topic on MB, a request to send to subscribers.
     *
     * @param topic             the topic to which the update should happen
     * @param content           the content to send, with payload and content type set
     * @throws BallerinaWebSubException if the hub service is not started or topic registration is required, but the
     *                                  topic is not registered
     */
    public void publish(String topic, BMap<BString, Object> content) throws BallerinaWebSubException {
        if (!started) {
            throw new BallerinaWebSubException("Hub Service not started: publish failed");
        } else if (!topics.contains(topic) && hubTopicRegistrationRequired) {
            throw new BallerinaWebSubException("Publish call ignored for unregistered topic[" + topic + "]");
        } else {
            brokerInstance.publish(topic, new BallerinaBrokerByteBuf(content));
        }
    }

    public boolean isStarted() {
        return started;
    }

    /**
     * Method to start up the default Ballerina WebSub Hub.
     *
     * @param basePath                  the base path of the hub service
     * @param subscriptionResourcePath  the resource path for subscription
     * @param publishResourcePath       the resource path for publishing and topic registration
     * @param topicRegistrationRequired whether a topic needs to be registered at the hub prior to
     *                                  publishing/subscribing to the topic
     * @param publicUrl                 the URL for the hub to be included in content delivery requests
     * @param hubListener               the http:Listener to which the hub service is attached
     * @return the hub object if the hub was started up successfully, error if not
     */
    @SuppressWarnings("unchecked")
    public Object startUpHubService(Runtime runtime, String basePath, String subscriptionResourcePath,
                                    String publishResourcePath, boolean topicRegistrationRequired, String publicUrl,
                                    BObject hubListener, BObject bridge) {
        synchronized (this) {
            if (!isStarted()) {
                try {
                    brokerInstance = BallerinaBroker.getBrokerInstance();
                } catch (Exception e) {
                    throw new BallerinaConnectorException("Error starting up internal broker for WebSub Hub");
                }
                this.basePath = basePath.startsWith(SLASH) ? basePath : SLASH.concat(basePath);
                this.subscribeResourcePath = subscriptionResourcePath.startsWith(SLASH) ? subscriptionResourcePath :
                        SLASH.concat(subscriptionResourcePath);
                this.publishResourcePath = publishResourcePath.startsWith(SLASH) ? publishResourcePath :
                        SLASH.concat(publishResourcePath);
                hubTopicRegistrationRequired = topicRegistrationRequired;
                String publishUrl = populatePublishUrl(publicUrl, hubListener);
                String subscribeUrl = populateSubscribeUrl(publicUrl, hubListener);

                started = true;
                //TODO: this has to be re-written in a non-blocking way for better performance
                CountDownLatch completeFunction = new CountDownLatch(1);
                final BError[] errorValue = new BError[1];
                runtime.invokeMethodAsync(bridge, "setupOnStartup",
                                          null, null, new Callback() {
                            @Override
                            public void notifySuccess(Object result) {
                                completeFunction.countDown();
                            }

                            @Override
                            public void notifyFailure(BError error) {
                                started = false;
                                errorValue[0] = error;
                                completeFunction.countDown();
                            }
                        });
                try {
                    completeFunction.await();
                } catch (InterruptedException e) {
                    started = false;
                    return ErrorCreator.createError(StringUtils.fromString("Hub start timeout"), e);
                }

                if (errorValue[0] != null) {
                    return errorValue[0];
                }

                PrintStream console = System.err;
                console.println("[ballerina/websub] Ballerina WebSub Hub started up.\n[ballerina/websub] Publish URL:" +
                                        " " + publishUrl + "\n[ballerina/websub] Subscription URL: " + subscribeUrl);
                setPublishUrl(publishUrl);
                setSubscribeUrl(subscribeUrl);

                BObject hubObject = ValueCreator.createObjectValue(
                        WEBSUB_PACKAGE_ID, STRUCT_WEBSUB_BALLERINA_HUB, StringUtils.fromString(subscribeUrl),
                        StringUtils.fromString(publishUrl), hubListener);
                setHubObject(hubObject);
                this.runtime = runtime;
                this.bridge = bridge;
                return hubObject;
            } else {
                throw new BallerinaWebSubException("Hub Service already started up");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String populatePublishUrl(String publicUrl, BObject hubListener) {
        if (publicUrl.isEmpty()) {
            String hubPort = String.valueOf(hubListener.get(StringUtils.fromString("port")));
            Object secureSocket = ((BMap<BString, Object>) hubListener.get(
                    StringUtils.fromString("config"))).get(StringUtils.fromString("secureSocket"));

            String path = basePath.equals(SLASH) ? publishResourcePath : basePath.concat(publishResourcePath);
            return secureSocket != null ? ("https://localhost:" + hubPort + path)
                    : ("http://localhost:" + hubPort + path);
        }
        return publicUrl.concat(basePath.equals(SLASH) ? publishResourcePath : basePath.concat(publishResourcePath));
    }

    @SuppressWarnings("unchecked")
    private String populateSubscribeUrl(String publicUrl, BObject hubListener) {
        if (publicUrl.isEmpty()) {
            String hubPort = String.valueOf(hubListener.get(StringUtils.fromString("port")));
            Object secureSocket = ((BMap<BString, Object>) hubListener.get(
                    StringUtils.fromString("config"))).get(StringUtils.fromString("secureSocket"));

            String path = basePath.equals(SLASH) ? subscribeResourcePath : basePath.concat(subscribeResourcePath);
            return secureSocket != null ? ("https://localhost:" + hubPort + path)
                    : ("http://localhost:" + hubPort + path);
        }
        return publicUrl.concat(basePath.equals(SLASH) ? subscribeResourcePath :
                                        basePath.concat(subscribeResourcePath));
    }

    /**
     * Method to clean up after stopping the default Ballerina WebSub Hub.
     */
    public void stopHubService() {
        synchronized (this) {
            if (isStarted()) {
                started = false;
                setHubObject(null);
                setSubscribeUrl(null);
                setPublishUrl(null);
                hubTopicRegistrationRequired = false;
                topics = new ArrayList<>();
                for (HubSubscriber subscriber : getSubscribers()) {
                    brokerInstance.removeSubscription(subscriber);
                }
                subscribers = new ArrayList<>();
                brokerInstance = null;
            } else {
                throw new BallerinaWebSubException("error stopping the hub service: already stopped");
            }
        }
    }

    private void setSubscribeUrl(String subscribeUrl) {
        this.subscribeUrl = subscribeUrl;
    }

    private void setPublishUrl(String publishUrl) {
        this.publishUrl = publishUrl;
    }

    private void setHubObject(BObject hubObject) {
        this.hubObject = hubObject;
    }

    /**
     * Retrieve available topics of the Hub.
     *
     * @return the array of topics
     */
    public String[] getTopics() {
        return topics.toArray(new String[0]);
    }

    /**
     * Retrieve subscribers list.
     *
     * @return the list of subscribers
     */
    public List<HubSubscriber> getSubscribers() {
        return subscribers;
    }
}

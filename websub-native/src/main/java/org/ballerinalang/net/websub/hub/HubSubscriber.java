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

import io.ballerina.messaging.broker.core.BrokerException;
import io.ballerina.messaging.broker.core.Consumer;
import io.ballerina.messaging.broker.core.Message;
import org.ballerinalang.jvm.api.BStringUtils;
import org.ballerinalang.jvm.api.values.BMap;
import org.ballerinalang.jvm.api.values.BString;
import org.ballerinalang.jvm.scheduling.Scheduler;
import org.ballerinalang.jvm.scheduling.Strand;
import org.ballerinalang.jvm.util.exceptions.BallerinaException;
import org.ballerinalang.jvm.api.BExecutor;
import org.ballerinalang.net.websub.broker.BallerinaBrokerByteBuf;

import java.util.Objects;
import java.util.Properties;

import static org.ballerinalang.net.websub.WebSubSubscriberConstants.BALLERINA;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.GENERATED_PACKAGE_VERSION;
import static org.ballerinalang.net.websub.WebSubSubscriberConstants.WEBSUB;

/**
 * WebSub Subscriber representation for the Broker.
 *
 * @since 0.965.0
 */
public class HubSubscriber extends Consumer {

    private final String queue;
    private final String topic;
    private final String callback;
    private final BMap<BString, Object> subscriptionDetails;
    private final Scheduler scheduler;

    HubSubscriber(Strand strand, String queue, String topic, String callback,
                  BMap<BString, Object> subscriptionDetails) {
        this.scheduler = strand.scheduler;
        this.queue = queue;
        this.topic = topic;
        this.callback = callback;
        this.subscriptionDetails = subscriptionDetails;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void send(Message message) throws BrokerException {
        BMap<BString, Object> content =
                (BMap<BString, Object>) ((BallerinaBrokerByteBuf) (message.getContentChunks().get(0).getByteBuf())
                        .unwrap()).getValue();
        Object[] args = {BStringUtils.fromString(getCallback()), getSubscriptionDetails(), content};
        try {
            BExecutor.executeFunction(scheduler, null, null, this.getClass().getClassLoader(), BALLERINA, WEBSUB,
                                     GENERATED_PACKAGE_VERSION, "hub_service", "distributeContent", args);
        } catch (BallerinaException e) {
            throw new BallerinaException("send failed: " + e.getMessage());
        }
    }

    @Override
    public String getQueueName() {
        return queue;
    }

    @Override
    protected void close() throws BrokerException {

    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public boolean equals(Object subscriberObject) {
        if (subscriberObject instanceof HubSubscriber) {
            HubSubscriber subscriber = (HubSubscriber) subscriberObject;
            return subscriber.getTopic().equals(getTopic()) && subscriber.getCallback().equals(getCallback());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTopic(), getCallback());
    }

    @Override
    public Properties getTransportProperties() {
        return new Properties();
    }

    public String getTopic() {
        return topic;
    }

    public String getCallback() {
        return callback;
    }

    public BMap<BString, Object> getSubscriptionDetails() {
        return subscriptionDetails;
    }
}

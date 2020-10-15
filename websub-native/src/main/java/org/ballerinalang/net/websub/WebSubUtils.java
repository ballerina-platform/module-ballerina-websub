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

package org.ballerinalang.net.websub;

import org.ballerinalang.jvm.JSONParser;
import org.ballerinalang.jvm.api.BErrorCreator;
import org.ballerinalang.jvm.api.BStringUtils;
import org.ballerinalang.jvm.api.BValueCreator;
import org.ballerinalang.jvm.api.values.BError;
import org.ballerinalang.jvm.api.values.BMap;
import org.ballerinalang.jvm.api.values.BObject;
import org.ballerinalang.jvm.api.values.BString;
import org.ballerinalang.jvm.types.AttachedFunction;
import org.ballerinalang.jvm.util.exceptions.BallerinaConnectorException;
import org.ballerinalang.mime.util.EntityBodyHandler;
import org.ballerinalang.mime.util.MimeConstants;
import org.ballerinalang.mime.util.MimeUtil;
import org.ballerinalang.net.http.HttpConstants;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.net.transport.message.HttpCarbonMessage;

/**
 * Util class for WebSub.
 */
public class WebSubUtils {

    public static final String WEBSUB_ERROR = "WebSubError";

    static BObject getHttpRequest(HttpCarbonMessage httpCarbonMessage) {
        BObject httpRequest = BValueCreator.createObjectValue(HttpConstants.PROTOCOL_HTTP_PKG_ID,
                                                                    HttpConstants.REQUEST);
        BObject inRequestEntity = BValueCreator.createObjectValue(MimeConstants.PROTOCOL_MIME_PKG_ID,
                                                                        MimeConstants.ENTITY);

        HttpUtil.populateInboundRequest(httpRequest, inRequestEntity, httpCarbonMessage);
        HttpUtil.populateEntityBody(httpRequest, inRequestEntity, true, true);
        return httpRequest;
    }

    // TODO: 8/1/18 Handle duplicate code
    @SuppressWarnings("unchecked")
    static BMap<BString, ?> getJsonBody(BObject httpRequest) {
        BObject entityObj = HttpUtil.extractEntity(httpRequest);
        if (entityObj != null) {
            Object dataSource = EntityBodyHandler.getMessageDataSource(entityObj);
            String stringPayload;
            if (dataSource != null) {
                stringPayload = MimeUtil.getMessageAsString(dataSource);
            } else {
                stringPayload = EntityBodyHandler.constructStringDataSource(entityObj).getValue();
                EntityBodyHandler.addMessageDataSource(entityObj, stringPayload);
                // Set byte channel to null, once the message data source has been constructed
                entityObj.addNativeData(MimeConstants.ENTITY_BYTE_CHANNEL, null);
            }

            Object result = JSONParser.parse(stringPayload);
            if (result instanceof BMap) {
                return (BMap<BString, ?>) result;
            }
            throw new BallerinaConnectorException("Non-compatible payload received for payload key based dispatching");
        } else {
            throw new BallerinaConnectorException("Error retrieving payload for payload key based dispatching");
        }
    }

    public static AttachedFunction getAttachedFunction(BObject service, String functionName) {
        AttachedFunction attachedFunction = null;
        String functionFullName = service.getType().getName() + "." + functionName;
        for (AttachedFunction function : service.getType().getAttachedFunctions()) {
            //TODO test the name of resource
            if (functionFullName.contains(function.getName())) {
                attachedFunction = function;
            }
        }
        return attachedFunction;
    }

    /**
     * Create WebSub specific error record with 'WebSubError' as error type ID name.
     *
     * @param errMsg  Actual error message
     * @return Ballerina error value
     */
    public static BError createError(String errMsg) {
        return BErrorCreator.createDistinctError(WEBSUB_ERROR, WebSubSubscriberConstants.WEBSUB_PACKAGE_ID,
                                                 BStringUtils.fromString(errMsg));
    }

    /**
     * Create WebSub specific error for a given error message.
     *
     * @param typeIdName  The error type ID name
     * @param message  The Actual error cause
     * @return Ballerina error value
     */
    public static BError createError(String typeIdName, String message) {
        return BErrorCreator.createDistinctError(typeIdName, WebSubSubscriberConstants.WEBSUB_PACKAGE_ID,
                                                 BStringUtils.fromString(message));
    }
}

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

import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.values.BMap;
import org.ballerinalang.net.http.HttpResource;
import org.ballerinalang.net.http.HttpService;

/**
 * WebSub HTTP wrapper for the {@code Resource} implementation. TODO: remove dependency on HTTP resource
 *
 * @since 0.965.0
 */
class WebSubHttpResource extends HttpResource {

    private WebSubHttpResource(MethodType resource, HttpService parentService) {
        super(resource, parentService);
    }

    /**
     * Builds the WebSub HTTP resource representation for the resource.
     *
     * @param resource      the resource of the service for which the HTTP resource is built
     * @param httpService   the HTTP service representation of the service
     * @return  the built HTTP resource
     */
    static HttpResource buildWebSubHttpResource(MethodType resource, HttpService httpService) {
        WebSubHttpResource httpResource = new WebSubHttpResource(resource, httpService);
        BMap resourceConfigAnnotation = getResourceConfigAnnotation(resource);

        if (resourceConfigAnnotation != null) {
            throw new BallerinaConnectorException(
                    "resourceConfig annotation not allowed for WebSubSubscriber resource");
        }

        httpResource.setPath("/");
        return httpResource;
    }
}

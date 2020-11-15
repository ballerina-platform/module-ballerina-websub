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

import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BLink;
import io.ballerina.runtime.api.values.BString;

import java.util.List;
import java.util.Map;

/**
 * Represents a Runtime Exception that could be thrown when performing WebSub actions.
 *
 * @since 0.970.2
 */
public class BallerinaWebSubException extends BError {

    public BallerinaWebSubException(String message) {
        super(StringUtils.fromString(message));
    }

    @Override
    public BString getErrorMessage() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public BError getCause() {
        return null;
    }

    @Override
    public String getPrintableStackTrace() {
        return null;
    }

    @Override
    public List<StackTraceElement> getCallStack() {
        return null;
    }

    @Override
    public Object copy(Map<Object, Object> map) {
        return null;
    }

    @Override
    public Object frozenCopy(Map<Object, Object> map) {
        return null;
    }

    @Override
    public String stringValue(BLink bLink) {
        return null;
    }

    @Override
    public String expressionStringValue(BLink bLink) {
        return null;
    }

    @Override
    public Type getType() {
        return null;
    }
}

package org.ballerinalang.net.websub;

import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.types.AttachedFunction;
import io.ballerina.runtime.api.types.AttachedFunctionType;
import io.ballerina.runtime.util.exceptions.BallerinaException;
import org.ballerinalang.net.http.HttpResource;
import org.ballerinalang.net.http.HttpService;

/**
 * WebSub HTTP wrapper for the {@code Resource} implementation. TODO: remove dependency on HTTP resource
 *
 * @since 0.965.0
 */
class WebSubHttpResource extends HttpResource {

    private WebSubHttpResource(AttachedFunctionType resource, HttpService parentService) {
        super(resource, parentService);
    }

    /**
     * Builds the WebSub HTTP resource representation for the resource.
     *
     * @param resource      the resource of the service for which the HTTP resource is built
     * @param httpService   the HTTP service representation of the service
     * @return  the built HTTP resource
     */
    static HttpResource buildWebSubHttpResource(AttachedFunctionType resource, HttpService httpService) {
        WebSubHttpResource httpResource = new WebSubHttpResource(resource, httpService);
        BMap resourceConfigAnnotation = getResourceConfigAnnotation(resource);

        if (resourceConfigAnnotation != null) {
            throw new BallerinaException("resourceConfig annotation not allowed for WebSubSubscriber resource");
        }

        httpResource.setPath("/");
        return httpResource;
    }
}

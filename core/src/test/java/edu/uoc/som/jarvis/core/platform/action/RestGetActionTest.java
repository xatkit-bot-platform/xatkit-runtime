package edu.uoc.som.jarvis.core.platform.action;

import com.google.gson.JsonElement;
import com.mashape.unirest.http.Headers;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.stubs.StubRuntimePlatform;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RestGetActionTest extends RestActionTest {

    /*
     * Do not test limit values for runtimePlatform and session, this is done in RuntimeAction tests.
     */

    @Test
    public void constructValidGetRequest() {
        new StubRestGetAction(RUNTIME_PLATFORM, session, Collections.emptyMap(), VALID_GET_ENDPOINT,
                Collections.emptyMap());
        /*
         * Do not test here that the query is actually performed, this is done in compute() tests.
         */
    }

    @Test
    public void constructNullHeaders() {
        new StubRestGetAction(RUNTIME_PLATFORM, session, null, VALID_GET_ENDPOINT, Collections.emptyMap());
        /*
         * Shouldn't throw an exception, headers is @Nullable
         */
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNullEndpoint() {
        new StubRestGetAction(RUNTIME_PLATFORM, session, Collections.emptyMap(), null, Collections.emptyMap());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructEmptyEndpoint() {
        new StubRestGetAction(RUNTIME_PLATFORM, session, Collections.emptyMap(), "", Collections.emptyMap());
    }

    @Test
    public void constructNullParams() {
        new StubRestGetAction(RUNTIME_PLATFORM, session, Collections.emptyMap(), VALID_GET_ENDPOINT, null);
        /*
         * Shouldn't throw an exception, headers is @Nullable
         */
    }

    @Test
    public void computeValidGetEndpoint() throws Exception {
        StubRestGetAction stubRestGetAction = new StubRestGetAction(RUNTIME_PLATFORM, session, Collections.emptyMap()
                , VALID_GET_ENDPOINT, Collections.emptyMap());
        stubRestGetAction.compute();
        /*
         * Soft checks, the goal is not to test the result of the API, just to ensure that the response code,
         * headers, and Json content have been set.
         */
        checkValidGetResponse(stubRestGetAction.getResponseStatus(),
                stubRestGetAction.getResponseJsonElement(), stubRestGetAction.getResponseHeaders());
    }

    public static void checkValidGetResponse(int responseStatus, JsonElement responseJsonElement,
                                             Headers responseHeaders) {
        assertThat(responseStatus).as("Valid response status").isEqualTo(200);
        assertThat(responseJsonElement).as("Valid JsonElement").isInstanceOf(JsonElement.class);
        assertThat(responseHeaders).as("Valid response Headers").isNotNull();
    }

    private static class StubRestGetAction extends RestGetAction<StubRuntimePlatform> {

        private Headers responseHeaders;

        private int responseStatus;

        private JsonElement responseJsonElement;

        public StubRestGetAction(StubRuntimePlatform runtimePlatform, JarvisSession session, Map<String,
                String> headers, String restEndpoint, Map<String, Object> params) {
            super(runtimePlatform, session, headers, restEndpoint, params);
        }

        @Override
        protected Object handleResponse(Headers headers, int status, JsonElement jsonElement) {
            this.responseHeaders = headers;
            this.responseStatus = status;
            this.responseJsonElement = jsonElement;
            return jsonElement;
        }

        public Headers getResponseHeaders() {
            return responseHeaders;
        }

        public int getResponseStatus() {
            return responseStatus;
        }

        public JsonElement getResponseJsonElement() {
            return responseJsonElement;
        }
    }
}

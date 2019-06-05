package com.xatkit.core.platform.action;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mashape.unirest.http.Headers;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.stubs.StubRuntimePlatform;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RestPostActionTest extends RestActionTest {

    /*
     * Do not test limit values for runtimePlatform and session, this is done in RuntimeAction tests.
     */

    @Test
    public void constructValidPostRequest() {
        new StubRestPostAction(RUNTIME_PLATFORM, session, Collections.emptyMap(), VALID_POST_ENDPOINT,
                Collections.emptyMap(), null);
        /*
         * Do not test here that the query is actually performed, this is done in compute() tests.
         * They are probably some additional parameters to provide to compute this action (e.g. headers and body),
         * this is done in compute() tests.
         */
    }

    @Test
    public void constructNullHeaders() {
        new StubRestPostAction(RUNTIME_PLATFORM, session, null, VALID_POST_ENDPOINT, Collections.emptyMap(), null);
        /*
         * Shouldn't throw an exception, headers is @Nullable
         */
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNullEndpoint() {
        new StubRestPostAction(RUNTIME_PLATFORM, session, Collections.emptyMap(), null, Collections.emptyMap(),
                null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructEmptyEndpoint() {
        new StubRestPostAction(RUNTIME_PLATFORM, session, Collections.emptyMap(), "", Collections.emptyMap(), null);
    }

    @Test
    public void constructNullParams() {
        new StubRestPostAction(RUNTIME_PLATFORM, session, Collections.emptyMap(), VALID_POST_ENDPOINT, null, null);
        /*
         * Shouldn't throw an exception, headers is @Nullable
         */
    }

    @Test
    public void computeValidPostEndpoint() throws Exception {
        StubRestPostAction stubRestPostAction = new StubRestPostAction(RUNTIME_PLATFORM, session,
                Collections.emptyMap(), VALID_POST_ENDPOINT, Collections.emptyMap(), VALID_POST_BODY);
        stubRestPostAction.compute();
        /*
         * Soft checks, the goal is not to test the result of the API, just to ensure that the response code,
         * headers, and Json content have been set.
         * Note that we also check that the Json content contains the new "id" field, that has been created after
         * processing the POST request.
         */
        checkValidPostResponse(stubRestPostAction.getResponseStatus(),
                stubRestPostAction.getResponseJsonElement(),
                stubRestPostAction.getResponseHeaders());
    }

    private static void checkValidPostResponse(int responseStatus, JsonElement responseJsonElement,
                                              Headers responseHeaders) {
        assertThat(responseStatus).as("Valid response status").isEqualTo(201);
        assertThat(responseJsonElement).as("Valid JsonElement").isInstanceOf(JsonElement.class);
        JsonObject jsonObject = responseJsonElement.getAsJsonObject();
        assertThat(jsonObject.has("id")).as("JsonElement contains the new field").isTrue();
        assertThat(responseHeaders).as("Valid response Headers").isNotNull();
    }

    private static class StubRestPostAction extends RestPostAction<StubRuntimePlatform> {

        private Headers responseHeaders;

        private int responseStatus;

        private JsonElement responseJsonElement;

        public StubRestPostAction(StubRuntimePlatform runtimePlatform, XatkitSession session,
                                  Map<String, String> headers, String restEndpoint, Map<String, Object> params,
                                  @Nullable JsonElement jsonContent) {
            super(runtimePlatform, session, headers, restEndpoint, params, jsonContent);
        }

        @Override
        protected Object handleResponse(Headers headers, int status, InputStream body) {
            JsonElement jsonElement = getJsonBody(body);
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

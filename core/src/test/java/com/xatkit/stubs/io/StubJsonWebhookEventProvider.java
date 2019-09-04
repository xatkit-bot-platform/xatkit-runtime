package com.xatkit.stubs.io;

import com.xatkit.core.platform.io.WebhookEventProvider;
import com.xatkit.core.server.JsonRestHandler;
import com.xatkit.core.server.RestHandlerFactory;
import com.xatkit.stubs.EmptyRuntimePlatform;

public class StubJsonWebhookEventProvider extends WebhookEventProvider<EmptyRuntimePlatform, JsonRestHandler> {

    private static boolean eventReceived;

    /**
     * Use a public static handler to ease testing (the handler can be quickly compared to the registered ones from
     * {@link com.xatkit.core.server.XatkitServer}.
     */
    public static JsonRestHandler handler =
            RestHandlerFactory.createJsonRestHandler((headers, params, content) -> {
                eventReceived = true;
                return null;
            });

    public StubJsonWebhookEventProvider(EmptyRuntimePlatform runtimePlatform) {
        super(runtimePlatform);
        eventReceived = false;
    }

    @Override
    public String getEndpointURI() {
        return "/stubJsonProvider";
    }

    @Override
    protected JsonRestHandler createRestHandler() {
        return handler;
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {

            }
        }
    }

    public boolean hasReceivedEvent() {
        return eventReceived;
    }
}

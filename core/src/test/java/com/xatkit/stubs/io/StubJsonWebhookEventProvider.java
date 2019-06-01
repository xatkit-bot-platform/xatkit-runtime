package edu.uoc.som.jarvis.stubs.io;

import com.google.gson.JsonElement;
import edu.uoc.som.jarvis.core.platform.io.JsonWebhookEventProvider;
import edu.uoc.som.jarvis.stubs.EmptyRuntimePlatform;
import org.apache.http.Header;

public class StubJsonWebhookEventProvider extends JsonWebhookEventProvider<EmptyRuntimePlatform> {

    private boolean eventReceived;

    public StubJsonWebhookEventProvider(EmptyRuntimePlatform runtimePlatform) {
        super(runtimePlatform);
        this.eventReceived = false;
    }

    @Override
    protected void handleParsedContent(JsonElement parsedContent, Header[] headers) {
        eventReceived = true;
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

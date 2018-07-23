package fr.zelus.jarvis.stubs.io;

import com.google.gson.JsonElement;
import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.io.JsonWebhookEventProvider;
import fr.zelus.jarvis.io.WebhookEventProvider;

public class StubJsonWebhookEventProvider extends JsonWebhookEventProvider {

    private boolean eventReceived;

    public StubJsonWebhookEventProvider(JarvisCore jarvisCore) {
        super(jarvisCore);
        this.eventReceived = false;
    }

    @Override
    protected void handleParsedContent(JsonElement parsedContent) {
        eventReceived = true;
    }

    @Override
    public void run() {

    }

    public boolean hasReceivedEvent() {
        return eventReceived;
    }
}

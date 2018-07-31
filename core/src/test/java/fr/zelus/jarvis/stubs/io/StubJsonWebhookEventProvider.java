package fr.zelus.jarvis.stubs.io;

import com.google.gson.JsonElement;
import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.io.JsonWebhookEventProvider;
import org.apache.http.Header;

public class StubJsonWebhookEventProvider extends JsonWebhookEventProvider {

    private boolean eventReceived;

    public StubJsonWebhookEventProvider(JarvisCore jarvisCore) {
        super(jarvisCore);
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

package fr.zelus.jarvis.plugins.github.module.io;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.zelus.jarvis.core.EventDefinitionRegistry;
import fr.zelus.jarvis.intent.EventInstance;
import fr.zelus.jarvis.io.EventInstanceBuilder;

import java.util.Arrays;
import java.util.List;

import static fr.zelus.jarvis.io.JsonWebhookEventProvider.JsonHelper.getJsonElementFromJsonObject;


public class GithubForkEventBuilder {

    public static List<EventInstance> handleForkEvent(JsonElement parsedContent, EventDefinitionRegistry
            eventRegistry) {
        JsonObject json = parsedContent.getAsJsonObject();
        JsonObject forkeeObject = getJsonElementFromJsonObject(json, "forkee").getAsJsonObject();
        String forkeeFullName = getJsonElementFromJsonObject(forkeeObject, "full_name").getAsString();
        String forkeeURL = getJsonElementFromJsonObject(forkeeObject, "html_url").getAsString();
        JsonObject repositoryObject = getJsonElementFromJsonObject(json, "repository").getAsJsonObject();
        String forkedFullName = getJsonElementFromJsonObject(repositoryObject, "full_name").getAsString();
        String forkedURL = getJsonElementFromJsonObject(repositoryObject, "html_url").getAsString();
        JsonObject senderObject = getJsonElementFromJsonObject(json, "sender").getAsJsonObject();
        String senderLogin = getJsonElementFromJsonObject(senderObject, "login").getAsString();
        EventInstanceBuilder builder = EventInstanceBuilder.newBuilder(eventRegistry)
                .setEventDefinitionName("Forked_Repository")
                .setOutContextValue("forked_name", forkedFullName)
                .setOutContextValue("forked_url", forkedURL)
                .setOutContextValue("forkee_name", forkeeFullName)
                .setOutContextValue("forkee_url", forkeeURL)
                .setOutContextValue("user", senderLogin);
        return Arrays.asList(builder.build());
    }

}

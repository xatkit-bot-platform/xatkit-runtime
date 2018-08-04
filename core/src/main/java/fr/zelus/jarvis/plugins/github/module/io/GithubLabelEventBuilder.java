package fr.zelus.jarvis.plugins.github.module.io;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.zelus.jarvis.core.EventDefinitionRegistry;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.intent.EventInstance;
import fr.zelus.jarvis.io.EventInstanceBuilder;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import static fr.zelus.jarvis.io.JsonWebhookEventProvider.JsonHelper.getJsonElementFromJsonObject;
import static java.util.Objects.nonNull;

public class GithubLabelEventBuilder {

    private final static String GITHUB_LABEL_ACTION_CREATED = "created";

    private final static String GITHUB_LABEL_ACTION_EDITED = "edited";

    private final static String GITHUB_LABEL_ACTION_DELETED = "deleted";

    public static List<EventInstance> handleGithubLabelEvent(JsonElement parsedContent, EventDefinitionRegistry
            eventRegistry) {
        JsonObject json = parsedContent.getAsJsonObject();
        String action = getJsonElementFromJsonObject(json, "action").getAsString();
        JsonObject labelObject = getJsonElementFromJsonObject(json, "label").getAsJsonObject();
        String name = getJsonElementFromJsonObject(labelObject, "name").getAsString();
        String color = getJsonElementFromJsonObject(labelObject, "color").getAsString();
        JsonObject senderObject = getJsonElementFromJsonObject(json, "sender").getAsJsonObject();
        String senderLogin = getJsonElementFromJsonObject(senderObject, "login").getAsString();
        EventInstanceBuilder builder = EventInstanceBuilder.newBuilder(eventRegistry);
        switch(action) {
            case GITHUB_LABEL_ACTION_CREATED:
                builder.setEventDefinitionName("Label_Created");
                break;
            case GITHUB_LABEL_ACTION_EDITED:
                builder.setEventDefinitionName("Label_Edited");
                JsonObject changesObject = getJsonElementFromJsonObject(json, "changes").getAsJsonObject();
                JsonObject changesNameObject = changesObject.getAsJsonObject("name");
                if(nonNull(changesNameObject)) {
                    String previousName = getJsonElementFromJsonObject(changesNameObject, "from").getAsString();
                    builder.setOutContextValue("old_name", previousName);
                } else {
                    /*
                     * The title has not been updated, setting the old title with the value of the current one.
                     */
                    builder.setOutContextValue("old_name", name);
                }
                JsonObject changesColorObject = changesObject.getAsJsonObject("color");
                if(nonNull(changesColorObject)) {
                    String previousColor = getJsonElementFromJsonObject(changesColorObject, "from").getAsString();
                    builder.setOutContextValue("old_color", previousColor);
                } else  {
                    /*
                     * The body has not been updated, setting the old body with the value of the current one.
                     */
                    builder.setOutContextValue("old_color", color);
                }
                break;
            case GITHUB_LABEL_ACTION_DELETED:
                builder.setEventDefinitionName("Label_Deleted");
                break;
            default:
                throw new JarvisException(MessageFormat.format("Unknown Issue action {0}", action));

        }
        builder.setOutContextValue("name", name)
                .setOutContextValue("color", color)
                .setOutContextValue("user", senderLogin);
        return Arrays.asList(builder.build());
    }
}

package fr.zelus.jarvis.plugins.github.module.io;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.zelus.jarvis.core.EventDefinitionRegistry;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.intent.EventInstance;
import fr.zelus.jarvis.io.EventInstanceBuilder;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import static fr.zelus.jarvis.io.JsonWebhookEventProvider.JsonHelper.getJsonElementFromJsonObject;


public class GithubGollumEventBuilder {

    private final static String WIKI_PAGE_CREATED = "created";

    private final static String WIKI_PAGE_EDITED = "edited";

    public static List<EventInstance> handleGithubGollumEvent(JsonElement parsedContent, EventDefinitionRegistry
            eventRegistry) {
        List<EventInstance> eventInstances = new ArrayList<>();
        JsonObject json = parsedContent.getAsJsonObject();
        JsonArray pages = getJsonElementFromJsonObject(json, "pages").getAsJsonArray();
        JsonObject senderObject = getJsonElementFromJsonObject(json, "sender").getAsJsonObject();
        String senderLogin = getJsonElementFromJsonObject(senderObject, "login").getAsString();
        StreamSupport.stream(pages.spliterator(), false).forEach(page -> {
            EventInstanceBuilder builder = EventInstanceBuilder.newBuilder(eventRegistry);
            builder.setOutContextValue("user", senderLogin);
            JsonObject pageObject = page.getAsJsonObject();
            String pageName = getJsonElementFromJsonObject(pageObject, "page_name").getAsString();
            String pageTitle = getJsonElementFromJsonObject(pageObject, "title").getAsString();
            String pageUrl = getJsonElementFromJsonObject(pageObject, "html_url").getAsString();
            String action = getJsonElementFromJsonObject(pageObject, "action").getAsString();
            if(action.equals(WIKI_PAGE_CREATED)) {
                builder.setEventDefinitionName("Wiki_Page_Created");
            } else if(action.equals(WIKI_PAGE_EDITED)) {
                builder.setEventDefinitionName("Wiki_Page_Edited");
            } else {
                throw new JarvisException(MessageFormat.format("Unknown wiki page action {0}", action));
            }
            builder.setOutContextValue("page_name", pageName);
            builder.setOutContextValue("page_title", pageTitle);
            builder.setOutContextValue("url", pageUrl);
            eventInstances.add(builder.build());
        });
        return eventInstances;
    }

}

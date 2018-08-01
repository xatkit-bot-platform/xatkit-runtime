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


public class GithubIssueCommentEventBuilder {

    private final static String GITHUB_ISSUE_COMMENT_ACTION_CREATED = "created";

    private final static String GITHUB_ISSUE_COMMENT_ACTION_EDITED = "edited";

    private final static String GITHUB_ISSUE_COMMENT_ACTION_DELETED = "deleted";


    public static List<EventInstance> handleGithubIssueCommentEvent(JsonElement parsedContent, EventDefinitionRegistry
            eventRegistry) {
        JsonObject json = parsedContent.getAsJsonObject();
        String action = getJsonElementFromJsonObject(json, "action").getAsString();
        JsonObject commentObject = getJsonElementFromJsonObject(json, "comment").getAsJsonObject();
        String body = getJsonElementFromJsonObject(commentObject, "body").getAsString();
        String url = getJsonElementFromJsonObject(commentObject, "html_url").getAsString();
        JsonObject commentUserObject = getJsonElementFromJsonObject(commentObject, "user").getAsJsonObject();
        String user = getJsonElementFromJsonObject(commentUserObject, "login").getAsString();
        JsonObject issueObject = getJsonElementFromJsonObject(json, "issue").getAsJsonObject();
        String issueNumber = getJsonElementFromJsonObject(issueObject, "number").getAsString();
        String issueTitle = getJsonElementFromJsonObject(issueObject, "title").getAsString();
        EventInstanceBuilder builder = EventInstanceBuilder.newBuilder(eventRegistry);
        switch(action) {
            /*
             * TODO deal with PR comment, they are matched by this switch and should not
             */
            case GITHUB_ISSUE_COMMENT_ACTION_CREATED:
                builder.setEventDefinitionName("Issue_Comment_Created");
                break;
            case GITHUB_ISSUE_COMMENT_ACTION_EDITED:
                builder.setEventDefinitionName("Issue_Comment_Edited");
                JsonObject changesObject = getJsonElementFromJsonObject(json, "changes").getAsJsonObject();
                JsonObject changesBodyObject = changesObject.getAsJsonObject("body");
                if(nonNull(changesBodyObject)) {
                    String previousBody = getJsonElementFromJsonObject(changesBodyObject, "from").getAsString();
                    builder.setOutContextValue("old_body", previousBody);
                } else {
                    builder.setOutContextValue("old_body", body);
                }
                break;
            case GITHUB_ISSUE_COMMENT_ACTION_DELETED:
                builder.setEventDefinitionName("Issue_Comment_Deleted");
                break;
            default:
                throw new JarvisException(MessageFormat.format("Unknown Issue Comment action {0}", action));
        }
        builder.setOutContextValue("body", body);
        builder.setOutContextValue("url", url);
        builder.setOutContextValue("user", user);
        builder.setOutContextValue("issue_number", issueNumber);
        builder.setOutContextValue("issue_title", issueTitle);
        return Arrays.asList(builder.build());
    }
}

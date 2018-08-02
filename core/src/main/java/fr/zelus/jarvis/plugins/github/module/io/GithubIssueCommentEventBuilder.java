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
import static java.util.Objects.isNull;
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
        JsonObject object = getJsonElementFromJsonObject(json, "issue").getAsJsonObject();
        String number = getJsonElementFromJsonObject(object, "number").getAsString();
        String title = getJsonElementFromJsonObject(object, "title").getAsString();
        EventInstanceBuilder builder = EventInstanceBuilder.newBuilder(eventRegistry);
        switch(action) {
            /*
             * TODO deal with PR comment, they are matched by this switch and should not
             */
            case GITHUB_ISSUE_COMMENT_ACTION_CREATED:
                if(isIssueComment(json)) {
                    builder.setEventDefinitionName("Issue_Comment_Created");
                } else if(isPullRequestComment(json)) {
                    builder.setEventDefinitionName("Pull_Request_Comment_Created");
                } else {
                    throw new JarvisException("Unknown comment type");
                }
                break;
            case GITHUB_ISSUE_COMMENT_ACTION_EDITED:
                if(isIssueComment(json)) {
                    builder.setEventDefinitionName("Issue_Comment_Edited");
                } else if(isPullRequestComment(json)) {
                    builder.setEventDefinitionName("Pull_Request_Comment_Edited");
                } else {
                    throw new JarvisException("Unknown comment type");
                }

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
                if(isIssueComment(json)) {
                    builder.setEventDefinitionName("Issue_Comment_Deleted");
                } else if(isPullRequestComment(json)) {
                    builder.setEventDefinitionName("Pull_Request_Comment_Deleted");
                } else {
                    throw new JarvisException("Unknown comment type");
                }
                break;
            default:
                throw new JarvisException(MessageFormat.format("Unknown Issue Comment action {0}", action));
        }
        builder.setOutContextValue("body", body);
        builder.setOutContextValue("url", url);
        builder.setOutContextValue("user", user);
        if(isIssueComment(json)) {
            builder.setOutContextValue("issue_number", number);
            builder.setOutContextValue("issue_title", title);
        } else {
            // PR, already checked
            builder.setOutContextValue("pull_request_number", number);
            builder.setOutContextValue("pull_request_title", title);
        }

        return Arrays.asList(builder.build());
    }

    private static boolean isIssueComment(JsonObject json) {
        JsonObject issueObject = json.getAsJsonObject("issue");
        JsonObject pullRequestIssueObject = issueObject.getAsJsonObject("pull_request");
        return nonNull(issueObject) && isNull(pullRequestIssueObject);
    }

    private static boolean isPullRequestComment(JsonObject json) {
        JsonObject issueObject = json.getAsJsonObject("issue");
        JsonObject pullRequestIssueObject = issueObject.getAsJsonObject("pull_request");
        return nonNull(pullRequestIssueObject);
    }
}

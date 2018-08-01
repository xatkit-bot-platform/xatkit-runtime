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

public class GithubPullRequestEventBuilder {

    private final static String GITHUB_PULL_REQUEST_ACTION_OPENED = "opened";

    private final static String GITHUB_PULL_REQUEST_ACTION_EDITED = "edited";

    private final static String GITHUB_PULL_REQUEST_ACTION_CLOSED = "closed";

    private final static String GITHUB_PULL_REQUEST_ACTION_REOPENED = "reopened";

    private final static String GITHUB_PULL_REQUEST_ACTION_ASSIGNED = "assigned";

    private final static String GITHUB_PULL_REQUEST_ACTION_UNASSIGNED = "unassigned";

    private final static String GITHUB_PULL_REQUEST_ACTION_LABELED = "labeled";

    private final static String GITHUB_PULL_REQUEST_ACTION_UNLABELED = "unlabeled";

    private final static String GITHUB_PULL_REQUEST_ACTION_REVIEW_REQUESTED = "review_requested";

    private final static String GITHUB_PULL_REQUEST_ACTION_REVIEW_REQUEST_REMOVED = "review_request_removed";

    public static List<EventInstance> handleGithubPullRequestEvent(JsonElement parsedContent, EventDefinitionRegistry
            eventRegistry) {
        JsonObject json = parsedContent.getAsJsonObject();
        String action = getJsonElementFromJsonObject(json, "action").getAsString();
        JsonObject pullRequestObject = getJsonElementFromJsonObject(json, "pull_request").getAsJsonObject();
        String number = getJsonElementFromJsonObject(pullRequestObject, "number").getAsString();
        String title = getJsonElementFromJsonObject(pullRequestObject, "title").getAsString();
        String body = getJsonElementFromJsonObject(pullRequestObject, "body").getAsString();
        JsonObject senderObject = getJsonElementFromJsonObject(json, "sender").getAsJsonObject();
        String senderLogin = getJsonElementFromJsonObject(senderObject, "login").getAsString();
        String htmlURL = getJsonElementFromJsonObject(pullRequestObject, "html_url").getAsString();
        EventInstanceBuilder builder = EventInstanceBuilder.newBuilder(eventRegistry);
        switch(action) {
            case GITHUB_PULL_REQUEST_ACTION_OPENED:
                builder.setEventDefinitionName("Pull_Request_Opened");
                break;
            case GITHUB_PULL_REQUEST_ACTION_EDITED:
                builder.setEventDefinitionName("Pull_Request_Edited");
                JsonObject changesObject = getJsonElementFromJsonObject(json, "changes").getAsJsonObject();
                JsonObject changesTitleObject = changesObject.getAsJsonObject("title");
                if(nonNull(changesTitleObject)) {
                    String previousTitle = getJsonElementFromJsonObject(changesTitleObject, "from").getAsString();
                    builder.setOutContextValue("old_title", previousTitle);
                } else {
                    /*
                     * The title has not been updated, setting the old title with the value of the current one.
                     */
                    builder.setOutContextValue("old_title", title);
                }
                JsonObject changesBodyObject = changesObject.getAsJsonObject("body");
                if(nonNull(changesBodyObject)) {
                    String previousBody = getJsonElementFromJsonObject(changesBodyObject, "from").getAsString();
                    builder.setOutContextValue("old_body", previousBody);
                } else {
                    /*
                     * The body has not been updated, setting the old body with the value of the current one.
                     */
                    builder.setOutContextValue("old_body", body);
                }
                break;
            case GITHUB_PULL_REQUEST_ACTION_CLOSED:
                /*
                 * Find if the pull request has been merged or not.
                 */
                String merged = getJsonElementFromJsonObject(pullRequestObject, "merged").getAsString();
                if(merged.equals("true")) {
                    builder.setEventDefinitionName("Pull_Request_Closed_Merged");
                } else if(merged.equals("false")) {
                    builder.setEventDefinitionName("Pull_Request_Closed_Not_Merged");
                } else {
                    throw new JarvisException(MessageFormat.format("Unknown Pull Request merged field value {0}",
                            merged));
                }
                break;
            case GITHUB_PULL_REQUEST_ACTION_REOPENED:
                builder.setEventDefinitionName("Pull_Request_Reopened");
                break;
            case GITHUB_PULL_REQUEST_ACTION_ASSIGNED:
                builder.setEventDefinitionName("Pull_Request_Assigned");
                JsonObject assigneeObject = getJsonElementFromJsonObject(json, "assignee").getAsJsonObject();
                String assigneeLogin = getJsonElementFromJsonObject(assigneeObject, "login").getAsString();
                builder.setOutContextValue("assignee", assigneeLogin);
                break;
            case GITHUB_PULL_REQUEST_ACTION_UNASSIGNED:
                builder.setEventDefinitionName("Pull_Request_Unassigned");
                JsonObject removedAssigneeObject = getJsonElementFromJsonObject(json, "assignee").getAsJsonObject();
                String removedAssigneeLogin = getJsonElementFromJsonObject(removedAssigneeObject, "login")
                        .getAsString();
                builder.setOutContextValue("old_assignee", removedAssigneeLogin);
                break;
            default:
                throw new JarvisException(MessageFormat.format("Unknown Pull Request action {0}", action));

        }
        builder.setOutContextValue("title", title)
                .setOutContextValue("number", number)
                .setOutContextValue("body", body)
                .setOutContextValue("user", senderLogin)
                .setOutContextValue("url", htmlURL);
        return Arrays.asList(builder.build());
    }
}

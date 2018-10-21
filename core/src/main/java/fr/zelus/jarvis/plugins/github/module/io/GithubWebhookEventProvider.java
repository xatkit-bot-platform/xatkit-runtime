package fr.zelus.jarvis.plugins.github.module.io;

import com.google.gson.JsonElement;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.EventInstance;
import fr.zelus.jarvis.io.EventInstanceBuilder;
import fr.zelus.jarvis.io.JsonEventMatcher;
import fr.zelus.jarvis.io.JsonWebhookEventProvider;
import fr.zelus.jarvis.plugins.github.module.GithubModule;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.Header;

public class GithubWebhookEventProvider extends JsonWebhookEventProvider<GithubModule> {

    private final static String GITHUB_EVENT_HEADER_KEY = "X-Github-Event";

    private JsonEventMatcher matcher;

    public GithubWebhookEventProvider(GithubModule containingModule, Configuration configuration) {
        super(containingModule, configuration);
        matcher = new JsonEventMatcher(EventInstanceBuilder.newBuilder(this.jarvisCore.getEventDefinitionRegistry()),
                configuration);
        JsonEventMatcher.HeaderValue issueHeader = JsonEventMatcher.HeaderValue.of(GITHUB_EVENT_HEADER_KEY, "issues");
        matcher.addMatchableEvent(issueHeader, JsonEventMatcher.FieldValue.of("action", "opened"), "Issue_Opened");
        matcher.addMatchableEvent(issueHeader, JsonEventMatcher.FieldValue.of("action", "edited"), "Issue_Edited");
        matcher.addMatchableEvent(issueHeader, JsonEventMatcher.FieldValue.of("action", "closed"), "Issue_Closed");
        matcher.addMatchableEvent(issueHeader, JsonEventMatcher.FieldValue.of("action", "reopened"), "Issue_Reopened");
        matcher.addMatchableEvent(issueHeader, JsonEventMatcher.FieldValue.of("action", "assigned"), "Issue_Assigned");
        matcher.addMatchableEvent(issueHeader, JsonEventMatcher.FieldValue.of("action", "unassigned"),
                "Issue_Unassigned");
        matcher.addMatchableEvent(issueHeader, JsonEventMatcher.FieldValue.of("action", "labeled"), "Issue_Labeled");
        matcher.addMatchableEvent(issueHeader, JsonEventMatcher.FieldValue.of("action", "unlabeled"),
                "Issue_Unlabeled");
        matcher.addMatchableEvent(issueHeader, JsonEventMatcher.FieldValue.of("action", "milestoned"),
                "Issue_Milestoned");
        matcher.addMatchableEvent(issueHeader, JsonEventMatcher.FieldValue.of("action", "demilestoned"),
                "Issue_Demilestoned");
        JsonEventMatcher.HeaderValue issueCommentHeader = JsonEventMatcher.HeaderValue.of(GITHUB_EVENT_HEADER_KEY, "issue_comment");
        // Issue Comments
        // TODO: should we differentiate pull requests and issues?
        matcher.addMatchableEvent(issueCommentHeader, JsonEventMatcher.FieldValue.of("action", "created"),
                "Issue_Comment_Created");
        matcher.addMatchableEvent(issueCommentHeader, JsonEventMatcher.FieldValue.of("action", "edited"),
                "Issue_Comment_Edited");
        matcher.addMatchableEvent(issueCommentHeader, JsonEventMatcher.FieldValue.of("action", "deleted"),
                "Issue_Comment_Deleted");
        // Labels
        JsonEventMatcher.HeaderValue labelHeader = JsonEventMatcher.HeaderValue.of(GITHUB_EVENT_HEADER_KEY, "label");
        matcher.addMatchableEvent(labelHeader, JsonEventMatcher.FieldValue.of("action", "created"), "Label_Created");
        matcher.addMatchableEvent(labelHeader, JsonEventMatcher.FieldValue.of("action", "edited"), "Label_Edited");
        matcher.addMatchableEvent(labelHeader, JsonEventMatcher.FieldValue.of("action", "deleted"), "Label_Deleted");
        // Pull Requests
        JsonEventMatcher.HeaderValue pullRequestHeader = JsonEventMatcher.HeaderValue.of(GITHUB_EVENT_HEADER_KEY, "pull_request");
        matcher.addMatchableEvent(pullRequestHeader, JsonEventMatcher.FieldValue.of("action", "opened"),
                "Pull_Request_Opened");
        matcher.addMatchableEvent(pullRequestHeader, JsonEventMatcher.FieldValue.of("action", "edited"),
                "Pull_Request_Edited");
        // TODO: differentiate between merged and not-merged pull requests
        matcher.addMatchableEvent(pullRequestHeader, JsonEventMatcher.FieldValue.of("action", "closed"),
                "Pull_Request_Closed");
        matcher.addMatchableEvent(pullRequestHeader, JsonEventMatcher.FieldValue.of("action", "reopened"),
                "Pull_Request_Reopened");
        matcher.addMatchableEvent(pullRequestHeader, JsonEventMatcher.FieldValue.of("action", "assigned"),
                "Pull_Request_Assigned");
        matcher.addMatchableEvent(pullRequestHeader, JsonEventMatcher.FieldValue.of("action", "unassigned"),
                "Pull_Request_Unassigned");
        matcher.addMatchableEvent(pullRequestHeader, JsonEventMatcher.FieldValue.of("action", "labeled"),
                "Pull_Request_Labeled");
        matcher.addMatchableEvent(pullRequestHeader, JsonEventMatcher.FieldValue.of("action", "unlabeled"),
                "Pull_Request_Unlabeled");
        matcher.addMatchableEvent(pullRequestHeader, JsonEventMatcher.FieldValue.of("action", "review_requested"),
                "Pull_Request_Review_Requested");
        matcher.addMatchableEvent(pullRequestHeader, JsonEventMatcher.FieldValue.of("action", "review_request_removed"),
                "Pull_Request_Review_Request_Removed");
        // Push
        JsonEventMatcher.HeaderValue pushHeader = JsonEventMatcher.HeaderValue.of(GITHUB_EVENT_HEADER_KEY, "push");
        matcher.addMatchableEvent(pushHeader, JsonEventMatcher.FieldValue.EMPTY_FIELD_VALUE, "Push");
    }

    @Override
    protected void handleParsedContent(JsonElement parsedContent, Header[] headers) {
        EventInstance eventInstance = matcher.match(headers, parsedContent);
        JarvisSession jarvisSession = this.jarvisCore.getOrCreateJarvisSession("github");
        this.jarvisCore.getOrchestrationService().handleEventInstance(eventInstance, jarvisSession);
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
}

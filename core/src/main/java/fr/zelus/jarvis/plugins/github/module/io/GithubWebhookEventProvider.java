package fr.zelus.jarvis.plugins.github.module.io;

import com.google.gson.JsonElement;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.EventInstance;
import fr.zelus.jarvis.io.JsonWebhookEventProvider;
import org.apache.http.Header;

import java.util.List;

import static java.util.Objects.nonNull;

public class GithubWebhookEventProvider extends JsonWebhookEventProvider {

    private final static String GITHUB_EVENT_HEADER_KEY = "X-Github-Event";

    private final static String GITHUB_ISSUES_EVENT = "issues";

    private final static String GITHUB_ISSUE_COMMENT_EVENT = "issue_comment";

    // action on the wiki
    private final static String GITHUB_GOLLUM_EVENT = "gollum";

    private final static String GITHUB_PULL_REQUEST_EVENT = "pull_request";

    private final static String GITHUB_LABEL_EVENT = "label";

    private final static String GITHUB_FORK_EVENT = "fork";

    public GithubWebhookEventProvider(JarvisCore jarvisCore) {
        super(jarvisCore);
    }

    @Override
    protected void handleParsedContent(JsonElement parsedContent, Header[] headers) {
        String githubEvent = getHeaderValue(headers, GITHUB_EVENT_HEADER_KEY);
        if (nonNull(githubEvent)) {
            Log.info("Processing Github event {0}", githubEvent);
            try {
                // list because some builder flatten github events (e.g. GithubGollumEventBuilder)
                List<EventInstance> eventInstances;
                switch (githubEvent) {
                    case GITHUB_ISSUES_EVENT:
                        eventInstances = GithubIssueEventBuilder.handleGithubIssuesEvent(parsedContent, this
                                .jarvisCore.getEventDefinitionRegistry());
                        break;
                    case GITHUB_ISSUE_COMMENT_EVENT:
                        eventInstances = GithubIssueCommentEventBuilder.handleGithubIssueCommentEvent(parsedContent,
                                this.jarvisCore.getEventDefinitionRegistry());
                        break;
                    case GITHUB_GOLLUM_EVENT:
                        eventInstances = GithubGollumEventBuilder.handleGithubGollumEvent
                                (parsedContent, this.jarvisCore.getEventDefinitionRegistry());
                        break;
                    case GITHUB_PULL_REQUEST_EVENT:
                        eventInstances = GithubPullRequestEventBuilder.handleGithubPullRequestEvent(parsedContent,
                                this.jarvisCore.getEventDefinitionRegistry());
                        break;
                    case GITHUB_LABEL_EVENT:
                        eventInstances = GithubLabelEventBuilder.handleGithubLabelEvent(parsedContent, this
                                .jarvisCore.getEventDefinitionRegistry());
                        break;
                    case GITHUB_FORK_EVENT:
                        eventInstances = GithubForkEventBuilder.handleForkEvent(parsedContent, this.jarvisCore
                                .getEventDefinitionRegistry());
                        break;
                    default:
                        Log.warn("Unknown Github event type {0}", githubEvent);
                        return;
                }
                JarvisSession session = this.jarvisCore.getOrCreateJarvisSession("github");
                for(EventInstance eventInstance : eventInstances) {
                    this.jarvisCore.handleEvent(eventInstance, session);
                }
            } catch(JarvisException e) {
                /*
                 * An error occurred when parsing the provided content.
                 */
                Log.error(e, "An error occurred when parsing the request content");
                return;
            }
        } else {
            /*
             * The received request is not a GitHub event.
             */
            Log.info("The received request is not a Github request, skipping it");
            return;
        }
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

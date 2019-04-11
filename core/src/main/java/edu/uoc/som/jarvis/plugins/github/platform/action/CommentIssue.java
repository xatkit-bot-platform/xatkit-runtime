package edu.uoc.som.jarvis.plugins.github.platform.action;

import com.jcabi.github.Comment;
import com.jcabi.github.Issue;
import edu.uoc.som.jarvis.core.JarvisException;
import edu.uoc.som.jarvis.core.platform.action.RuntimeAction;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.plugins.github.platform.GithubPlatform;

import java.io.IOException;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * A {@link RuntimeAction} that retrieves the issue with the provided {@code number} on the given {@code repository}.
 * <p>
 * This class relies on the {@link GithubPlatform} to access the Github API and authenticate the bot.
 * <p>
 * <b>Note: </b> this class requires that its containing {@link GithubPlatform} has been loaded with valid Github
 * credentials in order to authenticate the bot and access the Github API.
 *
 * @see GithubPlatform
 */
public class CommentIssue extends RuntimeAction<GithubPlatform> {

    /**
     * The {@link Issue} to create a comment for.
     */
    private Issue issue;

    /**
     * The content of the comment to post on the {@link Issue}.
     */
    private String commentContent;


    /**
     * Constructs a new {@link CommentIssue} with the provided {@code runtimePlatform}, {@code session}, {@code user},
     * {@code repository}, and {@code issueNumber}.
     *
     * @param runtimePlatform the {@link GithubPlatform} containing this action
     * @param session         the {@link JarvisSession} associated to this action
     * @param issue           the {@link Issue} to create a comment for
     * @param commentContent  the content of the comment to post on the {@link Issue}
     * @throws NullPointerException     if the provided {@code runtimePlatform}, {@code session}, {@code issue}, or
     *                                  {@code commentContent} is {@code null}
     * @throws IllegalArgumentException if the provided {@code commentContent} is empty
     */
    public CommentIssue(GithubPlatform runtimePlatform, JarvisSession session, Issue issue, String commentContent) {
        super(runtimePlatform, session);
        checkNotNull(issue, "Cannot construct a %s action with the provided %s %s", this.getClass().getSimpleName(),
                Issue.class.getSimpleName(), issue);
        checkNotNull(commentContent, "Cannot construct a %s action with the provided comment %s",
                this.getClass().getSimpleName(), commentContent);
        checkArgument(!commentContent.isEmpty(), "Cannot construct a %s action with the provided comment, expected a " +
                "non-empty comment, found %s", commentContent);
        this.issue = issue;
        this.commentContent = commentContent;
    }

    /**
     * Retrieve the issue on the given {@code repository} with the provided {@code issueNumber}.
     * <p>
     * This method relies on the containing {@link GithubPlatform} to access the Github API, and will throw a
     * {@link JarvisException} if the Jarvis {@link org.apache.commons.configuration2.Configuration} does not define
     * valid Github authentication credentials.
     *
     * @return the retrieved {@link Issue}
     * @throws JarvisException if the {@link GithubPlatform} does not hold a valid Github API client (i.e. if the
     *                         Jarvis {@link org.apache.commons.configuration2.Configuration} does not define valid
     *                         Github authentication credentials)
     * @see GithubPlatform#getGithubClient()
     */
    @Override
    protected Object compute() {
        try {
            Comment comment = issue.comments().post(commentContent);
            return comment;
        } catch (IOException e) {
            throw new JarvisException("Cannot retrieve the Github issue, see attached exception", e);
        }
    }
}

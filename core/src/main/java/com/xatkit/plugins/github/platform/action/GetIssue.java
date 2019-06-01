package com.xatkit.plugins.github.platform.action;

import com.jcabi.github.Coordinates;
import com.jcabi.github.Issue;
import com.jcabi.github.Repo;
import com.xatkit.plugins.github.platform.GithubPlatform;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.XatkitSession;
import fr.inria.atlanmod.commons.log.Log;

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
public class GetIssue extends RuntimeAction<GithubPlatform> {

    /**
     * The Github user managing the repository to open an issue in.
     */
    private String user;

    /**
     * The Github repository to open an issue in.
     */
    private String repository;

    /**
     * The number of the issue to retrieve.
     */
    private int issueNumber;

    /**
     * Constructs a new {@link GetIssue} with the provided {@code runtimePlatform}, {@code session}, {@code user},
     * {@code repository}, and {@code issueNumber}.
     *
     * @param runtimePlatform the {@link GithubPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param user            the Github user managing the repository to retrieve the issue from
     * @param repository      the Github repository to retrieve the issue from
     * @param issueNumber     the number of the issue to retrieve
     * @throws NullPointerException     if the provided {@code runtimePlatform}, {@code session}, {@code user}, {@code
     *                                  repository}, or {@code issueNumber} is {@code null}
     * @throws IllegalArgumentException if the provided {@code issueNumber} is not lesser of equal to {@code 0}
     */
    public GetIssue(GithubPlatform runtimePlatform, XatkitSession session, String user, String repository, String
            issueNumber) {
        super(runtimePlatform, session);
        checkNotNull(user, "Cannot construct a %s action with the provided Github user %s", this.getClass()
                .getSimpleName(), user);
        checkNotNull(repository, "Cannot construct a %s action with the provided Github repository %s", this.getClass
                ().getSimpleName(), repository);
        checkNotNull(issueNumber, "Cannot construct a %s action with the provided issue number %s", this.getClass()
                .getSimpleName(), issueNumber);
        this.user = user;
        this.repository = repository;
        this.issueNumber = Integer.parseInt(issueNumber);
        checkArgument(this.issueNumber >= 1, "Cannot construct a %s action with the provided issue number, expected a" +
                " positive integer (>= 1), found %s", this.getClass().getSimpleName(), this.issueNumber);
    }

    /**
     * Retrieve the issue on the given {@code repository} with the provided {@code issueNumber}.
     * <p>
     * This method relies on the containing {@link GithubPlatform} to access the Github API, and will throw a
     * {@link XatkitException} if the Xatkit {@link org.apache.commons.configuration2.Configuration} does not define
     * valid Github authentication credentials.
     *
     * @return the retrieved {@link Issue}
     * @throws XatkitException if the {@link GithubPlatform} does not hold a valid Github API client (i.e. if the
     *                         Xatkit {@link org.apache.commons.configuration2.Configuration} does not define valid
     *                         Github authentication credentials)
     * @see GithubPlatform#getGithubClient()
     */
    @Override
    protected Object compute() {
        Repo githubRepo = this.runtimePlatform.getGithubClient().repos().get(new Coordinates.Simple(user, repository));
        Issue issue = githubRepo.issues().get(issueNumber);
        Log.info("Found issue {0}", issue.number());
        Issue.Smart smartIssue = new Issue.Smart(issue);
        try {
            Log.info("Issue title: {0}", smartIssue.title());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return issue;
    }
}

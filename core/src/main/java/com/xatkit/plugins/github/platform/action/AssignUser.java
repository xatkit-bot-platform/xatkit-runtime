package com.xatkit.plugins.github.platform.action;

import com.jcabi.github.Issue;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.github.platform.GithubPlatform;

import java.io.IOException;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

/**
 * A {@link RuntimeAction} that assigns the provided {@code username} to the given {@code issue}.
 * <p>
 * <b>Note:</b> this class requires that its containing {@link GithubPlatform} has been loaded with a valid Github
 * credentials in order to authenticate the bot and access the Github API.
 */
public class AssignUser extends RuntimeAction<GithubPlatform> {

    /**
     * The {@link Issue} to assign a user to.
     */
    private Issue issue;

    /**
     * The username of the Github user to assign to the {@link Issue}.
     */
    private String username;

    /**
     * Constructs a new {@link AssignUser} with the provided {@code runtimePlatform}, {@code session}, {@code issue},
     * and {@code username}.
     *
     * @param runtimePlatform the {@link GithubPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param issue           the {@link Issue} to assign a user to
     * @param username        the username of the Github user to assign to the provided {@code issue}
     * @throws NullPointerException     if the provided {@code runtimePlatform}, {@code session}, or {@code issue} is
     *                                  {@code null}
     * @throws IllegalArgumentException if the provided {@code username} is {@code null} or empty
     */
    public AssignUser(GithubPlatform runtimePlatform, XatkitSession session, Issue issue, String username) {
        super(runtimePlatform, session);
        checkNotNull("Cannot construct a %s action with the provided %s %s", this.getClass().getSimpleName(),
                Issue.class.getSimpleName(), issue);
        checkArgument(!isNull(username) && !username.isEmpty(), "Cannot construct a %s action with the provided " +
            "username, expected a " +
                "non-empty username, found %s", this.getClass().getSimpleName(), username);
        this.issue = issue;
        this.username = username;
    }

    /**
     * Assigns the provided {@code username} to the given {@link Issue}.
     *
     * @return the assigned username
     * @throws XatkitException if an error occurred when assigning the {@code username} to the provided {@code issue}.
     */
    @Override
    protected Object compute() {
        try {
            new Issue.Smart(issue).assign(username);
            return username;
        } catch (IOException e) {
            throw new XatkitException("Cannot assign a username to the provided issue, see attached exception", e);
        }
    }
}

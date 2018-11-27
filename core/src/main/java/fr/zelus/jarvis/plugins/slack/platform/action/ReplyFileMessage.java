package fr.zelus.jarvis.plugins.slack.platform.action;

import fr.zelus.jarvis.core.platform.action.RuntimeAction;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.plugins.slack.platform.SlackPlatform;

import java.io.File;

/**
 * A {@link RuntimeAction} that replies to a message by uploading a {@code file} using the input
 * Slack channel.
 * <p>
 * This action relies on the provided {@link JarvisSession} to retrieve the Slack {@code channel} associated to the
 * user input.
 * <p>
 * This class relies on the {@link SlackPlatform}'s {@link com.github.seratch.jslack.Slack} client and Slack bot API
 * token to connect to the Slack API and upload the reply file.
 * <p>
 * <b>Note:</b> this class requires that its containing {@link SlackPlatform} has been loaded with a valid Slack bot
 * API token in order to authenticate the bot an upload messages.
 *
 * @see PostFileMessage
 */
public class ReplyFileMessage extends PostFileMessage {

    /**
     * Constructs a new {@link ReplyFileMessage} with the provided {@code runtimePlatform}, {@code session}, {@code
     * message}, and {@code file}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this action
     * @param session          the {@link JarvisSession} associated to this action
     * @param message          the message to associated to the uploaded {@link File}
     * @param file             the {@link File} to upload
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is
     *                                  {@code null}
     * @throws IllegalArgumentException if the provided {@code message} is {@code null} or empty, or if the provided
     *                                  {@code file} is {@code null} or does not exist
     * @see Reply#getChannel(JarvisContext)
     * @see PostFileMessage#PostFileMessage(SlackPlatform, JarvisSession, String, File, String)
     */
    public ReplyFileMessage(SlackPlatform runtimePlatform, JarvisSession session, String message, File file) {
        super(runtimePlatform, session, message, file, Reply.getChannel(session.getJarvisContext()));
    }
}

package fr.zelus.jarvis.plugins.slack.module.action;

import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.plugins.slack.module.SlackModule;

import java.io.File;

/**
 * A {@link fr.zelus.jarvis.core.JarvisAction} that replies to a message by uploading a {@code file} using the input
 * Slack channel.
 * <p>
 * This action relies on the provided {@link JarvisSession} to retrieve the Slack {@code channel} associated to the
 * user input.
 * <p>
 * This class relies on the {@link SlackModule}'s {@link com.github.seratch.jslack.Slack} client and Slack bot API
 * token to connect to the Slack API and upload the reply file.
 * <p>
 * <b>Note:</b> this class requires that its containing {@link SlackModule} has been loaded with a valid Slack bot
 * API token in order to authenticate the bot an upload messages.
 *
 * @see PostFileMessage
 */
public class ReplyFileMessage extends PostFileMessage {

    /**
     * Constructs a new {@link ReplyFileMessage} with the provided {@code containingModule}, {@code session}, {@code
     * message}, and {@code file}.
     *
     * @param containingModule the {@link SlackModule} containing this action
     * @param session          the {@link JarvisSession} associated to this action
     * @param message          the message to associated to the uploaded {@link File}
     * @param file             the {@link File} to upload
     * @throws NullPointerException     if the provided {@code containingModule} or {@code session} is
     *                                  {@code null}
     * @throws IllegalArgumentException if the provided {@code message} is {@code null} or empty, or if the provided
     *                                  {@code file} is {@code null} or does not exist
     * @see Reply#getChannel(JarvisContext)
     * @see PostFileMessage#PostFileMessage(SlackModule, JarvisSession, String, File, String)
     */
    public ReplyFileMessage(SlackModule containingModule, JarvisSession session, String message, File file) {
        super(containingModule, session, message, file, Reply.getChannel(session.getJarvisContext()));
    }
}

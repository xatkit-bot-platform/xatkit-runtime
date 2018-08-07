package fr.zelus.jarvis.plugins.slack.module.action;

import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.files.FilesUploadRequest;
import com.github.seratch.jslack.api.methods.response.files.FilesUploadResponse;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.plugins.slack.module.SlackModule;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * A {@link fr.zelus.jarvis.core.JarvisAction} that uploads a {@code file} with an associated {@code message} to a
 * given Slack {@code channel}.
 * <p>
 * This class relies on the {@link SlackModule}'s {@link com.github.seratch.jslack.Slack} client and Slack bot API
 * token to connect to the Slack API and post messages.
 * <p>
 * <b>Note:</b> this class requires that its containing {@link SlackModule} has been loaded with a valid Slack bot
 * API token in order to authenticate the bot and post messages.
 *
 * @see PostMessage
 */
public class PostFileMessage extends PostMessage {

    /**
     * The {@link File} to upload to the given Slack {@code channel}.
     * <p>
     * If this field is {@code null} the class should hold a valid {@code content} value.
     */
    private File file;

    /**
     * The title of the file to upload to the given Slack {@code channel}.
     */
    private String title;

    /**
     * The content of the file to upload to the given Slack {@code channel}.
     * <p>
     * If this field is {@code null} the class should hold a valid {@code file} value.
     */
    private String content;

    /**
     * Constructs a new {@link PostFileMessage} with the provided {@code containingModule}, {@code session}, {@code
     * message}, {@code file}, and {@code channel}.
     * <p>
     * This constructor builds a {@link PostFileMessage} action that uploads the provided {@code file} to the given
     * Slack {@code channel}. To upload a {@link String} as a file see
     * {@link #PostFileMessage(SlackModule, JarvisSession, String, String, String, String)}.
     *
     * @param containingModule the {@link SlackModule} containing this action
     * @param session          the {@link JarvisSession} associated to this action
     * @param message          the message to associate to the uploaded {@link File}
     * @param file             the file to upload
     * @param channel          the Slack channel to upload the {@link File} to
     * @throws NullPointerException     if the provided {@code containingModule} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code message} or {@code channel} is {@code null} or empty,
     *                                  or if the provided {@code file} is {@code null} or does not exist
     * @see #PostFileMessage(SlackModule, JarvisSession, String, String, String, String)
     */
    public PostFileMessage(SlackModule containingModule, JarvisSession session, String message, File file, String
            channel) {
        super(containingModule, session, message, channel);
        checkArgument(nonNull(file) && file.exists(), "Cannot construct a %s action with the provided file %s, " +
                "expected a non-null and existing file", this.getClass().getSimpleName(), file);
        this.file = file;
    }

    /**
     * Constructs a new {@link PostFileMessage} with the provided {@code containingModule}, {@code session}, {@code
     * title}, {@code message}, {@code content}, and {@code channel}.
     * <p>
     * This constructor builds a {@link PostFileMessage} action that uploads the provided {@code content} as a file
     * to the given Slack {@code channel}. To upload an existing {@link File} see
     * {@link #PostFileMessage(SlackModule, JarvisSession, String, File, String)}.
     *
     * @param containingModule the {@link SlackModule} containing this action
     * @param session          the {@link JarvisSession} associated to this action
     * @param title            the title of the file to upload
     * @param message          the message to associate to the uploaded {@link File}
     * @param content          the content of the file to upload
     * @param channel          the Slack channel to upload the {@link File} to
     * @throws NullPointerException     if the provided {@code containingModule} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code title}, {@code message}, {@code content}, or {@code
     *                                  channel} is {@code null} or empty.
     * @see #PostFileMessage(SlackModule, JarvisSession, String, File, String)
     */
    public PostFileMessage(SlackModule containingModule, JarvisSession session, String title, String message, String
            content, String channel) {
        super(containingModule, session, message, channel);
        checkArgument(nonNull(title) && !title.isEmpty(), "Cannot construct a %s action with the provided title %s, " +
                "expected a non-null and not empty String", this.getClass().getSimpleName(), title);
        checkArgument(nonNull(content) && !content.isEmpty(), "Cannot construct a %s action with the provided content" +
                " %s, expected a non-null and not empty String", this.getClass().getSimpleName(), content);
        this.title = title;
        this.content = content;
    }

    /**
     * Uploads the provided {@code file} and post it with the associated {@code message} to the given {@code channel}.
     * <p>
     * This method relies on the containing {@link SlackModule}'s Slack bot API token to authenticate the bot and
     * upload the {@code file} to the given {@code channel}.
     *
     * @return {@code null}
     */
    @Override
    public Object call() {
        FilesUploadRequest.FilesUploadRequestBuilder builder = FilesUploadRequest.builder();
        builder.token(module.getSlackToken())
                .channels(Arrays.asList(channel))
                .initialComment(message);
        if (nonNull(file)) {
            /*
             * Uploading an existing file
             */
            builder.title(file.getName())
                    .file(file)
                    .filename(file.getName());
        } else {
            /*
             * Uploading a String content as a file
             */
            builder.title(title)
                    .content(content)
                    .filename(title);
        }
        FilesUploadRequest request = builder.build();
        try {
            FilesUploadResponse response = module.getSlack().methods().filesUpload(request);
            if (response.isOk()) {
                Log.trace("Request {0} successfully sent to the Slack API", request);
            } else {
                Log.error("An error occurred when processing the request {0}: received response {1}", request,
                        response);
            }
        } catch (IOException | SlackApiException e) {
            throw new JarvisException(MessageFormat.format("Cannot send the message {0} to the Slack API", request), e);
        }
        return null;
    }
}

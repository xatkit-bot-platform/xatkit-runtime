package com.xatkit.plugins.slack.platform.action;

import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.response.chat.ChatPostMessageResponse;
import com.github.seratch.jslack.api.model.Attachment;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.platform.action.RuntimeArtifactAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.slack.platform.SlackPlatform;
import fr.inria.atlanmod.commons.log.Log;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * A {@link RuntimeAction} that posts {@code Attachment} to a given Slack {@code channel}.
 * <p>
 * This class relies on the {@link SlackPlatform}'s {@link com.github.seratch.jslack.Slack} client and Slack bot API
 * token to connect to the Slack API and post messages.
 * <p>
 * <b>Note:</b> this class requires that its containing {@link SlackPlatform} has been loaded with a valid Slack bot
 * API token in order to authenticate the bot and post messages.
 */
public class PostAttachmentsMessage extends RuntimeArtifactAction<SlackPlatform> {

    /**
     * The Slack channel to post the attachments to.
     */
    protected String channel;

    /**
     * The attachments to post to Slack channel.
     */
    protected List<Attachment> attachments;

    /**
     * Constructs a new {@link ReplyAttachmentsMessage} with the provided {@code runtimePlatform}, {@code session},
     * {@code attachments} and {@code channel}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param attachments     the {@link Attachment} list to post
     * @param channel         the Slack channel to post the attachments to
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the text parameter of each entry of the provided {@code attachments} list
     *                                  is {@code null} or empty
     * @see PostMessage#PostMessage(SlackPlatform, XatkitSession, String, String)
     */
    public PostAttachmentsMessage(SlackPlatform runtimePlatform, XatkitSession session, List<Attachment> attachments,
                                  String channel) {
        super(runtimePlatform, session);

        checkArgument(nonNull(channel) && !channel.isEmpty(), "Cannot construct a %s action with the provided channel" +
                " %s, expected a non-null and not empty String", this.getClass().getSimpleName(), channel);
        this.channel = channel;

        for (Attachment attch : attachments) {
            checkArgument(nonNull(attch.getText()) && !attch.getText().isEmpty(), "Cannot construct a %s action with " +
                            "the provided text %s, expected a non-null and not empty String for the attachment text",
                    this.getClass().getSimpleName(), attch.getText());
        }
        this.attachments = attachments;
    }

    /**
     * Constructs a new {@link ReplyAttachmentsMessage} with the provided {@code runtimePlatform}, {@code session},
     * {@code pretext}, {@code title}, {@code text}, {@code attchColor}, {@code timestamp} and {@code channel}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param pretext         the pretext of the {@link Attachment} to post
     * @param title           the title of the {@link Attachment} to post
     * @param text            the text of the {@link Attachment} to post
     * @param attchColor      the color of the {@link Attachment} to post in HEX format
     * @param timestamp       the timestamp of the {@link Attachment} to post in epoch format
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code text} list is {@code null} or empty
     * @see PostMessage#PostMessage(SlackPlatform, XatkitSession, String, String)
     */
    public PostAttachmentsMessage(SlackPlatform runtimePlatform, XatkitSession session, String pretext, String title,
                                  String text, String attchColor, String timestamp, String channel) {
        super(runtimePlatform, session);

        checkArgument(nonNull(channel) && !channel.isEmpty(), "Cannot construct a %s action with the provided channel" +
                " %s, expected a non-null and not empty String", this.getClass().getSimpleName(), channel);
        this.channel = channel;

        checkArgument(nonNull(text) && !text.isEmpty(), "Cannot construct a %s action with the provided text %s, " +
                        "expected a non-null and not empty String for the attachment text",
                this.getClass().getSimpleName(), text);

        Attachment attachment = createAttachment(pretext, title, text, attchColor, timestamp);
        this.attachments = new ArrayList<>();
        this.attachments.add(attachment);
    }

    /**
     * Constructs a new {@link ReplyAttachmentsMessage} with the provided {@code runtimePlatform}, {@code session},
     * {@code pretext}, {@code title}, {@code text}, {@code attchColor}, and {@code channel}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param pretext         the pretext of the {@link Attachment} to post
     * @param title           the title of the {@link Attachment} to post
     * @param text            the text of the {@link Attachment} to post
     * @param attchColor      the color of the {@link Attachment} to post in HEX format
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code text} list is {@code null} or empty
     * @see PostMessage#PostMessage(SlackPlatform, XatkitSession, String, String)
     */
    public PostAttachmentsMessage(SlackPlatform runtimePlatform, XatkitSession session, String pretext, String title,
                                  String text, String attchColor, String channel) {
        super(runtimePlatform, session);

        checkArgument(nonNull(channel) && !channel.isEmpty(), "Cannot construct a %s action with the provided channel" +
                " %s, expected a non-null and not empty String", this.getClass().getSimpleName(), channel);
        this.channel = channel;

        checkArgument(nonNull(text) && !text.isEmpty(), "Cannot construct a %s action with the provided text %s, " +
                        "expected a non-null and not empty String for the attachment text",
                this.getClass().getSimpleName(), text);

        String timestamp = String.valueOf(Calendar.getInstance().getTime().getTime() / 1000);
        Attachment attachment = createAttachment(pretext, title, text, attchColor, timestamp);
        this.attachments = new ArrayList<>();
        this.attachments.add(attachment);
    }

    /**
     * Posts the provided {@code attachments} to the given {@code channel}.
     * <p>
     * This method relies on the containing {@link SlackPlatform}'s Slack bot API token to authenticate the bot and
     * post the {@code attachments} to the given {@code channel}.
     *
     * @return {@code null}
     * @throws IOException     if an I/O error occurred when sending the message
     * @throws XatkitException if the provided token does not authenticate the bot
     */
    @Override
    public Object compute() throws IOException {
        ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                .token(runtimePlatform.getSlackToken())
                .channel(channel)
                .attachments(attachments)
                .unfurlLinks(true)
                .unfurlMedia(true)
                .build();
        try {
            ChatPostMessageResponse response = runtimePlatform.getSlack().methods().chatPostMessage(request);
            if (response.isOk()) {
                Log.trace("Request {0} successfully sent to the Slack API", request);
            } else {
                Log.error("An error occurred when processing the request {0}: received response {1}", request,
                        response);
            }
        } catch (SlackApiException e) {
            throw new XatkitException(MessageFormat.format("Cannot send the message {0} to the Slack API", request), e);
        }
        return null;
    }

    /**
     * Constructs a new {@link Attachment} with the provided {@code pretext}, {@code title}, {@code text}, {@code
     * attchColor}, {@code timestamp}.
     *
     * @param pretext    the pretext of the {@link Attachment} to post
     * @param title      the title of the {@link Attachment} to post
     * @param text       the text of the {@link Attachment} to post
     * @param color the color of the {@link Attachment} to post in HEX format
     * @param timestamp  the timestamp of the {@link Attachment} to post in epoch
     *                   format
     */
    private Attachment createAttachment(String pretext, String title, String text, String color, String timestamp) {
        Attachment.AttachmentBuilder attachmentBuilder = Attachment.builder();
        attachmentBuilder.pretext(pretext);
        attachmentBuilder.title(title);
        attachmentBuilder.text(text);
        attachmentBuilder.color(color);
        attachmentBuilder.ts(timestamp);

        return attachmentBuilder.build();
    }

    @Override
    protected XatkitSession getClientSession() {
        return this.runtimePlatform.createSessionFromChannel(channel);
    }
}

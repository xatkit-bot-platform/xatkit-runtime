package edu.uoc.som.jarvis.plugins.slack.platform.action;

import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.response.chat.ChatPostMessageResponse;
import com.github.seratch.jslack.api.model.Attachment;

import edu.uoc.som.jarvis.core.JarvisException;
import edu.uoc.som.jarvis.core.platform.action.RuntimeAction;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.plugins.slack.platform.SlackPlatform;
import fr.inria.atlanmod.commons.log.Log;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link RuntimeAction} that posts a {@code message} to a given Slack
 * {@code channel}.
 * <p>
 * This class relies on the {@link SlackPlatform}'s
 * {@link com.github.seratch.jslack.Slack} client and Slack bot API token to
 * connect to the Slack API and post messages.
 * <p>
 * <b>Note:</b> this class requires that its containing {@link SlackPlatform}
 * has been loaded with a valid Slack bot API token in order to authenticate the
 * bot and post messages.
 */
public class PostAttachmentsMessage extends PostMessage {

	protected List<Attachment> attachments;

	/**
	 * Constructs a new {@link ReplyAttachmentsMessage} with the provided
	 * {@code runtimePlatform}, {@code session}, {@code attachments} and
	 * {@code channel}.
	 *
	 * @param runtimePlatform the {@link SlackPlatform} containing this action
	 * @param session         the {@link JarvisSession} associated to this action
	 * @param attachments     the {@link Attachment} list to post
	 * @param channel         the Slack channel to post the attachments to
	 * @throws NullPointerException     if the provided {@code runtimePlatform} or
	 *                                  {@code session} is {@code null}
	 * @throws IllegalArgumentException if the text parameter of each entry of the
	 *                                  provided {@code attachments} list is
	 *                                  {@code null} or empty
	 * @see #getChannel(RuntimeContexts)
	 * @see PostMessage#PostMessage(SlackPlatform, JarvisSession, String, String)
	 */
	public PostAttachmentsMessage(SlackPlatform runtimePlatform, JarvisSession session, List<Attachment> attachments,
			String channel) {
		/**
		 * TODO: add method to {@link RuntimeMessageAction} to allow being called
		 * without defining a message parameter
		 */
		super(runtimePlatform, session, "no_messge_required", channel);

		for (Attachment attch : attachments) {
			checkArgument(nonNull(attch.getText()) && !attch.getText().isEmpty(),
					"Cannot construct a %s action with the provided content"
							+ " %s, expected a non-null and not empty String for the attachment text",
					this.getClass().getSimpleName(), attch.getText());
		}

		this.attachments = attachments;
	}

	/**
	 * Constructs a new {@link ReplyAttachmentsMessage} with the provided
	 * {@code runtimePlatform}, {@code session}, {@code pretext}, {@code title},
	 * {@code text}, {@code attchColor}, {@code timestamp} and {@code channel}.
	 *
	 * @param runtimePlatform the {@link SlackPlatform} containing this action
	 * @param session         the {@link JarvisSession} associated to this action
	 * @param pretext         the pretext of the {@link Attachment} to post
	 * @param title           the title of the {@link Attachment} to post
	 * @param text            the text of the {@link Attachment} to post
	 * @param attchColor      the color of the {@link Attachment} to post in HEX
	 *                        format
	 * @param timestamp       the timestamp of the {@link Attachment} to post in
	 *                        epoch format
	 * @throws NullPointerException     if the provided {@code runtimePlatform} or
	 *                                  {@code session} is {@code null}
	 * @throws IllegalArgumentException if the provided {@code text} list is
	 *                                  {@code null} or empty
	 * @see #getChannel(RuntimeContexts)
	 * @see PostMessage#PostMessage(SlackPlatform, JarvisSession, String, String)
	 */
	public PostAttachmentsMessage(SlackPlatform runtimePlatform, JarvisSession session, String pretext, String title,
			String text, String attchColor, String timestamp, String channel) {
		/**
		 * TODO: add method to {@link RuntimeMessageAction} to allow being called
		 * without defining a message parameter
		 */
		super(runtimePlatform, session, "no_messge_required", channel);

		checkArgument(nonNull(text) && !text.isEmpty(),
				"Cannot construct a %s action with the provided content"
						+ " %s, expected a non-null and not empty String for the attachment text",
				this.getClass().getSimpleName(), text);

		Attachment attachment = createAttachment(pretext, title, text, attchColor, timestamp);
		this.attachments = new ArrayList<>();
		this.attachments.add(attachment);
	}

	/**
	 * Posts the provided {@code attachments} to the given {@code channel}.
	 * <p>
	 * This method relies on the containing {@link SlackPlatform}'s Slack bot API
	 * token to authenticate the bot and post the {@code attachments} to the given
	 * {@code channel}.
	 *
	 * @return {@code null}
	 * @throws IOException     if an I/O error occurred when sending the message
	 * @throws JarvisException if the provided token does not authenticate the bot
	 */
	@Override
	public Object compute() throws IOException {
		ChatPostMessageRequest request = ChatPostMessageRequest.builder().token(runtimePlatform.getSlackToken())
				.channel(channel).attachments(attachments).unfurlLinks(true).unfurlMedia(true).build();
		try {
			ChatPostMessageResponse response = runtimePlatform.getSlack().methods().chatPostMessage(request);
			if (response.isOk()) {
				Log.trace("Request {0} successfully sent to the Slack API", request);
			} else {
				Log.error("An error occurred when processing the request {0}: received response {1}", request,
						response);
			}
		} catch (SlackApiException e) {
			throw new JarvisException(MessageFormat.format("Cannot send the message {0} to the Slack API", request), e);
		}
		return null;
	}

	/**
	 * Constructs a new {@link Attachment} with the provided {@code pretext},
	 * {@code title}, {@code text}, {@code attchColor}, {@code timestamp}.
	 *
	 * @param pretext    the pretext of the {@link Attachment} to post
	 * @param title      the title of the {@link Attachment} to post
	 * @param text       the text of the {@link Attachment} to post
	 * @param attchColor the color of the {@link Attachment} to post in HEX format
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
}

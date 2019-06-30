package com.xatkit.plugins.slack.platform.action;

import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.core.session.RuntimeContexts;
import com.xatkit.plugins.slack.platform.SlackPlatform;

import java.util.List;

import com.github.seratch.jslack.api.model.Attachment;

/**
 * A {@link RuntimeAction} that replies to a message by uploading
 * {@code Attachment}} using the input Slack channel.
 * <p>
 * This action relies on the provided {@link XatkitSession} to retrieve the
 * Slack {@code channel} associated to the user input.
 * <p>
 * This class relies on the {@link SlackPlatform}'s
 * {@link com.github.seratch.jslack.Slack} client and Slack bot API token to
 * connect to the Slack API and post the reply attachments.
 * <p>
 * <b>Note:</b> this class requires that its containing {@link SlackPlatform}
 * has been loaded with a valid Slack bot API token in order to authenticate the
 * bot and post messages.
 *
 * @see PostMessage
 */
public class ReplyAttachmentsMessage extends PostAttachmentsMessage {

	/**
	 * Constructs a new {@link ReplyAttachmentsMessage} with the provided
	 * {@code runtimePlatform}, {@code session}, and {@code attachments}.
	 *
	 * @param runtimePlatform the {@link SlackPlatform} containing this action
	 * @param session         the {@link XatkitSession} associated to this action
	 * @param attachments     the {@link Attachment} list to post
	 * @throws NullPointerException     if the provided {@code runtimePlatform} or
	 *                                  {@code session} is {@code null}
	 * @throws IllegalArgumentException if the text parameter of each entry of the
	 *                                  provided {@code attachments} list is
	 *                                  {@code null} or empty
	 * @see #getChannel(RuntimeContexts)
	 * @see PostMessage#PostMessage(SlackPlatform, XatkitSession, String, String)
	 */
	public ReplyAttachmentsMessage(SlackPlatform runtimePlatform, XatkitSession session, List<Attachment> attachments) {
		super(runtimePlatform, session, attachments, Reply.getChannel(session.getRuntimeContexts()));
	}

	/**
	 * Constructs a new {@link ReplyAttachmentsMessage} with the provided
	 * {@code runtimePlatform}, {@code session}, {@code pretext}, {@code title},
	 * {@code text}, {@code attchColor}, and {@code timestamp}.
	 *
	 * @param runtimePlatform the {@link SlackPlatform} containing this action
	 * @param session         the {@link XatkitSession} associated to this action
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
	 * @see PostMessage#PostMessage(SlackPlatform, XatkitSession, String, String)
	 */
	public ReplyAttachmentsMessage(SlackPlatform runtimePlatform, XatkitSession session, String pretext, String title,
			String text, String attchColor, String timestamp) {
		super(runtimePlatform, session, pretext, title, text, attchColor, timestamp,
				Reply.getChannel(session.getRuntimeContexts()));
	}

	/**
	 * Constructs a new {@link ReplyAttachmentsMessage} with the provided
	 * {@code runtimePlatform}, {@code session}, {@code pretext}, {@code title},
	 * {@code text}, and {@code attchColor}.
	 *
	 * @param runtimePlatform the {@link SlackPlatform} containing this action
	 * @param session         the {@link XatkitSession} associated to this action
	 * @param pretext         the pretext of the {@link Attachment} to post
	 * @param title           the title of the {@link Attachment} to post
	 * @param text            the text of the {@link Attachment} to post
	 * @param attchColor      the color of the {@link Attachment} to post in HEX
	 *                        format
	 * @param timestamp       the timestamp of the {@link Attachment} to post will
	 *                        be set to the actual time
	 * @throws NullPointerException     if the provided {@code runtimePlatform} or
	 *                                  {@code session} is {@code null}
	 * @throws IllegalArgumentException if the provided {@code text} list is
	 *                                  {@code null} or empty
	 * @see #getChannel(RuntimeContexts)
	 * @see PostMessage#PostMessage(SlackPlatform, XatkitSession, String, String)
	 */
	public ReplyAttachmentsMessage(SlackPlatform runtimePlatform, XatkitSession session, String pretext, String title,
			String text, String attchColor) {
		super(runtimePlatform, session, pretext, title, text, attchColor,
				Reply.getChannel(session.getRuntimeContexts()));
	}
}

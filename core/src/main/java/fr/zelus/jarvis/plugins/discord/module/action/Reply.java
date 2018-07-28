package fr.zelus.jarvis.plugins.discord.module.action;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.plugins.discord.JarvisDiscordUtils;
import fr.zelus.jarvis.plugins.discord.module.DiscordModule;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * A {@link fr.zelus.jarvis.core.JarvisAction} that replies to a message using the input Discord channel.
 * <p>
 * This action relies on the provided {@link JarvisSession} to retrieve the Discord {@code channel} associated to the
 * user input.
 * <p>
 * This class relies on the {@link DiscordModule}'s {@link net.dv8tion.jda.core.JDA} client to connect to the Discord
 * API and post the reply message.
 * <p>
 * <b>Note:</b> this class requires that its containing {@link DiscordModule} has been loaded with a valid Discord
 * bot token in order to authenticate the bot and post messages.
 *
 * @see PostMessage
 */
public class Reply extends PostMessage {

    /**
     * Returns the channel associated to the user input.
     * <p>
     * This method searches in the provided {@link JarvisContext} for the values stored with the key
     * {@link JarvisDiscordUtils#DISCORD_CONTEXT_KEY}.{@link JarvisDiscordUtils#DISCORD_CHANNEL_CONTEXT_KEY}. Note
     * that if the provided {@link JarvisContext} does not contain the requested value a {@link NullPointerException}
     * is thrown.
     *
     * @param context the {@link JarvisContext} to retrieve the Discord channel from
     * @return the Discord channel associated to the user input
     * @throws NullPointerException     if the provided {@code context} is {@code null}, or if it does not contain the
     *                                  channel information
     * @throws IllegalArgumentException if the retrieved channel is not a {@link String}
     * @see JarvisDiscordUtils
     */
    private static String getChannel(JarvisContext context) {
        Object channelValue = context.getContextValue(JarvisDiscordUtils.DISCORD_CONTEXT_KEY, JarvisDiscordUtils
                .DISCORD_CHANNEL_CONTEXT_KEY);
        checkNotNull(channelValue, "Cannot retrieve the Discord channel from the context");
        checkArgument(channelValue instanceof String, "Invalid Discord channel type, expected %s, found %s", String
                .class.getSimpleName(), channelValue.getClass().getSimpleName());
        Log.info("Found channel {0}", channelValue);
        return (String) channelValue;
    }

    /**
     * Constructs a new {@link Reply} with the provided {@code containingModule}, {@code session}, and {@code message}.
     *
     * @param containingModule the {@link DiscordModule} containing this action
     * @param session          the {@link JarvisSession} associated to this action
     * @param message          the message to post
     * @throws NullPointerException     if the provided {@code containingModule} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code message} is {@code null} or empty
     */
    public Reply(DiscordModule containingModule, JarvisSession session, String message) {
        super(containingModule, session, message, getChannel(session.getJarvisContext()));
    }
}

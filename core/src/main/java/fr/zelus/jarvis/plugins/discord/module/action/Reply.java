package fr.zelus.jarvis.plugins.discord.module.action;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.plugins.discord.JarvisDiscordUtils;
import fr.zelus.jarvis.plugins.discord.module.DiscordModule;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

public class Reply extends PostMessage {

    private static String getChannel(JarvisContext context) {
        Object channelValue = context.getContextValue(JarvisDiscordUtils.DISCORD_CONTEXT_KEY, JarvisDiscordUtils
                .DISCORD_CHANNEL_CONTEXT_KEY);
        checkNotNull(channelValue, "Cannot retrieve the Discord channel from the context");
        checkArgument(channelValue instanceof String, "Invalid Discord channel type, expected %s, found %s", String
                .class.getSimpleName(), channelValue.getClass().getSimpleName());
        Log.info("Found channel {0}", channelValue);
        return (String) channelValue;
    }

    public Reply(DiscordModule containingModule, JarvisContext context, String message) {
        super(containingModule, context, message, getChannel(context));
    }
}

package fr.zelus.jarvis.plugins.slack.module.action;

import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.plugins.slack.JarvisSlackUtils;
import fr.zelus.jarvis.plugins.slack.module.SlackModule;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

public class Reply extends PostMessage {

    private static String getChannel(JarvisContext context) {
        Object channelValue = context.getContextValue(JarvisSlackUtils.SLACK_CONTEXT_KEY, JarvisSlackUtils
                .SLACK_CHANNEL_CONTEXT_KEY);
        checkNotNull(channelValue, "Cannot retrieve the Slack channel from the context");
        checkArgument(channelValue instanceof String, "Invalid Slack channel type, expected %s, found %s", String
                .class.getSimpleName(), channelValue.getClass().getSimpleName());
        return (String) channelValue;
    }

    public Reply(SlackModule containingModule, JarvisSession session, String message) {
        super(containingModule, session, message, getChannel(session.getJarvisContext()));
    }
}

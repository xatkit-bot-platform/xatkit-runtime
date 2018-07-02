package fr.zelus.jarvis.stubs.discord;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

public class StubPrivateMessageReceivedEvent extends PrivateMessageReceivedEvent {

    public StubPrivateMessageReceivedEvent(JDA api, StubMessage message) {
        super(api, 1, message);
    }
}

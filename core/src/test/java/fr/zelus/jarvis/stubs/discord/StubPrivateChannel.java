package fr.zelus.jarvis.stubs.discord;

import net.dv8tion.jda.client.entities.Call;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.requests.RestAction;

public class StubPrivateChannel implements PrivateChannel {

    public static String PRIVATE_CHANNEL_NAME = "private-test";

    @Override
    public boolean isFake() {
        return true;
    }

    @Override
    public boolean hasLatestMessage() {
        return false;
    }

    @Override
    public User getUser() {
        return null;
    }

    @Override
    public long getIdLong() {
        return 1;
    }

    @Override
    public JDA getJDA() {
        return null;
    }

    @Override
    public long getLatestMessageIdLong() {
        return 0;
    }

    @Override
    public Call getCurrentCall() {
        return null;
    }

    @Override
    public ChannelType getType() {
        return ChannelType.PRIVATE;
    }

    @Override
    public String getName() {
        return "test channel";
    }

    @Override
    public RestAction<Call> startCall() {
        return null;
    }

    @Override
    public RestAction<Void> close() {
        return null;
    }
}

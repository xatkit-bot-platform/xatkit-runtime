package edu.uoc.som.jarvis.stubs.discord;


import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.impl.AbstractMessage;
import net.dv8tion.jda.core.entities.impl.UserImpl;

public class StubMessage extends AbstractMessage {

    public static String TEST_MESSAGE_CONTENT = "hello";

    public static String TEST_MESSAGE_AUTHOR = "Stub Author";

    public static StubMessage createEmptyStubMessage() {
        return new StubMessage("");
    }

    public static StubMessage createTestStubMessage() {
        return new StubMessage(TEST_MESSAGE_CONTENT);
    }

    protected StubMessage(String content) {
        super(content, "", false);
    }

    @Override
    public long getIdLong() {
        return 0;
    }

    @Override
    public User getAuthor() {
        return new UserImpl(1, null).setName(TEST_MESSAGE_AUTHOR);
    }

    @Override
    public PrivateChannel getPrivateChannel() {
        return new StubPrivateChannel();
    }

    @Override
    protected void unsupported() {
        throw new UnsupportedOperationException("The operation is not supported");
    }
}

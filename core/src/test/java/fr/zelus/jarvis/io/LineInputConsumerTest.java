package fr.zelus.jarvis.io;

import fr.zelus.jarvis.stubs.StubInputProvider;
import fr.zelus.jarvis.stubs.StubJarvisCore;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.*;

import java.io.IOException;
import java.io.InputStream;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class LineInputConsumerTest {

    private static StubJarvisCore stubJarvisCore;

    private StubInputProvider validInputProvider;

    private LineInputConsumer lineInputConsumer;

    private Thread lineInputThread;

    private Thread readThread;

    @BeforeClass
    public static void setUpBeforeClass() {
        stubJarvisCore = new StubJarvisCore();
    }

    @Before
    public void setUp() {
        stubJarvisCore.clearHandledMessages();
        /*
         * Creates it before each test to clear the underlying stream.
         */
        validInputProvider = new StubInputProvider();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        stubJarvisCore.shutdown();
    }

    @After
    public void tearDown() {
        validInputProvider.close();
        if (nonNull(lineInputConsumer) && nonNull(lineInputThread) && lineInputThread.isAlive()) {
            lineInputThread.interrupt();
        }
        if (nonNull(readThread) && readThread.isAlive()) {
            readThread.interrupt();
        }
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test(expected = NullPointerException.class)
    public void constructNullJarvisCore() {
        new LineInputConsumer(null, validInputProvider);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullInputProvider() {
        new LineInputConsumer(stubJarvisCore, null);
    }

    @Test
    public void constructValidInputConsumer() throws IOException {
        lineInputConsumer = new LineInputConsumer(stubJarvisCore, validInputProvider);
        softly.assertThat(lineInputConsumer.getJarvisCore()).as("Valid JarvisCore").isEqualTo(stubJarvisCore);
        assertThat(lineInputConsumer.getInputStream()).as("Not null input stream").isNotNull();
        softly.assertThat(lineInputConsumer.getInputStream().available()).as("Connected in/out streams").isEqualTo(0);
        assertThat(lineInputConsumer.getReader()).as("Not null reader").isNotNull();
        assertThat(lineInputConsumer.getInputProvider()).as("Not null InputProvider");
        softly.assertThat(lineInputConsumer.getInputProvider()).as("Valid InputProvider").isEqualTo(validInputProvider);
    }

    @Test
    public void interruptLineInputConsumer() throws InterruptedException {
        createAndStartLineInputConsumer();
        assertThat(lineInputThread.isAlive()).as("LineInputProvider Thead is alive").isTrue();
        lineInputThread.interrupt();
        lineInputThread.join(1000);
        assertThat(lineInputThread.isAlive()).as("LineInputProvider Thread is alive after interruption").isFalse();
        /*
         * Read the stream in another Thread: if the stream is not closed the read operation will not return and will
         * block the execution of the other tests.
         */
        ReadTask readTask = new ReadTask(lineInputConsumer.getInputStream());
        readThread = new Thread(readTask);
        readThread.start();
        /*
         * Sleep to ensure that the consumer has read from the stream.
         */
        Thread.sleep(1000);
        softly.assertThat(readTask.getCaughtException()).as("An has been caught by the reader").isNotNull();
        softly.assertThat(readTask.getCaughtException()).as("Caught a Pipe closed IOException").isInstanceOf(IOException
                .class).hasMessage("Pipe closed");
    }

    @Test
    public void consumeMessage() throws InterruptedException {
        createAndStartLineInputConsumer();
        validInputProvider.write("test");
        /*
         * Sleep to ensure that the consumer has read from the stream.
         */
        Thread.sleep(500);
        assertThat(stubJarvisCore.getHandledMessages()).as("Message received by JarvisCore").contains("test");
    }

    @Test
    public void consumeMultipleMessages() throws InterruptedException {
        createAndStartLineInputConsumer();
        validInputProvider.write("test");
        validInputProvider.write("test2");
        /*
         * Sleep to ensure that the consumer has read from the stream.
         */
        Thread.sleep(500);
        assertThat(stubJarvisCore.getHandledMessages()).as("Received two messages").hasSize(2);
        softly.assertThat(stubJarvisCore.getHandledMessages().get(0)).as("Valid first message").isEqualTo("test");
        softly.assertThat(stubJarvisCore.getHandledMessages().get(1)).as("Valid second message").isEqualTo("test2");
    }

    private void createAndStartLineInputConsumer() {
        lineInputConsumer = new LineInputConsumer(stubJarvisCore, validInputProvider);
        lineInputThread = new Thread(lineInputConsumer);
        lineInputThread.start();
    }

    private static class ReadTask implements Runnable {

        private IOException caughtException;

        private InputStream inputStream;

        public IOException getCaughtException() {
            return caughtException;
        }

        public ReadTask(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try {
                inputStream.read();
            } catch (IOException e) {
                caughtException = e;
            }
        }
    }
}

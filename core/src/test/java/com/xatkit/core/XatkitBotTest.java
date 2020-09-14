package com.xatkit.core;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.recognition.regex.RegExIntentRecognitionProvider;
import com.xatkit.dsl.model.ExecutionModelProvider;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.EventDefinition;
import com.xatkit.test.bot.TestBot;
import org.apache.commons.configuration2.BaseConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class XatkitBotTest extends AbstractXatkitTest {

    private XatkitBot xatkitBot;

    private TestBot testBot;

    @Before
    public void setUp() {
        this.testBot = new TestBot();
    }

    @After
    public void tearDown() {
        if (nonNull(xatkitBot) && !xatkitBot.isShutdown()) {
            xatkitBot.shutdown();
        }
    }

    @Test(expected = NullPointerException.class)
    public void constructNullExecutionModel() {
        xatkitBot = new XatkitBot((ExecutionModel)null, new BaseConfiguration());
    }

    @Test(expected = NullPointerException.class)
    public void constructNullExecutionModelProvider() {
        xatkitBot = new XatkitBot((ExecutionModelProvider) null, new BaseConfiguration());
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        xatkitBot = new XatkitBot(testBot.getModel(), null);
    }

    @Test
    public void constructValidParameters() {
        xatkitBot = new XatkitBot(testBot.getModel(), new BaseConfiguration());
        assertThat(xatkitBot).isNotNull();
    }

    @Test
    public void runValidParameters() {
        xatkitBot = getValidXatkitBot();
        xatkitBot.run();
        assertThatXatkitBotIsInitializedWithModel(xatkitBot, testBot.getModel());
        /*
         * This method checks that the intent provider is the RegEx one, any other provider should be tested in its
         * own class.
         */
    }

    @Test(expected = XatkitException.class)
    public void shutdownAlreadyShutdown() {
        xatkitBot = getValidXatkitBot();
        xatkitBot.run();
        xatkitBot.shutdown();
        xatkitBot.shutdown();
    }

    @Test(expected = NullPointerException.class)
    public void getOrCreateContextNullSessionId() {
        xatkitBot = getValidXatkitBot();
        xatkitBot.run();
        xatkitBot.getOrCreateContext(null);
    }

    @Test
    public void getOrCreateContextValidSessionId() {
        xatkitBot = getValidXatkitBot();
        xatkitBot.run();
        StateContext context = xatkitBot.getOrCreateContext("contextId");
        assertThat(context).as("Not null StateContext").isNotNull();
        /*
         * Use contains because the underlying DialogFlow API add additional identification information in the
         * returned XatkitSession.
         */
        assertThat(context.getContextId()).as("Valid context ID").contains("contextId");
        assertThat(context.getNlpContext()).as("Not null session context").isNotNull();
        /*
         * We have one outgoing transition with an intent. This means that a context has been created to enable the
         * corresponding intent matching.
         */
        assertThat(context.getNlpContext()).hasSize(1);
    }

    @Test
    public void isShutdownNotRun() {
        xatkitBot = getValidXatkitBot();
        assertThat(xatkitBot.isShutdown()).isTrue();
    }

    @Test
    public void shutdown() {
        xatkitBot = getValidXatkitBot();
        xatkitBot.run();
        xatkitBot.shutdown();
        assertThat(xatkitBot.getExecutionService().isShutdown()).as("ExecutorService is shutdown");
        assertThat(xatkitBot.getIntentRecognitionProvider().isShutdown()).as("DialogFlow API is shutdown");
    }

    private XatkitBot getValidXatkitBot() {
        xatkitBot = new XatkitBot(testBot.getModel(), new BaseConfiguration());
        return xatkitBot;
    }

    /**
     * Computes a set of basic assertions on the provided {@code xatkitBot} using the provided {@code model}.
     *
     * @param xatkitBot     the {@link XatkitBot} instance to check
     * @param model the {@link ExecutionModel} to check
     */
    private void assertThatXatkitBotIsInitializedWithModel(XatkitBot xatkitBot, ExecutionModel model) {
        /*
         * isNotNull() assertions are not soft, otherwise the runner does not print the assertion error and fails on
         * a NullPointerException in the following assertions.
         */
        assertThat(xatkitBot.getIntentRecognitionProvider()).as("Not null IntentRecognitionProvider").isNotNull();
        /*
         * The provider should be the default one, any other provider is tested in its own class.
         */
        assertThat(xatkitBot.getIntentRecognitionProvider()).as("IntentRecognitionProvider is a " +
                "RegExIntentRecognitionProvider instance").isInstanceOf(RegExIntentRecognitionProvider.class);
        assertThat(xatkitBot.getEventDefinitionRegistry()).isNotNull();
        for(EventDefinition event : model.getUsedEvents()) {
            assertThat(xatkitBot.getEventDefinitionRegistry().getEventDefinition(event.getName())).isEqualTo(event);
        }
        assertThat(xatkitBot.getExecutionService().getModel()).as("Not null ExecutionModel")
                .isNotNull();
        assertThat(xatkitBot.getExecutionService().getModel()).as("Valid ExecutionModel").isEqualTo(model);
        assertThat(xatkitBot.isShutdown()).as("Not shutdown").isFalse();
        assertThat(xatkitBot.getXatkitServer()).as("Not null XatkitServer").isNotNull();
    }
}

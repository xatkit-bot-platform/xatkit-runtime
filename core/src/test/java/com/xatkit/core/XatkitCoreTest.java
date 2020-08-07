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

public class XatkitCoreTest extends AbstractXatkitTest {

    private XatkitCore xatkitCore;

    private TestBot testBot;

    @Before
    public void setUp() {
        this.testBot = new TestBot();
    }

    @After
    public void tearDown() {
        if (nonNull(xatkitCore) && !xatkitCore.isShutdown()) {
            xatkitCore.shutdown();
        }
    }

    @Test(expected = NullPointerException.class)
    public void constructNullExecutionModel() {
        xatkitCore = new XatkitCore((ExecutionModel)null, new BaseConfiguration());
    }

    @Test(expected = NullPointerException.class)
    public void constructNullExecutionModelProvider() {
        xatkitCore = new XatkitCore((ExecutionModelProvider) null, new BaseConfiguration());
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        xatkitCore = new XatkitCore(testBot.getModel(), null);
    }

    @Test
    public void constructValidParameters() {
        xatkitCore = new XatkitCore(testBot.getModel(), new BaseConfiguration());
        assertThat(xatkitCore).isNotNull();
    }

    @Test
    public void runValidParameters() {
        xatkitCore = getValidXatkitCore();
        xatkitCore.run();
        assertThatXatkitCoreIsInitializedWithModel(xatkitCore, testBot.getModel());
        /*
         * This method checks that the intent provider is the RegEx one, any other provider should be tested in its
         * own class.
         */
    }

    @Test(expected = XatkitException.class)
    public void shutdownAlreadyShutdown() {
        xatkitCore = getValidXatkitCore();
        xatkitCore.run();
        xatkitCore.shutdown();
        xatkitCore.shutdown();
    }

    @Test(expected = NullPointerException.class)
    public void getOrCreateContextNullSessionId() {
        xatkitCore = getValidXatkitCore();
        xatkitCore.run();
        xatkitCore.getOrCreateContext(null);
    }

    @Test
    public void getOrCreateContextValidSessionId() {
        xatkitCore = getValidXatkitCore();
        xatkitCore.run();
        StateContext context = xatkitCore.getOrCreateContext("contextId");
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
        xatkitCore = getValidXatkitCore();
        assertThat(xatkitCore.isShutdown()).isTrue();
    }

    @Test
    public void shutdown() {
        xatkitCore = getValidXatkitCore();
        xatkitCore.run();
        xatkitCore.shutdown();
        assertThat(xatkitCore.getExecutionService().isShutdown()).as("ExecutorService is shutdown");
        assertThat(xatkitCore.getIntentRecognitionProvider().isShutdown()).as("DialogFlow API is shutdown");
    }

    private XatkitCore getValidXatkitCore() {
        xatkitCore = new XatkitCore(testBot.getModel(), new BaseConfiguration());
        return xatkitCore;
    }

    /**
     * Computes a set of basic assertions on the provided {@code xatkitCore} using the provided {@code model}.
     *
     * @param xatkitCore     the {@link XatkitCore} instance to check
     * @param model the {@link ExecutionModel} to check
     */
    private void assertThatXatkitCoreIsInitializedWithModel(XatkitCore xatkitCore, ExecutionModel model) {
        /*
         * isNotNull() assertions are not soft, otherwise the runner does not print the assertion error and fails on
         * a NullPointerException in the following assertions.
         */
        assertThat(xatkitCore.getIntentRecognitionProvider()).as("Not null IntentRecognitionProvider").isNotNull();
        /*
         * The provider should be the default one, any other provider is tested in its own class.
         */
        assertThat(xatkitCore.getIntentRecognitionProvider()).as("IntentRecognitionProvider is a " +
                "RegExIntentRecognitionProvider instance").isInstanceOf(RegExIntentRecognitionProvider.class);
        assertThat(xatkitCore.getEventDefinitionRegistry()).isNotNull();
        for(EventDefinition event : model.getUsedEvents()) {
            assertThat(xatkitCore.getEventDefinitionRegistry().getEventDefinition(event.getName())).isEqualTo(event);
        }
        assertThat(xatkitCore.getExecutionService().getModel()).as("Not null ExecutionModel")
                .isNotNull();
        assertThat(xatkitCore.getExecutionService().getModel()).as("Valid ExecutionModel").isEqualTo(model);
        assertThat(xatkitCore.isShutdown()).as("Not shutdown").isFalse();
        assertThat(xatkitCore.getXatkitServer()).as("Not null XatkitServer").isNotNull();
    }
}

package com.xatkit.stubs;

import com.xatkit.core.ExecutionService;
import com.xatkit.core.RuntimePlatformRegistry;
import com.xatkit.core.XatkitCore;
import com.xatkit.core.XatkitCoreTest;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.recognition.dialogflow.DialogFlowApi;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.EventInstance;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link XatkitCore} subclass that stores handled messages in a {@link List}.
 * <p>
 * This class is designed to ease testing of classes depending on {@link XatkitCore}, and allows to easily retrieve
 * its processed messages (see {@link #getHandledEvents()}).
 */
public class StubXatkitCore extends XatkitCore {

    protected static ExecutionModel VALID_EXECUTION_MODEL = ExecutionFactory.eINSTANCE
            .createExecutionModel();

    /**
     * The {@link List} of {@link EventDefinition} that have been handled by this instance.
     */
    private List<EventDefinition> handledEvents;

    /**
     * Constructs a valid {@link StubXatkitCore} instance.
     */
    public StubXatkitCore() {
        /*
         * If the provided test-variables.properties file does not define DialogFlow credentials this class will use
         * the DefaultIntentRecognitionProvider.
         */
        super(XatkitCoreTest.buildConfiguration(VALID_EXECUTION_MODEL));
        this.handledEvents = new ArrayList<>();
        IntentDefinition welcomeIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        welcomeIntentDefinition.setName("Default Welcome Intent");
        welcomeIntentDefinition.getTrainingSentences().add("hello");
        getEventDefinitionRegistry().registerEventDefinition(welcomeIntentDefinition);
        if (!(this.getIntentRecognitionProvider() instanceof DialogFlowApi)) {
            /*
             * Only register the intent if we are not using the DialogFlowApi: the test agent already contains a
             * Default Welcome Intent, and registering it again throws an exception that make the test fail and
             * doesn't stop the XatkitServer.
             */
            getIntentRecognitionProvider().registerIntentDefinition(welcomeIntentDefinition);
        }
        /*
         * shutdown the default execution service to avoid multiple running instances.
         */
        this.executionService.shutdown();
        this.executionService = new StubExecutionService(VALID_EXECUTION_MODEL, this
                .getRuntimePlatformRegistry());
    }

    /**
     * Returns the {@link List} containing the handled {@link EventDefinition}s.
     *
     * @return the {@link List} containing the handled {@link EventDefinition}s
     */
    public List<EventDefinition> getHandledEvents() {
        return handledEvents;
    }

    /**
     * Clears the underlying {@link EventDefinition} {@link List}.
     */
    public void clearHandledMessages() {
        handledEvents.clear();
    }

    private class StubExecutionService extends ExecutionService {

        public StubExecutionService(ExecutionModel executionModel, RuntimePlatformRegistry registry) {
            super(executionModel, registry);
        }

        /**
         * Stores the provided {@code eventInstance} definition in the {@link #handledEvents} list.
         * <p>
         * <b>Note:</b> this method does not process the {@code message}, and does not build
         * {@link RuntimeAction}s from the provided {@code message}.
         *
         * @param eventInstance the {@link EventInstance} to store in the {@link #handledEvents} list
         * @param session       the user session to use to process the message
         */
        @Override
        public void handleEventInstance(EventInstance eventInstance, XatkitSession session) {
            StubXatkitCore.this.handledEvents.add(eventInstance.getDefinition());
        }
    }
}

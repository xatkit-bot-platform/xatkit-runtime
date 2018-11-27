package fr.zelus.jarvis.stubs;

import fr.zelus.jarvis.core.*;
import fr.zelus.jarvis.core.RuntimePlatformRegistry;
import fr.zelus.jarvis.core.platform.action.RuntimeAction;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.execution.ExecutionFactory;
import fr.zelus.jarvis.execution.ExecutionModel;
import fr.zelus.jarvis.intent.EventDefinition;
import fr.zelus.jarvis.intent.EventInstance;
import fr.zelus.jarvis.intent.IntentDefinition;
import fr.zelus.jarvis.intent.IntentFactory;
import fr.zelus.jarvis.test.util.VariableLoaderHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link JarvisCore} subclass that stores handled messages in a {@link List}.
 * <p>
 * This class is designed to ease testing of classes depending on {@link JarvisCore}, and allows to easily retrieve
 * its processed messages (see {@link #getHandledEvents()}).
 */
public class StubJarvisCore extends JarvisCore {

    protected static String VALID_PROJECT_ID = VariableLoaderHelper.getJarvisDialogFlowProject();

    protected static String VALID_LANGUAGE_CODE = "en-US";

    protected static ExecutionModel VALID_EXECUTION_MODEL = ExecutionFactory.eINSTANCE
            .createExecutionModel();

    /**
     * The {@link List} of {@link EventDefinition} that have been handled by this instance.
     */
    private List<EventDefinition> handledEvents;

    /**
     * Constructs a valid {@link StubJarvisCore} instance.
     */
    public StubJarvisCore() {
        super(JarvisCoreTest.buildConfiguration(VALID_PROJECT_ID, VALID_LANGUAGE_CODE, VALID_EXECUTION_MODEL));
        this.handledEvents = new ArrayList<>();
        IntentDefinition welcomeIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        welcomeIntentDefinition.setName("Default Welcome Intent");
        getEventDefinitionRegistry().registerEventDefinition(welcomeIntentDefinition);
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
         * @param session the user session to use to process the message
         */
        @Override
        public void handleEventInstance(EventInstance eventInstance, JarvisSession session) {
            StubJarvisCore.this.handledEvents.add(eventInstance.getDefinition());
        }
    }
}

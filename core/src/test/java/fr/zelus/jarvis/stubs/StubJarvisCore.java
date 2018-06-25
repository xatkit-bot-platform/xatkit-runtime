package fr.zelus.jarvis.stubs;

import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.orchestration.OrchestrationFactory;
import fr.zelus.jarvis.orchestration.OrchestrationModel;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link JarvisCore} subclass that stores handled messages in a {@link List}.
 * <p>
 * This class is designed to ease testing of classes depending on {@link JarvisCore}, and allows to easily retrieve
 * its processed messages (see {@link #getHandledMessages()}).
 */
public class StubJarvisCore extends JarvisCore {

    protected static String VALID_PROJECT_ID = "jarvis-fd96e";

    protected static String VALID_LANGUAGE_CODE = "en-US";

    protected static OrchestrationModel VALID_ORCHESTRATION_MODEL = OrchestrationFactory.eINSTANCE
            .createOrchestrationModel();

    protected static Class<StubInputProvider> VALID_INPUT_PROVIDER_CLAZZ = StubInputProvider.class;


    /**
     * The {@link List} of messages that have been handled by this instance.
     *
     * @see #handledMessages
     */
    private List<String> handledMessages;

    /**
     * Constructs a valid {@link StubJarvisCore} instance.
     */
    public StubJarvisCore() {
        super(VALID_PROJECT_ID, VALID_LANGUAGE_CODE, VALID_ORCHESTRATION_MODEL, VALID_INPUT_PROVIDER_CLAZZ);
        this.handledMessages = new ArrayList<>();
    }

    /**
     * Stores the provided {@code message} in the {@link #handledMessages} list.
     * <p>
     * <b>Note:</b> this method does not process the {@code message}, and does not build
     * {@link fr.zelus.jarvis.core.JarvisAction}s from the provided {@code message}.
     *
     * @param message the textual input to store in the {@link #handledMessages} list
     */
    @Override
    public void handleMessage(String message) {
        this.handledMessages.add(message);
    }

    /**
     * Returns the {@link List} containing the handled messages.
     *
     * @return the {@link List} containing the handled messages
     */
    public List<String> getHandledMessages() {
        return handledMessages;
    }

    /**
     * Clears the underlying message {@link List}.
     */
    public void clearHandledMessages() {
        handledMessages.clear();
    }
}

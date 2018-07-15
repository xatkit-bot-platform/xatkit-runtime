package fr.zelus.jarvis.core;

import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.EventDefinition;
import fr.zelus.jarvis.intent.EventInstance;
import fr.zelus.jarvis.intent.RecognizedIntent;
import fr.zelus.jarvis.orchestration.ActionInstance;
import fr.zelus.jarvis.orchestration.OrchestrationLink;
import fr.zelus.jarvis.orchestration.OrchestrationModel;

import java.util.Collections;
import java.util.List;

/**
 * Reads an {@link OrchestrationModel} and returns the {@link ActionInstance}s associated to the provided
 * {@link RecognizedIntent}s.
 * <p>
 * This class is used by {@link JarvisCore} to create the {@link JarvisAction} to execute when a
 * {@link RecognizedIntent} has been computed from an user input.
 *
 * @see ActionInstance
 * @see RecognizedIntent
 * @see JarvisCore
 */
public class OrchestrationService {

    /**
     * The {@link OrchestrationModel} used to retrieve {@link JarvisAction}s from the provided
     * {@link RecognizedIntent}s.
     */
    private OrchestrationModel orchestrationModel;

    /**
     * Constructs a new {@link OrchestrationService} based on the provided {@code orchestrationModel}.
     *
     * @param orchestrationModel the {@link OrchestrationModel} representing the intent-to-action bindings to use
     */
    public OrchestrationService(OrchestrationModel orchestrationModel) {
        this.orchestrationModel = orchestrationModel;
    }

    /**
     * Returns the {@link OrchestrationModel} associated to this class.
     *
     * @return the {@link OrchestrationModel} associated to this class
     */
    public OrchestrationModel getOrchestrationModel() {
        return orchestrationModel;
    }

    /**
     * Retrieves the {@link ActionInstance}s associated to the provided {@code eventInstance}.
     * <p>
     * This class navigates the underlying {@link OrchestrationModel} and retrieves the {@link ActionInstance}s
     * associated to the provided {@code eventInstance}. These {@link ActionInstance}s are used by the core
     * component to create the concrete {@link JarvisAction} to execute.
     *
     * @param eventInstance the {@link EventInstance} to retrieve the {@link ActionInstance}s from
     * @return a {@link List} containing the instantiated {@link ActionInstance}s associated to the provided {@code
     * recognizedIntent}.
     * @see JarvisModule#createJarvisAction(ActionInstance, RecognizedIntent, JarvisSession)
     */
    public List<ActionInstance> getActionFromEvent(EventInstance eventInstance) {
        EventDefinition eventDefinition  = eventInstance.getDefinition();
        for (OrchestrationLink link : orchestrationModel.getOrchestrationLinks()) {
            if (link.getEvent().getName().equals(eventDefinition.getName())) {
                return link.getActions();
            }
        }
        return Collections.emptyList();
    }
}

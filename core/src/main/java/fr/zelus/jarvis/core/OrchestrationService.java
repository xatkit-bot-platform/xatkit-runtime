package fr.zelus.jarvis.core;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.intent.IntentDefinition;
import fr.zelus.jarvis.intent.RecognizedIntent;
import fr.zelus.jarvis.module.Action;
import fr.zelus.jarvis.module.Module;
import fr.zelus.jarvis.orchestration.OrchestrationLink;
import fr.zelus.jarvis.orchestration.OrchestrationModel;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;

/**
 * Reads an {@link OrchestrationModel} and returns the {@link JarvisAction}s associated to the provided
 * {@link RecognizedIntent}s.
 * <p>
 * This class is used by {@link JarvisCore} to retrieve the {@link JarvisAction} to execute when a
 * {@link RecognizedIntent} has been computed from an user input. The {@link OrchestrationService} takes care of the
 * {@link JarvisAction} construction, and returns an valid action instances to be executed by the {@link JarvisCore}
 * component.
 *
 * @see JarvisCore
 * @see JarvisAction
 * @see RecognizedIntent
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
     * Retrieves and instantiate the {@link JarvisAction}s associated to the provided {@code recognizedIntent}.
     * <p>
     * This class navigates the underlying {@link OrchestrationModel} and retrieves the {@link Action} associated to
     * the provided {@code recognizedIntent}. These {@link Action}s are then reified into {@link JarvisAction}s and
     * instantiated by using the {@code recognizedIntent}'s values.
     *
     * @param recognizedIntent the {@link RecognizedIntent} to retrieve the {@link JarvisAction}s from
     * @return a {@link List} containing the instantiated {@link JarvisAction}s associated to the provided {@code
     * recognizedIntent}.
     * @see JarvisModuleRegistry#getJarvisModule(String)
     * @see JarvisModule#createJarvisAction(Action, RecognizedIntent)
     */
    public List<JarvisAction> getActionsFromIntent(RecognizedIntent recognizedIntent) {
        IntentDefinition intentDefinition = recognizedIntent.getDefinition();
        List<JarvisAction> jarvisActions = new ArrayList<>();
        for (OrchestrationLink link : orchestrationModel.getOrchestrationLinks()) {
            if (link.getIntent().getName().equals(intentDefinition.getName())) {
                // The link refers to this Intent
                for (Action modelAction : link.getActions()) {
                    JarvisModule jarvisModule = JarvisCore.getInstance().getJarvisModuleRegistry().getJarvisModule(
                            (Module) modelAction.eContainer());
                    JarvisAction jarvisAction = jarvisModule.createJarvisAction(modelAction, recognizedIntent);
                    if (isNull(jarvisAction)) {
                        Log.warn("Create Action with the provided parameters ({0}, {1}) returned null", modelAction,
                                recognizedIntent);
                    }
                    jarvisActions.add(jarvisAction);
                }
            }
        }
        return jarvisActions;
    }
}

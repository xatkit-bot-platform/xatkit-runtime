package fr.zelus.jarvis.core;

import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.intent.IntentDefinition;
import fr.zelus.jarvis.intent.RecognizedIntent;
import fr.zelus.jarvis.module.Action;
import fr.zelus.jarvis.orchestration.ActionInstance;
import fr.zelus.jarvis.orchestration.OrchestrationLink;
import fr.zelus.jarvis.orchestration.OrchestrationModel;

import java.util.Collections;
import java.util.List;

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
     * Returns the {@link OrchestrationModel} associated to this class.
     *
     * @return the {@link OrchestrationModel} associated to this class
     */
    public OrchestrationModel getOrchestrationModel() {
        return orchestrationModel;
    }

    /**
     * Retrieves and instantiate the {@link JarvisAction}s associated to the provided {@code recognizedIntent}.
     * <p>
     * This class navigates the underlying {@link OrchestrationModel} and retrieves the {@link Action} associated to
     * the provided {@code recognizedIntent}. These {@link Action}s are then reified into {@link JarvisAction}s and
     * instantiated by using the {@code recognizedIntent}'s values.
     *
     * @param recognizedIntent the {@link RecognizedIntent} to retrieve the {@link JarvisAction}s from
     * @param context          the {@link JarvisContext} associated to the action to create
     * @return a {@link List} containing the instantiated {@link JarvisAction}s associated to the provided {@code
     * recognizedIntent}.
     * @see JarvisModuleRegistry#getJarvisModule(String)
     * @see JarvisModule#createJarvisAction(ActionInstance, RecognizedIntent, JarvisContext)
     */
    public List<ActionInstance> getActionsFromIntent(RecognizedIntent recognizedIntent, JarvisContext
            context) {
        IntentDefinition intentDefinition = recognizedIntent.getDefinition();
//        List<Class<? extends JarvisAction>> jarvisActions = new ArrayList<>();
        for (OrchestrationLink link : orchestrationModel.getOrchestrationLinks()) {
            if (link.getIntent().getName().equals(intentDefinition.getName())) {
                return link.getActions();
                // The link refers to this Intent
//                for (ActionInstance actionInstance : link.getActions()) {
//                    Action baseAction = actionInstance.getAction();
//                    JarvisModule jarvisModule = JarvisCore.getInstance().getJarvisModuleRegistry().getJarvisModule(
//                            (Module) baseAction.eContainer());
////                    JarvisAction jarvisAction = jarvisModule.createJarvisAction(actionInstance, recognizedIntent,
////                            context);
//                    Class<? extends JarvisAction> jarvisActionClass = jarvisModule.getAction(actionInstance.getAction
//                            ().getName());
//                    if (isNull(jarvisActionClass)) {
//                        Log.warn("Cannot retrieve the JarvisAction", actionInstance,
//                                recognizedIntent);
//                    }
//                    jarvisActions.add(jarvisActionClass);
//                }
            }
        }
        return Collections.emptyList();
    }
}

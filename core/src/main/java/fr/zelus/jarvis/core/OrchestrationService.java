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

public class OrchestrationService {

    private OrchestrationModel orchestrationModel;


    public OrchestrationService(OrchestrationModel orchestrationModel) {
        this.orchestrationModel = orchestrationModel;
    }

    public List<JarvisAction> getActionsFromIntent(RecognizedIntent recognizedIntent) {
        IntentDefinition intentDefinition =  recognizedIntent.getDefinition();
        List<JarvisAction> jarvisActions = new ArrayList<>();
        for (OrchestrationLink link : orchestrationModel.getOrchestrationLinks()) {
            if (link.getIntent().getName().equals(intentDefinition.getName())) {
                // The link refers to this Intent
                for (Action modelAction : link.getActions()) {
                    JarvisModule jarvisModule = JarvisCore.getInstance().getJarvisModuleRegistry().getJarvisModule(
                            (Module) modelAction.eContainer());
                    JarvisAction jarvisAction = jarvisModule.createJarvisAction(modelAction, recognizedIntent);
                    if(isNull(jarvisAction)) {
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

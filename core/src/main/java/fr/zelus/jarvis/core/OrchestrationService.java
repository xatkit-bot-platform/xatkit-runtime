package fr.zelus.jarvis.core;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.orchestration.Action;
import fr.zelus.jarvis.orchestration.Intent;
import fr.zelus.jarvis.orchestration.OrchestrationLink;
import fr.zelus.jarvis.orchestration.OrchestrationModel;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

public class OrchestrationService {

    private OrchestrationModel orchestrationModel;

    private List<JarvisModule> modules;

    public OrchestrationService(OrchestrationModel orchestrationModel, List<JarvisModule> modules) {
        this.modules = modules;
        this.orchestrationModel = orchestrationModel;
    }

    // This should be an Intent from the common Intent metamodel, but it is not available yet
    public List<JarvisAction> getActionsFromIntent(com.google.cloud.dialogflow.v2.Intent intent) {
        List<JarvisAction> jarvisActions = new ArrayList<>();
        for(OrchestrationLink link: orchestrationModel.getOrchestrationLinks()) {
            if(link.getIntent().getName().equals(intent.getDisplayName())) {
                // The link refers to this Intent
                for(Action modelAction: link.getActions()) {
                    JarvisAction action = getJarvisAction(link.getIntent(), modelAction);
                    if(nonNull(action)) {
                        jarvisActions.add(action);
                    } else {
                        Log.warn("Null JarvisAction returned from intent {0} and model Action {1}", link.getIntent()
                                .getName(), modelAction.getName());
                    }
                    jarvisActions.add(getJarvisAction(link.getIntent(), modelAction));
                }
            }
        }
        return jarvisActions;
    }

    private JarvisAction getJarvisAction(Intent intent, Action action) {
        for(JarvisModule module: modules) {
            if(module.getName().equals(action.getPackage())) {
                List<String> intentOutputVariables = intent.getOutputVariables();
                Class<JarvisAction> jarvisActionClass = module.getActionWithName(action.getName());
                Constructor<?>[] constructorList = jarvisActionClass.getConstructors();
                for(int i = 0; i < constructorList.length; i++) {
                    Constructor<?> constructor = constructorList[i];
                    if(constructor.getParameterCount() == intentOutputVariables.size()) {
                        // Here we assume that all the parameters are String, this should be fixed
                        try {
                            return (JarvisAction) constructor.newInstance(intentOutputVariables.toArray());
                        } catch(InstantiationException | IllegalAccessException | InvocationTargetException e) {
                            Log.error("Cannot construct the JarvisAction {0}", jarvisActionClass.getSimpleName(), e);
                            // There isn't another constructor that match the provided output variables
                            return null;
                        }
                    }
                }
            }
        }
        Log.warn("Cannot find a JarvisAction corresponding to the provided Intent {0} and model Action {1}", intent
                .getName(), action.getName());
        return null;
    }
}

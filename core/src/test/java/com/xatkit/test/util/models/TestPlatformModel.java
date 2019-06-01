package edu.uoc.som.jarvis.test.util.models;

import edu.uoc.som.jarvis.platform.ActionDefinition;
import edu.uoc.som.jarvis.platform.InputProviderDefinition;
import edu.uoc.som.jarvis.platform.PlatformDefinition;
import edu.uoc.som.jarvis.platform.PlatformFactory;

public class TestPlatformModel {

    private PlatformDefinition platformDefinition;

    private ActionDefinition actionDefinition;

    private InputProviderDefinition inputProviderDefinition;

    public TestPlatformModel() {
        platformDefinition = PlatformFactory.eINSTANCE.createPlatformDefinition();
        platformDefinition.setName("StubRuntimePlatform");
        platformDefinition.setRuntimePath("edu.uoc.som.jarvis.stubs.StubRuntimePlatform");
        actionDefinition = PlatformFactory.eINSTANCE.createActionDefinition();
        actionDefinition.setName("StubRuntimeAction");
        platformDefinition.getActions().add(actionDefinition);
        inputProviderDefinition = PlatformFactory.eINSTANCE.createInputProviderDefinition();
        inputProviderDefinition.setName("StubInputProvider");
        platformDefinition.getEventProviderDefinitions().add(inputProviderDefinition);
    }

    public PlatformDefinition getPlatformDefinition() {
        return platformDefinition;
    }

    public ActionDefinition getActionDefinition() {
        return actionDefinition;
    }

    public InputProviderDefinition getInputProviderDefinition() {
        return inputProviderDefinition;
    }
}

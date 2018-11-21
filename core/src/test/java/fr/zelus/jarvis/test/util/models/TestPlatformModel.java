package fr.zelus.jarvis.test.util.models;

import fr.zelus.jarvis.platform.ActionDefinition;
import fr.zelus.jarvis.platform.InputProviderDefinition;
import fr.zelus.jarvis.platform.PlatformDefinition;
import fr.zelus.jarvis.platform.PlatformFactory;

public class TestPlatformModel {

    private PlatformDefinition platformDefinition;

    private ActionDefinition actionDefinition;

    private InputProviderDefinition inputProviderDefinition;

    public TestPlatformModel() {
        platformDefinition = PlatformFactory.eINSTANCE.createPlatformDefinition();
        platformDefinition.setName("StubRuntimePlatform");
        platformDefinition.setRuntimePath("fr.zelus.jarvis.stubs.StubRuntimePlatform");
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

package com.xatkit.test.util.models;

import com.xatkit.platform.ActionDefinition;
import com.xatkit.platform.InputProviderDefinition;
import com.xatkit.platform.PlatformDefinition;
import com.xatkit.platform.PlatformFactory;

public class TestPlatformModel {

    private PlatformDefinition platformDefinition;

    private ActionDefinition actionDefinition;

    private InputProviderDefinition inputProviderDefinition;

    public TestPlatformModel() {
        platformDefinition = PlatformFactory.eINSTANCE.createPlatformDefinition();
        platformDefinition.setName("StubRuntimePlatform");
        platformDefinition.setRuntimePath("com.xatkit.stubs.StubRuntimePlatform");
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

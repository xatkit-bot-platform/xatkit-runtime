package com.xatkit.test.util.models;

import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.Library;

public class TestIntentModel {

    private Library intentLibrary;

    private IntentDefinition intentDefinition;

    public TestIntentModel() {
        intentLibrary = IntentFactory.eINSTANCE.createLibrary();
        intentLibrary.setName("StubLibrary");
        intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName("Default Welcome Intent");
        intentLibrary.getEventDefinitions().add(intentDefinition);
    }

    public Library getIntentLibrary() {
        return intentLibrary;
    }

    public IntentDefinition getIntentDefinition() {
        return intentDefinition;
    }
}

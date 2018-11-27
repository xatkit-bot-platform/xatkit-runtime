package edu.uoc.som.jarvis.test.util.models;

import edu.uoc.som.jarvis.intent.IntentDefinition;
import edu.uoc.som.jarvis.intent.IntentFactory;
import edu.uoc.som.jarvis.intent.Library;

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

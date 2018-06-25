package fr.zelus.jarvis.stubs;

import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.io.InputProvider;

public class StubInputProviderDefaultConstructor extends InputProvider {

    public StubInputProviderDefaultConstructor(JarvisCore jarvisCore) {
        super(jarvisCore);
    }

    public void write(String message) {
        jarvisCore.handleMessage(message);
    }

    @Override
    public void run() {
        // do nothing
    }

}
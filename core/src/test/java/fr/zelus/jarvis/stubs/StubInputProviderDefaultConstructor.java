package fr.zelus.jarvis.stubs;

import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.io.InputProvider;

public class StubInputProviderDefaultConstructor extends InputProvider {

    public StubInputProviderDefaultConstructor(JarvisCore jarvisCore) {
        super(jarvisCore);
    }

    @Override
    public void run() {
        synchronized(this) {
            try {
                wait();
            } catch (InterruptedException e) {

            }
        }
    }

}
package fr.zelus.jarvis.stubs.io;

import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.io.EventProvider;

public class StubInputProviderNoConfigurationConstructor extends EventProvider {

    public StubInputProviderNoConfigurationConstructor(JarvisCore jarvisCore) {
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
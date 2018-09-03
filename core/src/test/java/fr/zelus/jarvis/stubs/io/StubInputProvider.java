package fr.zelus.jarvis.stubs.io;

import fr.zelus.jarvis.io.EventProvider;
import fr.zelus.jarvis.stubs.EmptyJarvisModule;
import org.apache.commons.configuration2.Configuration;

public class StubInputProvider extends EventProvider<EmptyJarvisModule> {

    public StubInputProvider(EmptyJarvisModule containingModule) {
        super(containingModule);
    }

    public StubInputProvider(EmptyJarvisModule containingModule, Configuration configuration) {
        this(containingModule);
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {

            }
        }
    }

}
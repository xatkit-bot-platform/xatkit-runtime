package fr.zelus.jarvis.stubs.io;

import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.io.InputProvider;
import org.apache.commons.configuration2.Configuration;

public class StubInputProvider extends InputProvider {

    public StubInputProvider(JarvisCore jarvisCore) {
        super(jarvisCore);
    }

    public StubInputProvider(JarvisCore jarvisCore, Configuration configuration) {
        this(jarvisCore);
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
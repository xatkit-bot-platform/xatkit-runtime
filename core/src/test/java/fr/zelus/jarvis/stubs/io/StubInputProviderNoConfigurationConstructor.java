package fr.zelus.jarvis.stubs.io;

        import fr.zelus.jarvis.io.EventProvider;
import fr.zelus.jarvis.stubs.EmptyRuntimePlatform;

public class StubInputProviderNoConfigurationConstructor extends EventProvider<EmptyRuntimePlatform> {

    public StubInputProviderNoConfigurationConstructor(EmptyRuntimePlatform runtimePlatform) {
        super(runtimePlatform);
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
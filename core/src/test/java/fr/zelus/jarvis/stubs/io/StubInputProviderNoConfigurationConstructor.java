package fr.zelus.jarvis.stubs.io;

        import fr.zelus.jarvis.io.EventProvider;
import fr.zelus.jarvis.stubs.EmptyJarvisModule;

public class StubInputProviderNoConfigurationConstructor extends EventProvider<EmptyJarvisModule> {

    public StubInputProviderNoConfigurationConstructor(EmptyJarvisModule containingModule) {
        super(containingModule);
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
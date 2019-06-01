package com.xatkit.stubs.io;

        import com.xatkit.core.platform.io.RuntimeEventProvider;
        import com.xatkit.stubs.EmptyRuntimePlatform;

public class StubInputProviderNoConfigurationConstructor extends RuntimeEventProvider<EmptyRuntimePlatform> {

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
package com.xatkit.stubs.io;

import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.stubs.EmptyRuntimePlatform;
import org.apache.commons.configuration2.Configuration;

public class StubInputProvider2 extends RuntimeEventProvider<EmptyRuntimePlatform> {

    public StubInputProvider2(EmptyRuntimePlatform runtimePlatform) {
        super(runtimePlatform);
    }

    public StubInputProvider2(EmptyRuntimePlatform runtimePlatform, Configuration configuration) {
        this(runtimePlatform);
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
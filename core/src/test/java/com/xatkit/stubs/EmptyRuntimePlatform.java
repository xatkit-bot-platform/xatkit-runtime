package com.xatkit.stubs;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.session.XatkitSession;
import org.eclipse.xtext.xbase.XMemberFeatureCall;

import java.util.List;

/**
 * An empty {@link RuntimePlatform} used to test {@link RuntimePlatform}-related methods.
 * <p>
 * See {@link StubRuntimePlatform} to create a stub {@link RuntimePlatform} that provided preset
 * {@link StubRuntimePlatform#getAction()} and
 * {@link StubRuntimePlatform#createRuntimeAction(XMemberFeatureCall, List, XatkitSession)} methods.
 */
public class EmptyRuntimePlatform extends RuntimePlatform {

    /**
     * Constructs a new {@link EmptyRuntimePlatform} from the provided {@link XatkitCore}.
     *
     * @param xatkitCore the {@link XatkitCore} instance associated to this runtimePlatform
     * @throws NullPointerException if the provided {@code xatkitCore} is {@code null}
     */
    public EmptyRuntimePlatform(XatkitCore xatkitCore) {
        super(xatkitCore);
    }
}

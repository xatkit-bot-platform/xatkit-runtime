package com.xatkit.core.recognition;

import com.xatkit.intent.impl.IntentDefinitionImpl;

/**
 * An {@link com.xatkit.intent.IntentDefinition} representing the default fallback.
 */
public class DefaultFallbackIntent extends IntentDefinitionImpl {

    /**
     * Constructs a new {@link DefaultFallbackIntent}.
     */
    DefaultFallbackIntent() {
        super();
        this.name = "Default_Fallback_Intent";
    }
}

package com.xatkit.dsl.library;

import com.xatkit.dsl.intent.IntentDefinitionProvider;
import lombok.NonNull;

public interface IntentStep extends LibraryProvider {

    @NonNull IntentStep intent(@NonNull IntentDefinitionProvider intentProvider);
}

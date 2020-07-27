package com.xatkit.dsl.library;

import com.xatkit.dsl.intent.IntentDefinitionProvider;

public interface IntentStep extends LibraryProvider {

    IntentStep intent(IntentDefinitionProvider intentProvider);
}

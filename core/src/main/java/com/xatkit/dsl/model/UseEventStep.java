package com.xatkit.dsl.model;

import com.xatkit.dsl.intent.IntentDefinitionProvider;
import com.xatkit.dsl.library.LibraryProvider;

public interface UseEventStep extends UsePlatformStep {

    // TODO change this for EventDefinitionProvider
    UseEventStep useEvent(IntentDefinitionProvider intentProvider);

    // TODO change this for something like LibraryProvider
    UseEventStep useEvents(LibraryProvider libraryProvider);

}

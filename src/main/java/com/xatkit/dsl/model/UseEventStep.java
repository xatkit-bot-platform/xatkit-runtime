package com.xatkit.dsl.model;

import com.xatkit.dsl.intent.EventDefinitionProvider;
import com.xatkit.dsl.intent.IntentDefinitionProvider;
import com.xatkit.dsl.library.LibraryProvider;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.Library;
import lombok.NonNull;

public interface UseEventStep extends UsePlatformStep {

    @NonNull UseEventStep useEvent(@NonNull EventDefinitionProvider eventProvider);

    @NonNull UseEventStep useEvent(@NonNull EventDefinition event);

    @NonNull UseEventStep useIntent(@NonNull IntentDefinitionProvider intentProvider);

    @NonNull UseEventStep useIntent(@NonNull IntentDefinition intent);

    @NonNull UseEventStep useIntents(@NonNull LibraryProvider libraryProvider);

    @NonNull UseEventStep useIntents(@NonNull Library library);
}

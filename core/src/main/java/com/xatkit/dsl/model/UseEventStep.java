package com.xatkit.dsl.model;

import com.xatkit.dsl.intent.EventDefinitionProvider;
import com.xatkit.dsl.library.LibraryProvider;
import lombok.NonNull;

public interface UseEventStep extends UsePlatformStep {

    @NonNull UseEventStep useEvent(@NonNull EventDefinitionProvider intentProvider);

    @NonNull UseEventStep useEvents(@NonNull LibraryProvider libraryProvider);
}

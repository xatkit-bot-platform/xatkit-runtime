package com.xatkit.dsl.library;

import com.xatkit.intent.Library;
import lombok.NonNull;

public interface LibraryProvider {

    @NonNull Library getLibrary();
}

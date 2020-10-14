package com.xatkit.dsl.model.impl;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.dsl.intent.EventDefinitionProvider;
import com.xatkit.dsl.intent.IntentDefinitionProvider;
import com.xatkit.dsl.library.LibraryProvider;
import com.xatkit.dsl.model.DefaultFallbackStateStep;
import com.xatkit.dsl.model.ExecutionModelProvider;
import com.xatkit.dsl.model.InitStateStep;
import com.xatkit.dsl.model.ListenToStep;
import com.xatkit.dsl.model.StateStep;
import com.xatkit.dsl.model.UseEventStep;
import com.xatkit.dsl.model.UsePlatformStep;
import com.xatkit.dsl.state.StateProvider;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.State;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.Library;
import lombok.NonNull;

public class ExecutionModelBuilder extends ExecutionModelProviderImpl implements
        UseEventStep,
        UsePlatformStep,
        ListenToStep,
        StateStep,
        InitStateStep,
        DefaultFallbackStateStep {

    public ExecutionModelBuilder() {
        this.model = ExecutionFactory.eINSTANCE.createExecutionModel();
    }


    @Deprecated
    @Override
    public @NonNull UseEventStep useEvent(@NonNull EventDefinitionProvider eventProvider) {
        return this.useEvent(eventProvider.getEventDefinition());
    }

    @Deprecated
    @Override
    public @NonNull UseEventStep useEvent(@NonNull EventDefinition event) {
        this.model.getUsedEvents().add(event);
        return this;
    }

    @Deprecated
    @Override
    public @NonNull UseEventStep useIntent(@NonNull IntentDefinitionProvider intentProvider) {
        return this.useEvent(intentProvider);
    }

    @Deprecated
    @Override
    public @NonNull UseEventStep useIntent(@NonNull IntentDefinition intent) {
        return this.useEvent(intent);
    }

    @Deprecated
    @Override
    public @NonNull UseEventStep useIntents(@NonNull LibraryProvider libraryProvider) {
        return this.useIntents(libraryProvider.getLibrary());
    }

    @Deprecated
    @Override
    public @NonNull UseEventStep useIntents(@NonNull Library library) {
        library.getEventDefinitions().forEach(e -> this.model.getUsedEvents().add(e));
        return this;
    }

    @Override
    public @NonNull UsePlatformStep usePlatform(@NonNull RuntimePlatform platform) {
        this.model.getUsedPlatforms().add(platform);
        return this;
    }

    @Override
    public @NonNull StateStep listenTo(@NonNull RuntimeEventProvider<?> provider) {
        this.model.getUsedProviders().add(provider);
        return this;
    }

    @Deprecated
    @Override
    public @NonNull StateStep useState(@NonNull StateProvider stateProvider) {
        this.model.getStates().add(stateProvider.getState());
        return this;
    }

    @Override
    public @NonNull DefaultFallbackStateStep initState(@NonNull StateProvider stateProvider) {
        return this.initState(stateProvider.getState());
    }

    @Override
    public @NonNull DefaultFallbackStateStep initState(@NonNull State state) {
        // TODO document that the state is added if it is not part of the specified states
        if(!this.model.getStates().contains(state)) {
            this.model.getStates().add(state);
        }
        this.model.setInitState(state);
        return this;
    }

    @Override
    public @NonNull ExecutionModelProvider defaultFallbackState(@NonNull StateProvider stateProvider) {
        return this.defaultFallbackState(stateProvider.getState());
    }

    @Override
    public @NonNull ExecutionModelProvider defaultFallbackState(@NonNull State state) {
        // TODO document that the state is added if it is not part of the specified states
        if(!this.model.getStates().contains(state)) {
            this.model.getStates().add(state);
        }
        this.model.setDefaultFallbackState(state);
        return this;
    }
}

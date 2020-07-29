package com.xatkit.dsl.model.impl;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.dsl.intent.EventDefinitionProvider;
import com.xatkit.dsl.library.LibraryProvider;
import com.xatkit.dsl.model.DefaultFallbackStateStep;
import com.xatkit.dsl.model.ExecutionModelProvider;
import com.xatkit.dsl.model.InitStateStep;
import com.xatkit.dsl.model.ListenToStep;
import com.xatkit.dsl.model.StateStep;
import com.xatkit.dsl.model.UseEventStep;
import com.xatkit.dsl.model.UsePlatformStep;
import com.xatkit.dsl.state.StateProvider;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.execution.State;
import lombok.NonNull;

public class ExecutionModelDelegate extends ExecutionModelProviderImpl implements
        UseEventStep,
        UsePlatformStep,
        ListenToStep,
        StateStep,
        InitStateStep,
        DefaultFallbackStateStep {

    public ExecutionModelDelegate(@NonNull ExecutionModel executionModel) {
        super(executionModel);
    }


    @Override
    public @NonNull UseEventStep useEvent(@NonNull EventDefinitionProvider intentProvider) {
        this.model.getUsedEvents().add(intentProvider.getEventDefinition());
        return this;
    }

    @Override
    public @NonNull UseEventStep useEvents(@NonNull LibraryProvider libraryProvider) {
        libraryProvider.getLibrary().getEventDefinitions().forEach(e -> this.model.getUsedEvents().add(e));
        return this;
    }

    @Override
    public @NonNull UsePlatformStep usePlatform(@NonNull RuntimePlatform platform) {
        this.model.getUsedPlatforms().add(platform);
        return this;
    }

    @Override
    public @NonNull ListenToStep listenTo(@NonNull RuntimeEventProvider<?> provider) {
        this.model.getUsedProviders().add(provider);
        return this;
    }

    @Override
    public @NonNull StateStep state(@NonNull StateProvider stateProvider) {
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

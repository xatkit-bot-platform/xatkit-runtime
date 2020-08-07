package com.xatkit.dsl.entity;

import lombok.NonNull;

public interface CompositeEntryStep {

    @NonNull CompositeEntryFragmentStep entry();
}

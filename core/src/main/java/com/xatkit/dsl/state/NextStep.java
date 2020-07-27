package com.xatkit.dsl.state;

// TODO check if this is a state provider
public interface NextStep extends StateProvider {

    TransitionStep next();
}

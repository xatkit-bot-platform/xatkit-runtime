package com.xatkit.dsl.state;

import com.xatkit.execution.State;

/*
 * This should be moved at the mm level to ensure interoperability between languages.
 */
public interface StateProvider {

    State getState();
}

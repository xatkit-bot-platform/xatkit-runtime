package com.xatkit.util;

import com.xatkit.execution.AutoTransition;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.execution.GuardedTransition;
import com.xatkit.execution.State;
import com.xatkit.execution.StateContext;
import com.xatkit.execution.Transition;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.util.predicate.ComposedPredicate;
import com.xatkit.util.predicate.IsEventDefinitionPredicate;
import com.xatkit.util.predicate.IsIntentDefinitionPredicate;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import static java.util.Objects.nonNull;

public class ExecutionModelUtils {

    private ExecutionModelUtils() {

    }

    /**
     * Returns the {@link IntentDefinition} accessed in the provided {@code transition}.
     *
     * @param transition the {@link Transition} to retrieve the intents from
     * @return the accessed {@link IntentDefinition} if it exists, {@code null} otherwise
     */
    public static @Nullable
    IntentDefinition getAccessedIntent(@NonNull Transition transition) {
        EventDefinition accessedEvent = getAccessedEvent(transition);
        if(accessedEvent instanceof IntentDefinition) {
            return (IntentDefinition) accessedEvent;
        }
        return null;
    }

    /**
     * Returns the {@link EventDefinition} accessed in the provided {@code transition}.
     *
     * @param transition the {@link Transition} to retrieve the events from
     * @return the accessed {@link EventDefinition} if it exists, {@code null} otherwise
     */
    public static @Nullable
    EventDefinition getAccessedEvent(@NonNull Transition transition) {
        if(transition instanceof GuardedTransition) {
            GuardedTransition guardedTransition = (GuardedTransition) transition;
            Collection<EventDefinition> result = getAccessedEvents(guardedTransition.getCondition());
            if (result.isEmpty()) {
                return null;
            } else {
                /*
                 * TODO this method should probably return a collection too.
                 */
                return result.iterator().next();
            }
        } else {
            return null;
        }
    }

    public static Collection<EventDefinition> getAccessedEvents(Predicate<StateContext> predicate) {
        return getAccessedEvents(predicate, new HashSet<>());
    }

    private static Collection<EventDefinition> getAccessedEvents(Predicate<? super StateContext> predicate,
                                                              Set<EventDefinition> result) {
        if(predicate instanceof IsEventDefinitionPredicate) {
            result.add(((IsEventDefinitionPredicate) predicate).getEventDefinition());
        } else if(predicate instanceof IsIntentDefinitionPredicate) {
            result.add(((IsIntentDefinitionPredicate) predicate).getIntentDefinition());
        } else if(predicate instanceof ComposedPredicate) {
            ComposedPredicate<? super StateContext> composedPredicate =
                    (ComposedPredicate<? super StateContext>) predicate;
            getAccessedEvents(composedPredicate.getP1(), result);
            getAccessedEvents(composedPredicate.getP2(), result);
        }
        return result;
    }

    /**
     * Returns all the {@link EventDefinition}s accessed in the provided {@code model}
     *
     * @param model the {@link ExecutionModel} to retrieve the events from
     * @return a {@link Set} containing the accessed {@link EventDefinition}s
     */
    public static Set<EventDefinition> getAllAccessedEvents(ExecutionModel model) {
        Set<EventDefinition> result = new HashSet<>();
        StreamSupport.stream(Spliterators.spliteratorUnknownSize(model.eAllContents(), Spliterator.ORDERED),
                false).filter(e -> e instanceof Transition)
                .forEach(t -> {
                    EventDefinition eventDefinition = getAccessedEvent((Transition) t);
                    if (nonNull(eventDefinition)) {
                        result.add(eventDefinition);
                    }
                });
        return result;
    }

    /**
     * Returns the {@link State} that can be reached from the provided {@code state} with a wildcard {@link Transition}.
     *
     * @param state the {@link State} to retrieve the wildcard-reachable {@link State} from
     * @return the reachable {@link State} if it exist, or {@code null}
     * @throws IllegalStateException if the provided {@code state} contains more than one transition and at least one
     *                               is a wildcard
     * @see #getAllStatesReachableWithWildcard(State) to retrieve all the {@link State}s that can be transitively
     * reached with a wildcard {@link Transition} from the provided {@code state}
     */
    public static @Nullable
    State getStateReachableWithWildcard(@NonNull State state) {
        State result = null;
        /*
         * Null conditions corresponds to auto-transitions.
         */
        if (state.getTransitions().stream().anyMatch(t -> t instanceof AutoTransition)) {
            if (state.getTransitions().size() > 1) {
                throw new IllegalStateException(MessageFormat.format("The provided state {0} contains more than 1 " +
                        "transition and at least one is a wildcard", state.getName()));
            } else {
                result = state.getTransitions().get(0).getState();
            }
        }
        return result;
    }

    /**
     * Returns all the {@link State}s that can be transitively reached from the provided {@code state} with wildcard
     * {@link Transition}s.
     * <p>
     * <b>Note</b>: the order of the returned state is undefined, you cannot use it to navigate the states in a
     * depth-first or breadth-first way.
     *
     * @param state the {@link State} to use to start the search
     * @return the {@link State}s that can be reached from the provided {@code state} with wildcard {@link Transition}s
     * @throws IllegalStateException if the provided {@code state} contains more than one transition and at least one
     *                               is a wildcard
     * @see #getAllStatesReachableWithWildcard(State, Set)
     */
    public static Collection<State> getAllStatesReachableWithWildcard(State state) {
        return getAllStatesReachableWithWildcard(state, new HashSet<>());
    }

    /**
     * Returns all the {@link State}s that can be transitively reached from the provided {@code state} with wildcard
     * {@link Transition}s.
     * <p>
     * <b>Note</b>: the order of the returned state is undefined, you cannot use it to navigate the states in a
     * depth-first or breadth-first way.
     * <p>
     * This method is a recursive implementation of {@link #getAllStatesReachableWithWildcard(State)} that uses the
     * {@code result} {@link Set} as an accumulator.
     *
     * @param state  the {@link State} to use to start the search
     * @param result the {@link Set} used to gather the retrieved {@link State}s
     * @return the {@link State}s that can be reached from the provided {@code state} with wildcard {@link Transition}s
     * @throws IllegalStateException if the provided {@code state} contains more than one transition and at least one
     *                               is a wildcard
     */
    private static Collection<State> getAllStatesReachableWithWildcard(State state, Set<State> result) {
        boolean added = result.add(state);
        if (added) {
            State otherState = getStateReachableWithWildcard(state);
            if (nonNull(otherState)) {
                return getAllStatesReachableWithWildcard(otherState, result);
            } else {
                return result;
            }
        } else {
            /*
             * Break infinite loops (we have already added the state, no need to check its transitions)
             */
            return result;
        }
    }
}

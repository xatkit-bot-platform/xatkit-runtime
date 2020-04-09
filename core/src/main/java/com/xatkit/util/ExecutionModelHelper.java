package com.xatkit.util;

import com.xatkit.core.RuntimePlatformRegistry;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.execution.State;
import com.xatkit.execution.Transition;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.IntentDefinition;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.XMemberFeatureCall;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static fr.inria.atlanmod.commons.Preconditions.checkState;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * An utility class the eases the access to {@link ExecutionModel}'s features.
 * <p>
 * The singleton instance of this class is initialized from an {@link ExecutionModel} instance, and provide
 * high-level method to retrieve <i>Init</i> and <i>Default_Fallback</i> states, compute top-level
 * {@link IntentDefinition}, or retrieve accessed {@link EventDefinition}.
 */
public class ExecutionModelHelper {

    /**
     * The singleton instance of this class.
     */
    private static ExecutionModelHelper INSTANCE = null;

    /**
     * Initializes the {@link ExecutionModelHelper} singleton instance with the provided {@code executionModel}.
     * <p>
     * This method must be called before any access to the singleton instance.
     *
     * @param executionModel the {@link ExecutionModel} used to initialize the singleton instance
     * @return the created {@link ExecutionModelHelper}
     */
    public static ExecutionModelHelper create(ExecutionModel executionModel) {
        if (nonNull(INSTANCE)) {
            throw new IllegalStateException("Cannot create an ExecutionModelHelper, it already exists");
        } else {
            INSTANCE = new ExecutionModelHelper(executionModel);
            return INSTANCE;
        }
    }

    /**
     * Returns the singleton instance of {@link ExecutionModelHelper}.
     * <p>
     * The singleton instance must have been initialized with {@link ExecutionModelHelper#create(ExecutionModel)}
     * before accessing it.
     *
     * @return the singleton instance of {@link ExecutionModelHelper}
     * @throws IllegalStateException if the singleton instance has not been initialized
     * @see #create(ExecutionModel)
     */
    public static ExecutionModelHelper getInstance() {
        if (isNull(INSTANCE)) {
            throw new IllegalStateException(MessageFormat.format("Cannot access the {0} instance, the {0} has not " +
                    "been initialized. Use {0}.create to initialize it.", ExecutionModelHelper.class.getSimpleName()));
        }
        return INSTANCE;
    }

    /**
     * The underlying {@link ExecutionModel}.
     */
    private ExecutionModel executionModel;

    /**
     * The <i>Init</i> {@link State} of the {@link ExecutionModel}.
     */
    private State initState;

    /**
     * The <i>Default_Fallback</i> {@link State} of the {@link ExecutionModel}.
     */
    private State fallbackState;

    /**
     * The cached collection of top-level {@link IntentDefinition} associated to the {@link ExecutionModel}.
     * <p>
     * Top-level intents are intents that do not require any context to get matched. They are defined as part of the
     * <i>Init</i> state's transitions, or in states that can be reached from the <i>Init</i> through wildcard
     * transitions.
     */
    private Collection<IntentDefinition> topLevelIntents;

    /**
     * The cached collection of all accessed {@link EventDefinition}.
     */
    private Collection<EventDefinition> allAccessedEvents;

    /**
     * Initializes the {@link ExecutionModelHelper} with the provided {@link ExecutionModel}.
     * <p>
     * This constructor retrieves the <i>Init</i> and <i>Default_Fallback</i> {@link State}s from the provided
     * {@link ExecutionModel}, and compute high-level information such as the top-level {@link IntentDefinition}s or
     * the accessed {@link EventDefinition}.
     *
     * @param executionModel the {@link ExecutionModel} used to initialize the helper
     */
    private ExecutionModelHelper(ExecutionModel executionModel) {
        this.executionModel = executionModel;
        for (State s : executionModel.getStates()) {
            if (s.getName().equals("Init")) {
                this.initState = s;
            } else if (s.getName().equals("Default_Fallback")) {
                this.fallbackState = s;
            }
        }
        this.topLevelIntents = computeTopLevelIntents();
        this.allAccessedEvents = getAccessedEvents(executionModel::eAllContents);
    }

    /**
     * Returns the underlying {@link ExecutionModel}.
     *
     * @return the underlying {@link ExecutionModel}
     */
    public ExecutionModel getExecutionModel() {
        return this.executionModel;
    }

    /**
     * Returns the <i>Init</i> {@link State} of the {@link ExecutionModel}.
     *
     * @return the <i>Init</i> {@link State} of the {@link ExecutionModel}
     */
    public State getInitState() {
        return this.initState;
    }

    /**
     * Returns the <i>Default_Fallback</i> {@link State} of the {@link ExecutionModel}.
     *
     * @return the <i>Default_Fallback</i> {@link State} of the {@link ExecutionModel}.
     */
    public State getFallbackState() {
        return this.fallbackState;
    }

    /**
     * Returns the top-level {@link IntentDefinition}s of the {@link ExecutionModel}.
     * <p>
     * Top-level intents are intents that do not require any context to get matched. They are defined as part of the
     * <i>Init</i> state's transitions, or in states that can be reached from the <i>Init</i> through wildcard
     * transitions.
     *
     * @return the top-level {@link IntentDefinition}s of the {@link ExecutionModel}.
     */
    public Collection<IntentDefinition> getTopLevelIntents() {
        return this.topLevelIntents;
    }

    /**
     * Returns all the {@link EventDefinition}s accessed in the {@link ExecutionModel}.
     * <p>
     * This method does not filter where the {@link EventDefinition}s are accessed. If you need precise filtering you
     * can check {@link #getAccessedEvents(Transition)} to retrieve the {@link EventDefinition}s accessed in a
     * {@link Transition}.
     *
     * @return the {@link EventDefinition}s accessed in the {@link ExecutionModel}
     * @see #getAccessedEvents(Transition)
     */
    public Collection<EventDefinition> getAllAccessedEvents() {
        return this.allAccessedEvents;
    }

    /**
     * Retrieves the top-level {@link IntentDefinition}s of the {@link ExecutionModel}.
     * <p>
     * Top-level intents are intents that do not require any context to get matched. They are defined as part of the
     * <i>Init</i> state's transitions, or in states that can be reached from the <i>Init</i> through wildcard
     * transitions.
     *
     * @return the top-level {@link IntentDefinition}s of the {@link ExecutionModel}
     */
    private Collection<IntentDefinition> computeTopLevelIntents() {
        Set<IntentDefinition> result = new HashSet<>();
        Collection<State> topLevelStates = getAllStatesReachableWithWildcard(initState);
        topLevelStates.stream().flatMap(state -> state.getTransitions().stream()).forEach(t -> {
            getAccessedEvents(t).forEach(e -> {
                if (e instanceof IntentDefinition) {
                    result.add((IntentDefinition) e);
                }
            });
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
    public @Nullable
    State getStateReachableWithWildcard(State state) {
        checkNotNull(state, "Cannot retrieve the state reachable with wildcard transition from the provided state %s"
                , state);
        State result = null;
        if (state.getTransitions().stream().anyMatch(Transition::isIsWildcard)) {
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
     * @see Transition#isIsWildcard()
     * @see #getAllStatesReachableWithWildcard(State, Set)
     */
    public Collection<State> getAllStatesReachableWithWildcard(State state) {
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
    private Collection<State> getAllStatesReachableWithWildcard(State state, Set<State> result) {
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

    /**
     * Returns the {@link EventDefinition}s accessed from the provided {@link Transition}.
     * <p>
     * This method looks in the content tree of the provided {@link Transition} to retrieve the
     * {@link EventDefinition} accesses. This means that {@link EventDefinition} accesses defined as part of complex
     * conditions will be retrieved by this method.
     *
     * @param transition the {@link Transition} to retrieve the {@link EventDefinition} accesses from
     * @return the {@link EventDefinition}s accessed in the provided {@link Transition}
     */
    public Collection<EventDefinition> getAccessedEvents(Transition transition) {
        Iterable<EObject> transitionContents = transition::eAllContents;
        return getAccessedEvents(transitionContents);
    }

    /**
     * Returns all the {@link EventDefinition} accesses from the provided {@link EObject}s.
     * <p>
     * This method does not iterate the content tree of the provided {@link EObject}.
     *
     * @param eObjects the {@link EObject}s to retrieve the {@link EventDefinition} accesses from
     * @return the accessed {@link EventDefinition}s
     */
    private Collection<EventDefinition> getAccessedEvents(Iterable<EObject> eObjects) {
        Set<EventDefinition> result = new HashSet<>();
        for (EObject e : eObjects) {
            if (e instanceof XFeatureCall) {
                XFeatureCall featureCall = (XFeatureCall) e;
                if (isEventDefinitionAccess(featureCall.getFeature())) {
                    EventDefinition eventDefinition = getAccessedEventDefinition(featureCall.getFeature());
                    if (nonNull(eventDefinition)) {
                        result.add(eventDefinition);
                    } else {
                        throw new RuntimeException(MessageFormat.format("Cannot retrieve the {0} from the provided " +
                                        "{1} {2}", EventDefinition.class.getSimpleName(),
                                featureCall.getFeature().getClass().getSimpleName(), featureCall.getFeature()));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns {@code true} if the provided {@link JvmIdentifiableElement} is an {@link EventDefinition} access.
     * <p>
     * This method checks if the provided {@code element} corresponds to an access (in the execution language) to a
     * class derived from the imported events. Such accesses are typically performed in transition guards:
     * {@code
     * <pre>
     * MyState {
     *     Next {
     *         intent == MyIntent --> OtherState
     *     }
     * }
     * </pre>
     * }
     * The inferrer allows such accesses by deriving a class {@code MyIntent} from the imported
     * {@link com.xatkit.intent.IntentDefinition}s.
     * <p>
     * See {@link #getAccessedEventDefinition(JvmIdentifiableElement)} to retrieve the accessed {@link EventDefinition}.
     *
     * @param element the {@link JvmIdentifiableElement} to check
     * @return {@code true} if the provided {@code element} is an {@link EventDefinition} access, {@code false}
     * otherwise
     * @see #getAccessedEventDefinition(JvmIdentifiableElement)
     */
    private boolean isEventDefinitionAccess(JvmIdentifiableElement element) {
        if (element instanceof JvmGenericType) {
            JvmGenericType typeFeature = (JvmGenericType) element;
            if (typeFeature.getSuperTypes().stream().anyMatch(t -> t.getIdentifier().equals(EventDefinition.class.getName()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the {@link EventDefinition} associated to the provided {@link JvmIdentifiableElement}.
     * <p>
     * This method checks if the provided {@code element} corresponds to an access (in the execution language) to a
     * class derived from the imported events, and returns the events. Such accesses are typically performed in
     * transition guards:
     * {@code
     * <pre>
     * MyState {
     *     Next {
     *         intent == MyIntent --> OtherState
     *     }
     * }
     * </pre>
     * }
     * The inferrer allows such accesses by deriving a class {@code MyIntent} from the imported
     * {@link com.xatkit.intent.IntentDefinition}s.
     * <p>
     * See {@link #isEventDefinitionAccess(JvmIdentifiableElement)} to check whether {@code element} is an
     * {@link EventDefinition} access.
     *
     * @param element the {@link JvmIdentifiableElement} to check
     * @return the associated {@link EventDefinition} if it exists, {@code null} otherwise
     * @see #isEventDefinitionAccess(JvmIdentifiableElement)
     */
    private @Nullable
    EventDefinition getAccessedEventDefinition(JvmIdentifiableElement element) {
        if (element instanceof JvmGenericType) {
            JvmGenericType typeFeature = (JvmGenericType) element;
            if (typeFeature.getSuperTypes().stream().anyMatch(t -> t.getIdentifier().equals(EventDefinition.class.getName()))) {
                Optional<JvmMember> field =
                        typeFeature.getMembers().stream().filter(m -> m instanceof JvmField && m.getSimpleName().equals("base")).findAny();
                if (field.isPresent()) {
                    return (EventDefinition) ((JvmField) field.get()).getConstantValue();
                } else {
                    throw new RuntimeException(MessageFormat.format("Cannot find the static field {0}.{1}, this field" +
                            " should have been set during Xtext parsing", element.getSimpleName(), "base"));
                }
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if the provided {@code featureCall} is a platform's action call, {@code false} otherwise.
     *
     * @param featureCall the {@link XMemberFeatureCall} to check
     * @param registry    the {@link RuntimePlatformRegistry} containing the loaded
     *                    {@link com.xatkit.platform.PlatformDefinition}s.
     * @return {@code true} if the provided {@code featureCall} is a platform's action call, {@code false} otherwise
     */
    public boolean isPlatformActionCall(XMemberFeatureCall featureCall, RuntimePlatformRegistry registry) {
        String platformName;
        try {
            platformName = getPlatformName(featureCall);
        } catch (IllegalStateException e) {
            /*
             * The qualified name doesn't match the Platform.Action pattern, the feature call cannot be a platform
             * action call.
             */
            return false;
        }
        return registry.getLoadedPlatformDefinitions().stream().anyMatch(p -> p.getName().equals(platformName));
    }

    /**
     * Returns the name of the platform corresponding to the provided {@code actionCall}.
     * <p>
     * The qualified name of the provided {@code actionCall} matches the following pattern: {@code Platform.Action}.
     * This method extracts the name of the platform and returns it as a {@link String}.
     *
     * @param actionCall the {@link XMemberFeatureCall} representing the action call to retrieve the platform name from
     * @return the platform name
     * @throws NullPointerException  if the provided {@code actionCall} is {@code null}
     * @throws IllegalStateException if the {@code actionCall}'s qualified name doesn't follow the {@code Platform
     *                               .Action} template
     */
    public String getPlatformName(XMemberFeatureCall actionCall) {
        checkNotNull(actionCall, "Cannot retrieve the platform name of the provided %s %s",
                XMemberFeatureCall.class.getSimpleName(), actionCall);
        String[] splittedActionName = splitActionCallName(actionCall);
        return splittedActionName[0];
    }

    /**
     * Returns the name of the action corresponding to the provided {@code actionCall}.
     * <p>
     * The qualified name of the provided {@code actionCall} matches the following pattern: {@code Platform.Action}.
     * This method extracts the name of the action and returns it as a {@link String}.
     *
     * @param actionCall the {@link XMemberFeatureCall} representing the action call to retrieve the action name from
     * @return the action name
     * @throws NullPointerException  if the provided {@code actionCall} is {@code null}
     * @throws IllegalStateException if the {@code actionCall}'s qualified name doesn't follow the {@code Platform
     *                               .Action} template
     */
    public String getActionName(XMemberFeatureCall actionCall) {
        checkNotNull(actionCall, "Cannot retrieve the action name of the provided %s %s",
                XMemberFeatureCall.class.getSimpleName(), actionCall);
        String[] splittedActionName = splitActionCallName(actionCall);
        return splittedActionName[1];
    }

    /**
     * Returns an array of {@link String}s containing the {@code actionCall}'s qualified name parts.
     *
     * @param actionCall the {@link XMemberFeatureCall} representing the action to compute the qualified name parts
     * @return an array of {@link String}s containing the {@code actionCall}'s qualified name parts
     * @throws NullPointerException  if the provided {@code actionCall} is {@code null}
     * @throws IllegalStateException if the computed array does not contain exactly two elements (the {@code
     *                               actionCall} should follow the {@code Platform.Action} template)
     */
    private static String[] splitActionCallName(XMemberFeatureCall actionCall) {
        checkNotNull(actionCall, "Cannot split the name of the provided %s %s",
                XMemberFeatureCall.class.getSimpleName(), actionCall);
        String qualifiedName = actionCall.getFeature().getQualifiedName();
        String[] splittedQualifiedName = qualifiedName.split("\\.");
        checkState(splittedQualifiedName.length == 2, "An error occurred when splitting the name of the provided %s " +
                        "expected the following syntax: PlatformName.ActionName, found %s",
                XMemberFeatureCall.class.getSimpleName(), qualifiedName);
        return splittedQualifiedName;
    }
}

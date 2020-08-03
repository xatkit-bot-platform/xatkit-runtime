package com.xatkit.core;

import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.execution.State;
import com.xatkit.execution.StateContext;
import com.xatkit.execution.Transition;
import com.xatkit.intent.ContextInstance;
import com.xatkit.intent.ContextParameterValue;
import com.xatkit.intent.EventInstance;
import fr.inria.atlanmod.commons.log.Log;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Handles incoming {@link EventInstance}s and manages the underlying state machine.
 * <p>
 * This class is responsible of transition navigation, body/fallback executions, and exception handling. The Xatkit
 * runtime forwards received events to this service using
 * {@link #handleEventInstance(EventInstance, XatkitSession)}.
 * <p>
 * The reaction to a received event (body/fallback execution, transition evaluations) is executed in a dedicated
 * {@link Thread}.
 *
 * @see EventInstance
 */
public class ExecutionService {

    /**
     * The underlying state machine model used to compute transitions and find executable {@link State}s.
     */
    @Getter
    private ExecutionModel model;

    /**
     * The Xatkit {@link Configuration}.
     */
    private Configuration configuration;

    /**
     * The {@link ExecutorService} used to process the reaction to an incoming event.
     */
    @Getter
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Creates a new {@link ExecutionService} from the provided {@code model} and {@code configuration}.
     *
     * @param model         the {@link ExecutionModel} representing bot's state machine
     * @param configuration the Xatkit configuration
     */
    public ExecutionService(@NonNull ExecutionModel model, @NonNull Configuration configuration) {
        this.model = model;
        this.configuration = configuration;
    }

    /**
     * Initializes the provided {@code session}'s {@link State} and executes it.
     * <p>
     * The provided {@link XatkitSession}'s {@link State} is set with the {@code Init} {@link State} of the bot
     * execution model, and the {@code body} section of this {@link State} is directly executed.
     *
     * @param session the {@link XatkitSession} to initialize
     */
    public void initSession(@NonNull XatkitSession session) {
        session.setState(this.model.getInitState());
        this.executeBody(session.getState(), session);
    }

    /**
     * Executes the body of the provided {@code state}, using the provided {@code session}.
     * <p>
     * {@link Throwable}s thrown by the executed {@code body} are logged and rethrown to the caller.
     * <p>
     * Once the {@code body} has been full (and successfully) executed this method looks for automated transitions to
     * navigate.
     * <p>
     * This method can be safely called on {@link State}s that do not define a body.
     *
     * @param state   the {@link State} to execute the body section of
     * @param session the {@link XatkitSession} holding the contextual information
     */
    private void executeBody(@NonNull State state, @NonNull XatkitSession session) {
        Consumer<StateContext> body = state.getBody();
        if (isNull(body)) {
            Log.debug("{0}'s body section is null, skipping its execution", state.getName());
        } else {
            try {
                body.accept(session);
            } catch (Throwable t) {
                Log.error(t, "An error occurred when executing the body of state {0}", state.getName());
                throw t;
            }
        }
        /*
         * Set the current event instance to null: we want to check automated transitions and context-based
         * transitions. We don't want to return a transition that matches the exact same intent as the one that
         * triggered this state.
         */
        session.setEventInstance(null);
        Transition navigableTransition = getNavigableTransitions(state, session);
        if (nonNull(navigableTransition)) {
            session.setState(navigableTransition.getState());
            executeBody(navigableTransition.getState(), session);
        }
    }

    /**
     * Executes the fallback of the provided {@code state}, using the provided {@code session}.
     * <p>
     * The fallback of a {@link State} is executed when an event is received and the engine cannot
     * find any navigable {@link Transition}. If the provided {@code state} does not define a <i>fallback</i> section
     * the <i>body</i> of the <b>Default_Fallback</b> {@link State} is executed.
     * <p>
     * {@link Throwable}s thrown by the executed {@code fallback} are logged and rethrown to the caller.
     *
     * @param state   the {@link State} to execute the fallback section of
     * @param session the {@link XatkitSession} holding the contextual information
     * @see ExecutionModel#getDefaultFallbackState()
     */
    private void executeFallback(@NonNull State state, @NonNull XatkitSession session) {
        Consumer<StateContext> fallback = state.getFallback();
        if (isNull(fallback)) {
            this.executeBody(model.getDefaultFallbackState(), session);
        } else {
            try {
                fallback.accept(session);
            } catch (Throwable t) {
                Log.error(t, "An error occurred when executing the fallback of state {0}", state.getName());
                throw t;
            }
        }
        /*
         * The fallback semantics implies that the execution engine stays in the same state. This means that we need
         * to increment all the context lifespans to be sure they will be available for the next intent recognition
         * (otherwise they will be deleted and the matching will be inconsistent).
         */
        session.getRuntimeContexts().incrementLifespanCounts();
    }

    /**
     * Evaluates the provided {@code state}'s {@link Transition}s and return the one that can be navigated.
     * <p>
     * This method checks, for all the {@link Transition}s, whether their condition is fulfilled or not. The
     * provided {@code session} is used as parameter of the evaluated conditions (this means that all the values
     * stored in the {@code session} can be manipulated by the condition).
     * <p>
     * This method cannot return more than one {@link Transition}. Multiple navigable transitions are considered
     * design issues making the bot behavior unreliable. This method returns {@code null} if it cannot find any
     * navigable {@link Transition}.
     *
     * @param state   the current {@link State} to compute the navigable {@link Transition}s from
     * @param session the {@link XatkitSession} holding the context information
     * @return the navigable {@link Transition} if it exists, {@code null} otherwise
     * @throws IllegalStateException if more than 1 navigable transition is found
     */
    private @Nullable
    Transition getNavigableTransitions(@NonNull State state, @NonNull XatkitSession session) {
        /*
         * Use a list to store the navigable transitions so we can print a more useful error message is more than one
         *  is found.
         */
        List<Transition> result = new ArrayList<>();
        for (Transition t : state.getTransitions()) {
            if (isNull(t.getCondition())) {
                /*
                 * Null conditions represent auto-transitions.
                 */
                result.add(t);
                continue;
            }
            /*
             * Create the context with the received EventInstance. This is the instance we want to use in the
             * transition conditions.
             */
            try {
                if (t.getCondition().test(session)) {
                    result.add(t);
                }
            } catch (Throwable throwable) {
                Log.error(throwable, "An exception occurred when evaluating transition {0} of state {1}",
                        state.getTransitions().indexOf(t), state.getName());
                continue;
            }
        }
        if (result.size() > 1) {
            throw new IllegalStateException(MessageFormat.format("Found several navigable transitions ({0}), cannot " +
                    "decide which one to navigate", result.size()));
        }
        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
    }


    /**
     * Handles the provided {@code eventInstance} and updates the underlying state machine.
     * <p>
     * This method creates an asynchronous task that looks for navigable transitions and moves the state machine to
     * the appropriate state.
     * <p>
     * Exceptions thrown from the computed {@link RuntimeAction}s are logged and ignored to ensure the bot is not
     * crashing because of an erroring action.
     *
     * @param eventInstance the {@link EventInstance} to handle
     * @param session       the {@link XatkitSession} used to define and access context variables
     */
    public void handleEventInstance(@NonNull EventInstance eventInstance, @NonNull XatkitSession session) {
        checkNotNull(session.getState(), "Cannot handle the %s %s, the provided %s's state hasn't been initialized",
                EventInstance.class.getSimpleName(), eventInstance, XatkitSession.class.getSimpleName());
        CompletableFuture.runAsync(() -> {
            State sessionState = session.getState();
            session.setEventInstance(eventInstance);
            Transition navigableTransition = getNavigableTransitions(sessionState, session);
            if (isNull(navigableTransition)) {
                /*
                 * Reset the event instance, we don't need it anymore and we don't want to corrupt future condition
                 * evaluations.
                 */
                session.setEventInstance(null);
                executeFallback(sessionState, session);
            } else {
                for (ContextInstance contextInstance : eventInstance.getOutContextInstances()) {
                    /*
                     * Register the context first: this allows to register context without parameters (e.g. follow-up
                     * contexts).
                     */
                    session.getRuntimeContexts().setContext(contextInstance.getDefinition().getName(),
                            contextInstance.getLifespanCount());
                    for (ContextParameterValue value : contextInstance.getValues()) {
                        session.getRuntimeContexts().setContextValue(value);
                    }
                }
                /*
                 * Store the event that triggered the rule execution in the session, it can be useful to some actions (e
                 * .g. analytics)
                 */
                session.setState(navigableTransition.getState());
                executeBody(navigableTransition.getState(), session);
            }
        }, executorService).exceptionally((throwable) -> {
            Log.error("An error occurred when running the actions associated to the event {0}. Check the logs for " +
                    "additional information", eventInstance.getDefinition().getName());
            /*
             * Print the stack trace even if it may have been printed before (e.g. in executeRuntimeAction): some
             * unexpected error may occur out of the executeRuntimeAction control flow (for example the creation of
             * the RuntimeAction itself).
             */
            printStackTrace(throwable);
            return null;
        });
    }


    /**
     * Prints the stack trace associated to the provided {@link Throwable}.
     *
     * @param e the {@link Throwable} to print the stack trace of
     */
    private void printStackTrace(Throwable e) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(baos, true);
        e.printStackTrace(printWriter);
        Log.error("{0}", baos.toString());
    }

    /**
     * Shuts down the underlying {@link ExecutorService}.
     * <p>
     * Shutting down the {@link ExecutionService} invalidates it and does not allow to process new
     * {@link RuntimeAction}s.
     */
    public void shutdown() {
        this.executorService.shutdownNow();
    }

    /**
     * Returns whether the {@link ExecutionService} is shutdown.
     *
     * @return whether the {@link ExecutionService} is shutdown
     */
    public boolean isShutdown() {
        return this.executorService.isShutdown();
    }
}

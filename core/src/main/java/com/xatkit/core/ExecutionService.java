package com.xatkit.core;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.platform.action.RuntimeActionResult;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.execution.State;
import com.xatkit.execution.Transition;
import com.xatkit.intent.ContextInstance;
import com.xatkit.intent.ContextParameterValue;
import com.xatkit.intent.EventInstance;
import com.xatkit.metamodels.utils.EventWrapper;
import com.xatkit.metamodels.utils.RuntimeModel;
import com.xatkit.util.ExecutionModelUtils;
import fr.inria.atlanmod.commons.log.Log;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ConfigurationConverter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.xbase.XBinaryOperation;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.eclipse.xtext.xbase.interpreter.IEvaluationResult;
import org.eclipse.xtext.xbase.interpreter.impl.XbaseInterpreter;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A service that handles {@link EventInstance}s and executes the corresponding {@link RuntimeAction}s defined in the
 * provided {@link ExecutionModel}.
 * <p>
 * This class defines Xatkit' execution logic: {@link RuntimeEventProvider}s typically call the
 * {@link #handleEventInstance(EventInstance, XatkitSession)} method to process a retrieved {@link EventInstance},
 * and trigger the {@link RuntimeAction}s that are associated to it in the {@link ExecutionModel}.
 * <p>
 * The {@link ExecutionService} is initialized by the {@link XatkitCore} instance, that loads the
 * {@link ExecutionModel} and initializes the {@link RuntimePlatformRegistry}.
 *
 * @see EventInstance
 * @see XatkitCore
 */
public class ExecutionService extends XbaseInterpreter {

    /**
     * The {@link XatkitSession} key used to store the matched event.
     * <p>
     * The value associated to this key is an {@link EventInstance}.
     */
    public static final String MATCHED_EVENT_SESSION_KEY = "xatkit.matched_event";

    /**
     * The {@link IEvaluationContext} key used to store and access the current {@link XatkitSession}.
     */
    private static final QualifiedName EVALUATION_CONTEXT_SESSION_KEY = QualifiedName.create("com.xatkit.session");

    /**
     * The {@link ExecutionModel} used to retrieve the {@link RuntimeAction}s to compute from the handled
     * {@link EventInstance}s.
     */
    @Getter
    private ExecutionModel executionModel;

    /**
     * The {@link RuntimePlatformRegistry} used to cache loaded {@link RuntimePlatform}s, and provides utility method
     * to retrieve, unregister, and clear them.
     * <p>
     * This instance is provided in the {@link ExecutionService} constructor, and is typically initialized by the
     * {@link XatkitCore} component.
     * <p>
     * TODO should we initialize the {@link RuntimePlatformRegistry} in this class instead of {@link XatkitCore}? (see
     * <a href="https://github.com/xatkit-bot-platform/xatkit/issues/155">#155</a>
     *
     * @see #getRuntimePlatformRegistry()
     */
    @Getter
    private RuntimePlatformRegistry runtimePlatformRegistry;

    /**
     * The Xatkit {@link Configuration}.
     */
    private Configuration configuration;

    /**
     * The {@link Map} representing the Xatkit {@link Configuration}.
     * <p>
     * This {@link Map} is initialized once when this class is constructed to avoid serialization overhead from
     * {@link Configuration} to {@link Map} every time an execution rule is triggered.
     */
    private Map<Object, Object> configurationMap;

    /**
     * The {@link ExecutorService} used to process {@link RuntimeAction}s.
     *
     * @see RuntimePlatform
     * @see RuntimeAction
     */
    @Getter
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Constructs a new {@link ExecutionService} based on the provided {@code executionModel} and {@code
     * runtimePlatformRegistry}.
     * <p>
     * This constructor also takes care of resolving all the proxies in the provided {@link ExecutionModel},
     * ensuring that concurrent accesses of the model will not produce unexpected behaviors (see
     * <a href="https://www.eclipse.org/forums/index.php/t/1095731/">this post</a>)
     *
     * @param executionModel          the {@link ExecutionModel} representing the intent-to-action bindings to use
     * @param runtimePlatformRegistry the {@link RuntimePlatformRegistry} used to create the {@link RuntimeAction}s
     *                                to execute
     * @param configuration           the Xatkit configuration
     * @throws NullPointerException if the provided {@code executionModel} or {@code runtimePlatformRegistry} is
     *                              {@code null}
     */
    public ExecutionService(@NonNull ExecutionModel executionModel,
                            @NonNull RuntimePlatformRegistry runtimePlatformRegistry,
                            @NonNull Configuration configuration) {
        this.executionModel = executionModel;
        this.runtimePlatformRegistry = runtimePlatformRegistry;
        this.configuration = configuration;
        this.configurationMap = ConfigurationConverter.getMap(configuration);
        /*
         * Resolve all the proxies in the Resource: this should remove concurrent read issues on the model (see
         * https://www.eclipse.org/forums/index.php/t/1095731/)
         */
        EcoreUtil.resolveAll(executionModel);
    }

    /**
     * Initializes the provided {@code session}'s {@link State} and executes it.
     * <p>
     * The provided {@link XatkitSession}'s {@link State} is set with the {@code Init} {@link State} of the bot
     * execution model, and the {@code Body} section of this {@link State} is directly executed.
     * <p>
     * Note that {@code context} and {@code parameters} are not set for the execution of the {@code Init} state. This
     * means that execution models accessing {@code context} information in the {@code Init} state will throw a
     * {@link NullPointerException}.
     *
     * @param session the {@link XatkitSession} to initialize
     * @throws NullPointerException if the provided {@code session} is {@code null}
     */
    public void initSession(@NonNull XatkitSession session) {
        session.setState(ExecutionModelUtils.getInitState(executionModel));
        this.executeBody(session.getState(), session);
    }

    /**
     * Executes the body of the provided {@code state}, using the provided {@code session}.
     * <p>
     * This method evaluates the content of the <i>body</i> section of the provided {@code state}, and throws a
     * {@link XatkitException} wrapping any exception thrown by the interpreted code. Once the body section has been
     * fully (and successfully) evaluated this method looks for wildcard transitions ({@code _ --> MyState}) to
     * navigate.
     * <p>
     * This method can be safely called on {@link State}s that do not define a body.
     *
     * @param state   the {@link State} to execute the body section of
     * @param session the {@link XatkitSession} holding the contextual information
     * @throws NullPointerException if the provided {@code state} or {@code session} is {@code null}
     */
    private void executeBody(@NonNull State state, @NonNull XatkitSession session) {
        XExpression bodyExpression = state.getBody();
        if (isNull(bodyExpression)) {
            Log.debug("{0}'s body section is null, skipping its execution", state.getName());
        } else {
            /*
             * The event instance that made the state machine move to the current state. This event instance is set
             * by the handleEventInstance method
             */
            EventInstance lastEventInstance = (EventInstance) session.get(MATCHED_EVENT_SESSION_KEY);
            IEvaluationContext evaluationContext = this.createXatkitEvaluationContext(lastEventInstance, session);
            IEvaluationResult evaluationResult = this.evaluate(bodyExpression, evaluationContext,
                    CancelIndicator.NullImpl);
            if (nonNull(evaluationResult.getException())) {
                Log.error(evaluationResult.getException(), "An error occurred when executing fallback of state {0}",
                        state.getName());
            }
            /*
             * Result is ignored here, we just want to know if it contains a Throwable.
             */
        }
        /*
         * Look for wildcard transitions and navigate them. We don't need to wait here, we can just move to the next
         * state that defines non-wildcard transitions.
         */
        Transition navigableTransition = getNavigableTransitions(null, state, session);
        if (nonNull(navigableTransition)) {
            /*
             * Reset the matched event instance.
             */
            session.store(MATCHED_EVENT_SESSION_KEY, null);
            session.setState(navigableTransition.getState());
            executeBody(navigableTransition.getState(), session);
        }
        /*
         * Don't do anything if there is no navigable transition, this means that we are waiting an event to
         * re-evaluate them.
         */
    }

    /**
     * Executes the fallback of the provided {@code state}, using the provided {@code session}.
     * <p>
     * The fallback of a {@link State} is executed when an event is received and the engine cannot
     * find any navigable {@link Transition}. If the provided {@code state} does not define a <i>fallback</i> section
     * the <i>body</i> of the <b>Default_Fallback</b> {@link State} is executed.
     *
     * @param state   the {@link State} to execute the fallback section of
     * @param session the {@link XatkitSession} holding the contextual information
     * @throws NullPointerException if the provided {@code state} or {@code session} is {@code null}
     */
    private void executeFallback(@NonNull State state, @NonNull XatkitSession session) {
        XExpression fallbackExpression = state.getFallback();
        if (isNull(fallbackExpression)) {
            executeBody(ExecutionModelUtils.getFallbackState(executionModel), session);
        } else {
            /*
             * The event instance that made the state machine move to the current state. This event instance is set
             * by the handleEventInstance method
             */
            EventInstance lastEventInstance = (EventInstance) session.get(MATCHED_EVENT_SESSION_KEY);
            IEvaluationContext evaluationContext = this.createXatkitEvaluationContext(lastEventInstance, session);
            IEvaluationResult evaluationResult = this.evaluate(fallbackExpression, evaluationContext,
                    CancelIndicator.NullImpl);
            if (nonNull(evaluationResult.getException())) {
                Log.error(evaluationResult.getException(), "An error occurred when executing fallback of state {0}",
                        state.getName());
            }
            /*
             * Result is ignored here, we just want to know if it contains a Throwable.
             */
        }
        /*
         * The fallback semantics implies that the execution engine stays in the same state. This means that we need
         * to increment all the context lifespans to be sure they will be available for the next intent recognition
         * (otherwise they will be deleted and the matching will be inconsistent).
         */
        session.getRuntimeContexts().incrementLifespanCounts();
    }

    /**
     * Creates an {@link IEvaluationContext} containing Xatkit-related variables.
     * <p>
     * The created {@link IEvaluationContext} holds the values bound to {@code event}, {@code intent}, {@code session
     * }, {@code context}, and {@code config} variables in the execution language.
     * <p>
     * The {@code event} and {@code intent} variables are set from the provided {@code eventInstance}, while the
     * other ones are retrieved from the {@code session} or Xatkit internals.
     * <p>
     * <b>Note</b>: the provided {@code eventInstance} can be {@code null} (e.g. when creating the
     * {@link IEvaluationContext} for {@link State}s accessed via a wildcard {@link Transition}).
     *
     * @param eventInstance the {@link EventInstance} to set in the context
     * @param session       the {@link XatkitSession} to set in the context
     * @return the created {@link IEvaluationContext}
     * @throws NullPointerException if the provided {@link XatkitSession} is {@code null}
     */
    private @NonNull
    IEvaluationContext createXatkitEvaluationContext(@Nullable EventInstance eventInstance,
                                                     @NonNull XatkitSession session) {
        IEvaluationContext evaluationContext = this.createContext();
        RuntimeModel runtimeModel = new RuntimeModel(session.getRuntimeContexts().getContextMap(),
                session.getSessionVariables(), configurationMap,
                eventInstance);
        evaluationContext.newValue(QualifiedName.create("this"), runtimeModel);
        evaluationContext.newValue(EVALUATION_CONTEXT_SESSION_KEY, session);
        return evaluationContext;
    }

    /**
     * Evaluates the provided {@code state}'s {@link Transition} and return the one that can be navigated.
     * <p>
     * This method checks, for all the {@link Transition}s, whether their condition is fulfilled or not. The
     * evaluation context of the conditions includes the provided {@code eventInstance} (bound to the {@code event
     * /intent} variable in the execution language, and the provided {@code session} (bound to the {@code session}
     * variable in the execution language).
     * <p>
     * This method cannot return more than one {@link Transition}. Multiple navigable transitions are considered
     * design issues making the bot behavior unreliable.
     *
     * @param eventInstance the {@link EventInstance} to use to check event mappings in the transitions' conditions
     * @param state         the current {@link State} to compute the navigable {@link Transition}s from
     * @param session       the {@link XatkitSession} holding the context information
     * @return the navigable {@link Transition} if it exists, {@code null} otherwise
     * @throws IllegalStateException if the evaluation of a {@link Transition}'s condition returns a non-boolean
     *                               value, or if more than 1 navigable transition is found
     * @throws NullPointerException  if the provided {@code state}, or {@code session} is
     *                               {@code null}
     */
    private @Nullable
    Transition getNavigableTransitions(@Nullable EventInstance eventInstance, @NonNull State state,
                                       @NonNull XatkitSession session) {
        /*
         * Use a list to store the navigable transitions so we can print a more useful error message is more than one
         *  is found.
         */
        List<Transition> result = new ArrayList<>();
        for (Transition t : state.getTransitions()) {
            if (t.isIsWildcard()) {
                /*
                 * The transition is a wildcard, it is always navigable.
                 */
                result.add(t);
                continue;
            }
            /*
             * Create the context with the received EventInstance. This is the instance we want to use in the
             * transition conditions.
             */
            IEvaluationContext evaluationContext = this.createXatkitEvaluationContext(eventInstance, session);
            IEvaluationResult evaluationResult = this.evaluate(t.getCondition(), evaluationContext,
                    CancelIndicator.NullImpl);
            if (nonNull(evaluationResult.getException())) {
                Log.error(evaluationResult.getException(), "An exception occurred when evaluating transition " +
                        "{0} of state {1}", state.getTransitions().indexOf(t), state.getName());
                /*
                 * We consider the transition as non-navigable if an exception is thrown while computing its condition.
                 */
                continue;
            } else {
                Object expressionValue = evaluationResult.getResult();
                if (expressionValue instanceof Boolean) {
                    if ((Boolean) expressionValue) {
                        result.add(t);
                    } else {
                        /*
                         * Skip, the transition condition is not fulfilled.
                         */
                    }
                } else {
                    /*
                     * The evaluated condition is not a boolean. This should not happen in the general case because the
                     * execution language grammar explicitly uses boolean operation rules to set transitions'
                     * conditions.
                     */
                    throw new IllegalStateException(MessageFormat.format("An error occurred when evaluating {0}'s {1}" +
                                    " conditions: expected a boolean value, found {2}", state.getName(),
                            Transition.class.getSimpleName(), expressionValue));
                }
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
     * Handles the provided {@code eventInstance} and executes the corresponding {@link RuntimeAction}s defined in the
     * {@link ExecutionModel}.
     * <p>
     * This method creates an asynchronous task that retrieves the {@link RuntimeAction}s to execute from the
     * {@link ExecutionModel}, and executes them sequentially. Note that all the {@link RuntimeAction}s are
     * executed in the same {@link Thread}, in order to ensure that their pre-conditions are respected (i.e. the
     * context variables defined by an action are available for the next ones).
     * <p>
     * Exceptions thrown from the computed {@link RuntimeAction}s are logged and ignored, so the calling
     * {@link RuntimeEventProvider} does not have to handle the exception, and can process the next event.
     * <p>
     * The created task also registers the output context values to the provided {@code session}, making them
     * available for the computed actions.
     *
     * @param eventInstance the {@link EventInstance} to handle
     * @param session       the {@link XatkitSession} used to define and access context variables
     * @throws NullPointerException if the provided {@code eventInstance} or {@code session} is {@code null}
     */
    public void handleEventInstance(@NonNull EventInstance eventInstance, @NonNull XatkitSession session) {
        checkNotNull(session.getState(), "Cannot handle the %s %s, the provided %s's state hasn't been initialized",
                EventInstance.class.getSimpleName(), eventInstance, XatkitSession.class.getSimpleName());
        CompletableFuture.runAsync(() -> {
            State sessionState = session.getState();

            Transition navigableTransition = getNavigableTransitions(eventInstance, sessionState, session);
            if (isNull(navigableTransition)) {
                session.store(MATCHED_EVENT_SESSION_KEY, eventInstance);
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
                session.store(MATCHED_EVENT_SESSION_KEY, eventInstance);
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
     * Evaluates the provided {@code expression}.
     * <p>
     * This methods delegates to the {@link XbaseInterpreter} the computation of all {@link XExpression}s, excepted
     * {@link XMemberFeatureCall}s representing platform's action call. For these specific expressions the
     * interpreter takes care of constructing the corresponding {@link RuntimeAction}, evaluates its parameters, and
     * invoke it.
     *
     * @param expression the {@link XExpression} to evaluate
     * @param context    the {@link IEvaluationContext} containing the information already computed during the
     *                   evaluation
     * @param indicator  the indicator
     * @return the result of the evaluation
     * @see #executeRuntimeAction(RuntimeAction)
     */
    @Override
    protected Object doEvaluate(XExpression expression, IEvaluationContext context, CancelIndicator indicator) {
        if (expression instanceof XMemberFeatureCall) {
            XMemberFeatureCall featureCall = (XMemberFeatureCall) expression;
            if (ExecutionModelUtils.isPlatformActionCall(featureCall, this.runtimePlatformRegistry)) {
                XatkitSession session = (XatkitSession) context.getValue(EVALUATION_CONTEXT_SESSION_KEY);
                List<Object> evaluatedArguments = new ArrayList<>();
                for (XExpression xExpression : featureCall.getActualArguments()) {
                    evaluatedArguments.add(internalEvaluate(xExpression, context, indicator));
                }
                RuntimeAction runtimeAction = this.getRuntimeActionFromXMemberFeatureCall(featureCall,
                        evaluatedArguments,
                        session);
                RuntimeActionResult result = executeRuntimeAction(runtimeAction);
                return result.getResult();
            }
        } else if (expression instanceof XBinaryOperation) {
            /*
             * Custom implementation of intent = MyIntent and event = MyEvent binary operations.
             */
            XBinaryOperation operation = (XBinaryOperation) expression;
            if (operation.getFeature().getSimpleName().equals("operator_equals")) {
                if (operation.getLeftOperand() instanceof XFeatureCall) {
                    XFeatureCall leftFeatureCall = (XFeatureCall) operation.getLeftOperand();
                    if (leftFeatureCall.getFeature() instanceof JvmField && (leftFeatureCall.getFeature().getSimpleName().equals("intent") || leftFeatureCall.getFeature().getSimpleName().equals("event"))) {
                        XFeatureCall rightFeatureCall = (XFeatureCall) operation.getRightOperand();
                        if (rightFeatureCall.getFeature() instanceof JvmGenericType) {
                            JvmGenericType rightFeatureType = (JvmGenericType) rightFeatureCall.getFeature();
                            if (rightFeatureType.getSuperTypes().stream().anyMatch(t -> t.getIdentifier().equals("com" +
                                    ".xatkit.intent.EventDefinition"))) {
                                EventWrapper wrapper = (EventWrapper) internalEvaluate(operation.getLeftOperand(),
                                        context, indicator);
                                if (isNull(wrapper) || isNull(wrapper.getEventInstance())) {
                                    /*
                                     * The wrapper can be null if use the "intent" accessor on a received event. In
                                     * this case the result of the comparison is always false.
                                     * The wrapper does not contain any event if we are evaluating a transition
                                     * before receiving an event. This happens after state's body execution, when the
                                     *  engine tries to evaluate transitions that can be directly navigated (e.g.
                                     * context-based transitions).
                                     */
                                    return false;
                                } else {
                                     return wrapper.getEventInstance().getDefinition().getName().equals(rightFeatureType.getSimpleName());
                                }
                            }
                        }
                    }

                }
            }
        }
        return super.doEvaluate(expression, context, indicator);
    }

    /**
     * Executes the provided {@code action}.
     * <p>
     * This method executes the provided {@link RuntimeAction} in the calling {@link Thread}, and will block the
     * execution until the action completes. This method is called sequentially by the
     * {@link #handleEventInstance(EventInstance, XatkitSession)} method, that wraps all the computation in a single
     * asynchronous task.
     *
     * @param action the {@link RuntimeAction} to execute
     * @throws NullPointerException if the provided {@code action} is {@code null}
     */
    private RuntimeActionResult executeRuntimeAction(@NonNull RuntimeAction action) {
        RuntimeActionResult result = action.call();
        if (result.isError()) {
            Log.error("An error occurred when executing the action {0}", action.getClass().getSimpleName());
            printStackTrace(result.getThrowable());
        }
        Log.info("Action {0} executed in {1} ms", action.getClass().getSimpleName(), result.getExecutionTime());
        return result;
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
     * Constructs a {@link RuntimeAction} instance corresponding to the provided {@code actionCall}, initialized
     * with the provided {@code arguments} and {@code session}.
     * <p>
     * This method is used as a bridge between the {@link XMemberFeatureCall}s (from the execution model), and the
     * {@link RuntimeAction}s (from the internal Xatkit execution engine).
     *
     * @param actionCall the {@link XMemberFeatureCall} representing the {@link RuntimeAction} to create
     * @param arguments  the {@link List} of computed values used as arguments for the created {@link RuntimeAction}
     * @param session    the {@link XatkitSession} associated to the action
     * @return the constructed {@link RuntimeAction}
     */
    private RuntimeAction getRuntimeActionFromXMemberFeatureCall(XMemberFeatureCall actionCall, List<Object> arguments,
                                                                 XatkitSession session) {
        String platformName = ExecutionModelUtils.getPlatformName(actionCall);
        RuntimePlatform runtimePlatform = this.getRuntimePlatformRegistry().getRuntimePlatform(platformName);
        return runtimePlatform.createRuntimeAction(actionCall, arguments, session);
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

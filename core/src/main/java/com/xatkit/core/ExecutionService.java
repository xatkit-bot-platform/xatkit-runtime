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
import com.xatkit.util.ExecutionModelHelper;
import fr.inria.atlanmod.commons.log.Log;
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

import javax.annotation.Nonnull;
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
    public ExecutionService(ExecutionModel executionModel, RuntimePlatformRegistry runtimePlatformRegistry,
                            Configuration configuration) {
        checkNotNull(executionModel, "Cannot construct a %s from the provided %s %s", this.getClass()
                .getSimpleName(), ExecutionModel.class.getSimpleName(), executionModel);
        checkNotNull(runtimePlatformRegistry, "Cannot construct a %s from the provided %s %s", this.getClass()
                .getSimpleName(), RuntimePlatformRegistry.class.getSimpleName(), runtimePlatformRegistry);
        checkNotNull(configuration, "Cannot construct a %s from the provided %s %s", this.getClass().getSimpleName(),
                Configuration.class.getSimpleName(), configuration);
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
     * Returns the {@link ExecutorService} used to process {@link RuntimeAction}s.
     * <p>
     * <b>Note:</b> this method is designed to ease testing, and should not be accessed by client applications.
     * Manipulating {@link XatkitCore}'s {@link ExecutorService} may create consistency issues on currently executed
     * {@link RuntimeAction}s.
     *
     * @return the {@link ExecutorService} used to process {@link RuntimeAction}s
     */
    protected ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Returns the {@link ExecutionModel} associated to this {@link ExecutionService}.
     *
     * @return the {@link ExecutionModel} associated to this {@link ExecutionService}
     */
    public ExecutionModel getExecutionModel() {
        return executionModel;
    }

    /**
     * Returns the {@link RuntimePlatformRegistry} associated to this {@link ExecutionService}.
     *
     * @return the {@link RuntimePlatformRegistry} associated to this {@link ExecutionService}
     */
    public RuntimePlatformRegistry getRuntimePlatformRegistry() {
        return runtimePlatformRegistry;
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
    public void initSession(@Nonnull XatkitSession session) {
        checkNotNull(session, "Cannot initialize the provided %s %s", XatkitSession.class.getSimpleName(), session);
        session.setState(ExecutionModelHelper.getInstance().getInitState());
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
    private void executeBody(@Nonnull State state, @Nonnull XatkitSession session) {
        checkNotNull(state, "Cannot execute the body of the provided %s %s", State.class.getSimpleName(), state);
        checkNotNull(session, "Cannot execute the body of %s %s with the provided %s %s", State.class.getSimpleName()
                , state.getName(), XatkitSession.class.getSimpleName(), session);
        XExpression bodyExpression = state.getBody();
        if (isNull(bodyExpression)) {
            Log.debug("{0}'s body section is null, skipping its execution", state.getName());
        } else {
            IEvaluationContext evaluationContext = this.createXatkitEvaluationContext(null, session);
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
        State stateReachableWithWildcard = ExecutionModelHelper.getInstance().getStateReachableWithWildcard(state);
        if(nonNull(stateReachableWithWildcard)) {
            session.setState(stateReachableWithWildcard);
            executeBody(stateReachableWithWildcard, session);
        }
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
    private void executeFallback(@Nonnull State state, @Nonnull XatkitSession session) {
        checkNotNull(state, "Cannot execute the fallback of the provided %s %s", State.class.getSimpleName(), state);
        checkNotNull(session, "Cannot execute the fallback of %s %s with the provided %s %s",
                State.class.getSimpleName()
                , state.getName(), XatkitSession.class.getSimpleName(), session);
        XExpression fallbackExpression = state.getFallback();
        if (isNull(fallbackExpression)) {
            executeBody(ExecutionModelHelper.getInstance().getFallbackState(), session);
        } else {
            IEvaluationContext evaluationContext = this.createXatkitEvaluationContext(null, session);
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
    private @Nonnull
    IEvaluationContext createXatkitEvaluationContext(@Nullable EventInstance eventInstance,
                                                     @Nonnull XatkitSession session) {
        checkNotNull(session, "Cannot create the %s from the provided %s %s",
                IEvaluationContext.class.getSimpleName(), XatkitSession.class.getSimpleName(), session);
        IEvaluationContext evaluationContext = this.createContext();
        RuntimeModel runtimeModel = new RuntimeModel(session.getRuntimeContexts().getContextMap(),
                session.getSessionVariables(), configurationMap,
                eventInstance);
        evaluationContext.newValue(QualifiedName.create("this"), runtimeModel);
        evaluationContext.newValue(EVALUATION_CONTEXT_SESSION_KEY, session);
        return evaluationContext;
    }

    /**
     * Computes the {@link List} of {@link Transition}s that can be navigated from the provided {@code state}.
     * <p>
     * This method checks, for all the {@link Transition}s, whether their condition is fulfilled or not. The
     * evaluation context of the conditions includes the provided {@code eventInstance} (bound to the {@code event
     * /intent} variable in the execution language, and the provided {@code session} (bound to the {@code session}
     * variable in the execution language).
     * <p>
     * <b>Note</b>: this method should <b>not</b> return more than one transition. Multiple navigable transitions
     * generally indicate an issue in the bot design.
     *
     * @param eventInstance the {@link EventInstance} to use to check event mappings in the transitions' conditions
     * @param state         the current {@link State} to compute the navigable {@link Transition}s from
     * @param session       the {@link XatkitSession} holding the context information
     * @return the {@link List} of navigable {@link Transition}s
     * @throws XatkitException      if the evaluation of a {@link Transition}'s condition returns a non-boolean value
     * @throws NullPointerException if the provided {@code eventInstance}, {@code state}, or {@code session} is
     *                              {@code null}
     */
    private List<Transition> getNavigableTransitions(EventInstance eventInstance, State state, XatkitSession session) {
        checkNotNull(eventInstance, "Cannot compute the navigable transitions from the provided %s %s",
                EventInstance.class.getSimpleName());
        checkNotNull(state, "Cannot compute the navigable transitions from the provided %s %s",
                State.class.getSimpleName(), state);
        checkNotNull(session, "Cannot compute the navigable transitions from the provided %s %s",
                XatkitSession.class.getSimpleName(), session);
        List<Transition> result = new ArrayList<>();
        for (Transition t : state.getTransitions()) {
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
                    throw new XatkitException(MessageFormat.format("An error occurred when evaluating {0}'s {1} " +
                                    "conditions: expected a boolean value, found {2}", state.getName(),
                            Transition.class.getSimpleName(), expressionValue));
                }
            }
        }
        return result;
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
    public void handleEventInstance(EventInstance eventInstance, XatkitSession session) {
        checkNotNull(eventInstance, "Cannot handle the %s %s", EventInstance.class.getSimpleName(), eventInstance);
        checkNotNull(session, "Cannot handle the %s %s", XatkitSession.class.getSimpleName(), session);
        checkNotNull(session.getState(), "Cannot handle the %s %s, the provided %s's state hasn't been initialized",
                EventInstance.class.getSimpleName(), eventInstance, XatkitSession.class.getSimpleName());
        CompletableFuture.runAsync(() -> {
            State sessionState = session.getState();

            List<Transition> transitions = getNavigableTransitions(eventInstance, sessionState, session);
            if (transitions.size() > 1) {
                throw new XatkitException(MessageFormat.format("Found several navigable transitions ({0}), cannot " +
                        "decide which one to navigate", transitions.size()));
                /*
                 * TODO Here we are blocked, we need an error state or something
                 */
            } else if (transitions.size() == 0) {
                executeFallback(session.getState(), session);
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
                session.setState(transitions.get(0).getState());
                executeBody(transitions.get(0).getState(), session);
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
            if (ExecutionModelHelper.getInstance().isPlatformActionCall(featureCall, this.runtimePlatformRegistry)) {
                XatkitSession session = (XatkitSession) context.getValue(EVALUATION_CONTEXT_SESSION_KEY);
                List<Object> evaluatedArguments = new ArrayList<>();
                for (XExpression xExpression : featureCall.getActualArguments()) {
                    evaluatedArguments.add(internalEvaluate(xExpression, context, indicator));
                }
                RuntimeAction runtimeAction = this.getRuntimeActionFromXMemberFeatureCall(featureCall,
                        evaluatedArguments,
                        session);
                RuntimeActionResult result = executeRuntimeAction(runtimeAction);
                if (!session.equals(runtimeAction.getSession())) {
                    // TODO update the documentation
                    /*
                     * The runtimeAction.getSession can be different if the action changed its own session. This is the
                     * case for messaging actions that need to create a session associated to the targeted channel.
                     * An example of such behavior:
                     * on GithubEvent do
                     *  SlackPlatform.PostMessage("text", "channel")
                     * The rule session is created from the GithubEvent, but another session is created to invoke the
                     * PostMessage action.
                     */
                    context.assignValue(EVALUATION_CONTEXT_SESSION_KEY, runtimeAction.getSession());
                    RuntimeModel runtimeModel = (RuntimeModel) context.getValue(QualifiedName.create("this"));
                    /*
                     * Update the RuntimeModel's session to bind the new session used by the action. This is required
                     * to store values in the new session that will be accessible when the session is retrieved from
                     * another rule.
                     * Example:
                     * on GithubEvent do
                     *  SlackPlatform.PostMessage("test", "channel")
                     *  session.put("abc", "def")
                     * on Intent do (from channel)
                     *  session.get("abc")
                     * Here the channel's session should contain the key "abc", it cannot be the case if we don't
                     * switch the session from the RuntimeModel.
                     */
                    runtimeModel.setSession(runtimeAction.getSession().getSessionVariables());
                }
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
                                if(isNull(wrapper)) {
                                    /*
                                     * The wrapper can be null if use the "intent" accessor on a received event. In
                                     * this case the result of the comparison is always false.
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
    private RuntimeActionResult executeRuntimeAction(RuntimeAction action) {
        checkNotNull(action, "Cannot execute the provided %s %s", RuntimeAction.class.getSimpleName(), action);
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
        String platformName = ExecutionModelHelper.getInstance().getPlatformName(actionCall);
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

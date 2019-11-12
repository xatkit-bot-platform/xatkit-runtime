package com.xatkit.core;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.platform.action.RuntimeActionResult;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.execution.ExecutionRule;
import com.xatkit.intent.ContextInstance;
import com.xatkit.intent.ContextParameterValue;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.EventInstance;
import com.xatkit.metamodels.utils.RuntimeModel;
import com.xatkit.util.XbaseUtils;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ConfigurationConverter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.eclipse.xtext.xbase.interpreter.IEvaluationResult;
import org.eclipse.xtext.xbase.interpreter.impl.XbaseInterpreter;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
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
     * @see #executeExecutionRule(ExecutionRule, XatkitSession)
     */
    public void handleEventInstance(EventInstance eventInstance, XatkitSession session) {
        checkNotNull(eventInstance, "Cannot handle the %s %s", EventInstance.class.getSimpleName(), eventInstance);
        checkNotNull(session, "Cannot handle the %s %s", XatkitSession.class.getSimpleName(), session);
        CompletableFuture.runAsync(() -> {
            /*
             * Register the returned context values
             */
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
            List<ExecutionRule> executionRules = this.getExecutionRulesFromEvent(eventInstance);
            for (ExecutionRule rule : executionRules) {
                executeExecutionRule(rule, session);
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
     * Sets the evaluation context associated to the provided {@code executionRule} and delegates its evaluation.
     * <p>
     * This method also creates the {@link RuntimeModel} instance that will be used as {@code this} by the interpreter.
     *
     * @param executionRule the {@link ExecutionRule} to execute
     * @param session       the {@link XatkitSession} associated to the {@link ExecutionRule}
     * @see #evaluate(XExpression, IEvaluationContext, CancelIndicator)
     */
    private void executeExecutionRule(ExecutionRule executionRule, XatkitSession session) {
        IEvaluationContext evaluationContext = this.createContext();
        RuntimeModel runtimeModel = new RuntimeModel(session.getRuntimeContexts().getContextMap(),
                session.getSessionVariables(), configurationMap,
                (EventInstance) session.get(MATCHED_EVENT_SESSION_KEY));
        evaluationContext.newValue(QualifiedName.create("this"), runtimeModel);
        evaluationContext.newValue(EVALUATION_CONTEXT_SESSION_KEY, session);
        IEvaluationResult evaluationResult = this.evaluate(executionRule, evaluationContext, CancelIndicator.NullImpl);
        if (nonNull(evaluationResult.getException())) {
            throw new XatkitException(evaluationResult.getException());
        }
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
            if (XbaseUtils.isPlatformActionCall(featureCall, this.runtimePlatformRegistry)) {
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
            printStackTrace(result.getThrownException());
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
        String platformName = XbaseUtils.getPlatformName(actionCall);
        RuntimePlatform runtimePlatform = this.getRuntimePlatformRegistry().getRuntimePlatform(platformName);
        return runtimePlatform.createRuntimeAction(actionCall, arguments, session);
    }

    /**
     * Retrieves the {@link ExecutionRule}s associated to the provided {@code eventInstance}.
     * <p>
     * This method navigates the underlying {@link ExecutionModel} and retrieves all the {@link ExecutionRule}s that
     * match the {@link EventDefinition} of the provided {@code eventInstance}. Note that {@link ExecutionRule}s may
     * be returned in any order.
     *
     * @param eventInstance the {@link EventInstance} to retrieve the {@link ExecutionRule}s from
     * @return a {@link List} containing the retrieved {@link ExecutionRule}s
     * @see #executeExecutionRule(ExecutionRule, XatkitSession)
     */
    private List<ExecutionRule> getExecutionRulesFromEvent(EventInstance eventInstance) {
        EventDefinition eventDefinition = eventInstance.getDefinition();
        List<ExecutionRule> result = new ArrayList<>();
        for (ExecutionRule rule : executionModel.getExecutionRules()) {
            if (rule.getEvent().getName().equals(eventDefinition.getName())) {
                if(nonNull(rule.getFromPlatform())) {
                    if(rule.getFromPlatform().getName().equals(eventInstance.getTriggeredBy())) {
                        result.add(rule);
                    } else {
                        /*
                         * Do nothing, we don't want to execute this rule because its from guard is not matched.
                         * (keep the continue in case the method code evolves)
                         */
                        continue;
                    }
                } else {
                    result.add(rule);
                }
            }
        }
        return result;
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

package edu.uoc.som.jarvis.core;

import edu.uoc.som.jarvis.common.VariableDeclaration;
import edu.uoc.som.jarvis.core.interpreter.CommonInterpreter;
import edu.uoc.som.jarvis.core.interpreter.ExecutionContext;
import edu.uoc.som.jarvis.core.platform.RuntimePlatform;
import edu.uoc.som.jarvis.core.platform.action.RuntimeAction;
import edu.uoc.som.jarvis.core.platform.action.RuntimeActionResult;
import edu.uoc.som.jarvis.core.platform.io.RuntimeEventProvider;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.execution.ActionInstance;
import edu.uoc.som.jarvis.execution.ExecutionModel;
import edu.uoc.som.jarvis.execution.ExecutionRule;
import edu.uoc.som.jarvis.intent.ContextInstance;
import edu.uoc.som.jarvis.intent.ContextParameterValue;
import edu.uoc.som.jarvis.intent.EventDefinition;
import edu.uoc.som.jarvis.intent.EventInstance;
import edu.uoc.som.jarvis.platform.PlatformDefinition;
import fr.inria.atlanmod.commons.log.Log;
import org.eclipse.emf.ecore.util.EcoreUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

/**
 * A service that handles {@link EventInstance}s and executes the corresponding {@link RuntimeAction}s defined in the
 * provided {@link ExecutionModel}.
 * <p>
 * This class defines Jarvis' execution logic: {@link RuntimeEventProvider}s typically call the
 * {@link #handleEventInstance(EventInstance, JarvisSession)} method to process a retrieved {@link EventInstance},
 * and trigger the {@link RuntimeAction}s that are associated to it in the {@link ExecutionModel}.
 * <p>
 * The {@link ExecutionService} is initialized by the {@link JarvisCore} instance, that loads the
 * {@link ExecutionModel} and initializes the {@link RuntimePlatformRegistry}.
 *
 * @see ActionInstance
 * @see EventInstance
 * @see JarvisCore
 */
public class ExecutionService {

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
     * {@link JarvisCore} component.
     * <p>
     * TODO should we initialize the {@link RuntimePlatformRegistry} in this class instead of {@link JarvisCore}? (see
     * <a href="https://github.com/gdaniel/jarvis/issues/155">#155</a>
     *
     * @see #getRuntimePlatformRegistry()
     */
    private RuntimePlatformRegistry runtimePlatformRegistry;

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
     * @throws NullPointerException if the provided {@code executionModel} or {@code runtimePlatformRegistry} is
     *                              {@code null}
     */
    public ExecutionService(ExecutionModel executionModel, RuntimePlatformRegistry runtimePlatformRegistry) {
        checkNotNull(executionModel, "Cannot construct a %s from the provided %s %s", this.getClass()
                .getSimpleName(), ExecutionModel.class.getSimpleName(), executionModel);
        checkNotNull(runtimePlatformRegistry, "Cannot construct a %s from the provided %s %s", this.getClass()
                .getSimpleName(), RuntimePlatformRegistry.class.getSimpleName(), runtimePlatformRegistry);
        this.executionModel = executionModel;
        this.runtimePlatformRegistry = runtimePlatformRegistry;
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
     * Manipulating {@link JarvisCore}'s {@link ExecutorService} may create consistency issues on currently executed
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
     * Handles the provided {@code eventInstance} and executed the corresponding {@link RuntimeAction}s defined in the
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
     * @param session       the {@link JarvisSession} used to define and access context variables
     * @throws NullPointerException if the provided {@code eventInstance} or {@code session} is {@code null}
     * @see #executeExecutionRule(ExecutionRule, JarvisSession)
     */
    public void handleEventInstance(EventInstance eventInstance, JarvisSession session) {
        checkNotNull(eventInstance, "Cannot handle the %s %s", EventInstance.class.getSimpleName(), eventInstance);
        checkNotNull(session, "Cannot handle the %s %s", JarvisSession.class.getSimpleName(), session);
        CompletableFuture.runAsync(() -> {
            /*
             * Register the returned context values
             */
            for (ContextInstance contextInstance : eventInstance.getOutContextInstances()) {
                for (ContextParameterValue value : contextInstance.getValues()) {
                    session.getRuntimeContexts().setContextValue(value);
                }
            }
            List<ExecutionRule> executionRules = this.getExecutionRulesFromEvent(eventInstance);
            for (ExecutionRule rule : executionRules) {
                executeExecutionRule(rule, session);
            }
        }, executorService).exceptionally((throwable) -> {
            Log.error("An error occurred when running the actions associated to {0}: {1} {2}", eventInstance
                    .getDefinition().getName(), throwable.getClass().getSimpleName(), throwable.getMessage());
            Log.error(throwable);
            return null;
        });
    }

    /**
     * Executes <b>synchronously</b> the actions defined in the provided {@code executionRule}.
     * <p>
     * This method creates a new {@link ExecutionContext} used to evaluate all the expressions within the provided
     * {@code executionRule}. This {@link ExecutionContext} ensures that scoping is preserved, and that expressions
     * in the provided {@code executionRule} cannot access variables that are defined in another {@link ExecutionRule}.
     *
     * @param executionRule the {@link ExecutionRule} to execute
     * @param session       the {@link JarvisSession} used to define and access the rule's context variables
     * @see #executeRuntimeAction(RuntimeAction, ActionInstance, JarvisSession, ExecutionContext)
     */
    private void executeExecutionRule(ExecutionRule executionRule, JarvisSession session) {
        ExecutionContext context = new ExecutionContext();
        context.setSession(session);
        for (VariableDeclaration variable : executionRule.getVariables()) {
            CommonInterpreter.getInstance().compute(variable, context);
        }
        for (ActionInstance actionInstance : executionRule.getActions()) {
            RuntimeAction action = getRuntimeActionFromActionInstance(actionInstance, session, context);
            executeRuntimeAction(action, actionInstance, session, context);
        }
    }

    /**
     * Executes the provided {@code action} with the provided {@code session}.
     * <p>
     * This method executes the provided {@link RuntimeAction} in the calling {@link Thread}, and will block the
     * execution until the action completes. This method is called sequentially by the
     * {@link #handleEventInstance(EventInstance, JarvisSession)} method, that wraps all the computation in a single
     * asynchronous task.
     * <p>
     * This method processes the {@link RuntimeActionResult} returned by the computed {@code action}. If the {@code
     * action} threw an exception an error message is logged and the {@code onError} {@link ActionInstance}s are
     * retrieved and executed. If the {@code action} terminated successfully the corresponding context variables are
     * set and the {@code onSuccess} {@link ActionInstance}s are retrieved and executed.
     *
     * @param action         the {@link RuntimeAction} to execute
     * @param actionInstance the {@link ActionInstance} representing the signature of the {@link RuntimeAction} to
     *                       execute
     * @param session        the {@link JarvisSession} used to define and access the context variables associated to the
     *                       provided {@code action}
     * @throws NullPointerException if the provided {@code action} or {@code session} is {@code null}
     */
    private void executeRuntimeAction(RuntimeAction action, ActionInstance actionInstance, JarvisSession session,
                                      ExecutionContext context) {
        checkNotNull(action, "Cannot execute the provided %s %s", RuntimeAction.class.getSimpleName(), action);
        checkNotNull(session, "Cannot execute the provided %s with the provided %s %s", RuntimeAction.class
                .getSimpleName(), JarvisSession.class.getSimpleName(), session);
        RuntimeActionResult result = action.call();
        if (result.isError()) {
            Log.error("An error occurred when executing the action {0}: {1} {2}", action.getClass().getSimpleName
                    (), result.getThrownException().getClass().getSimpleName(), result.getThrownException()
                    .getMessage());
            /*
             * Retrieve the ActionInstances to execute when the computed ActionInstance returns an error and execute
             * them.
             */
            for (ActionInstance onErrorActionInstance : actionInstance.getOnError()) {
                Log.info("Executing fallback action {0}", onErrorActionInstance.getAction().getName());
                RuntimeAction onErrorRuntimeAction = getRuntimeActionFromActionInstance(onErrorActionInstance,
                        session, context);
                executeRuntimeAction(onErrorRuntimeAction, onErrorActionInstance, session, context);
            }
        } else {
            if (nonNull(action.getReturnVariable())) {
                Log.info("Registering context variable {0} with value {1}", action.getReturnVariable(), result);
                session.getRuntimeContexts().setContextValue("variables", 1, action.getReturnVariable(),
                        result.getResult());
            }
        }
        Log.info("Action {0} executed in {1} ms", action.getClass().getSimpleName(), result.getExecutionTime());
    }

    /**
     * Constructs a {@link RuntimeAction} instance corresponding to the provided {@code actionInstance}, initialized
     * in the provided {@code session}.
     * <p>
     * This method is used as a bridge between the {@link ActionInstance}s (from the execution model), and the
     * {@link RuntimeAction}s (from the internal Jarvis execution engine).
     *
     * @param actionInstance the {@link ActionInstance} to construct a {@link RuntimeAction} from
     * @param session        the {@link JarvisSession} used to define and access context variables
     * @return the constructed {@link RuntimeAction}
     */
    private RuntimeAction getRuntimeActionFromActionInstance(ActionInstance actionInstance, JarvisSession session,
                                                             ExecutionContext context) {
        RuntimePlatform runtimePlatform = this.getRuntimePlatformRegistry().getRuntimePlatform((PlatformDefinition)
                actionInstance.getAction().eContainer());
        return runtimePlatform.createRuntimeAction(actionInstance, session, context);
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
     *
     * @see #executeExecutionRule(ExecutionRule, JarvisSession)
     */
    private List<ExecutionRule> getExecutionRulesFromEvent(EventInstance eventInstance) {
        EventDefinition eventDefinition = eventInstance.getDefinition();
        List<ExecutionRule> result = new ArrayList<>();
        for (ExecutionRule rule : executionModel.getExecutionRules()) {
            if (rule.getEvent().getName().equals(eventDefinition.getName())) {
                result.add(rule);
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

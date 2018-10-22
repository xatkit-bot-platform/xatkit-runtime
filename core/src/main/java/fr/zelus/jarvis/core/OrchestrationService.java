package fr.zelus.jarvis.core;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.ContextInstance;
import fr.zelus.jarvis.intent.ContextParameterValue;
import fr.zelus.jarvis.intent.EventDefinition;
import fr.zelus.jarvis.intent.EventInstance;
import fr.zelus.jarvis.module.Module;
import fr.zelus.jarvis.orchestration.ActionInstance;
import fr.zelus.jarvis.orchestration.OrchestrationLink;
import fr.zelus.jarvis.orchestration.OrchestrationModel;
import org.eclipse.emf.ecore.util.EcoreUtil;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

/**
 * A service that handles {@link EventInstance}s and executes the corresponding {@link JarvisAction}s defined in the
 * provided {@link OrchestrationModel}.
 * <p>
 * This class defines Jarvis' execution logic: {@link fr.zelus.jarvis.io.EventProvider}s typically call the
 * {@link #handleEventInstance(EventInstance, JarvisSession)} method to process a retrieved {@link EventInstance},
 * and trigger the {@link JarvisAction}s that are associated to it in the {@link OrchestrationModel}.
 * <p>
 * The {@link OrchestrationService} is initialized by the {@link JarvisCore} instance, that loads the
 * {@link OrchestrationModel} and initializes the {@link JarvisModuleRegistry}.
 *
 * @see ActionInstance
 * @see EventInstance
 * @see JarvisCore
 */
public class OrchestrationService {

    /**
     * The {@link OrchestrationModel} used to retrieve the {@link JarvisAction}s to compute from the handled
     * {@link EventInstance}s.
     */
    private OrchestrationModel orchestrationModel;

    /**
     * The {@link JarvisModuleRegistry} used to cache loaded modules, and provides utility method to retrieve,
     * unregister, and clear them.
     * <p>
     * This instance is provided in the {@link OrchestrationService} constructor, and is typically initialized by the
     * {@link JarvisCore} component.
     * <p>
     * TODO should we initialize the {@link JarvisModuleRegistry} in this class instead of {@link JarvisCore}? (see
     * <a href="https://github.com/gdaniel/jarvis/issues/155">#155</a>
     *
     * @see #getJarvisModuleRegistry()
     */
    private JarvisModuleRegistry jarvisModuleRegistry;

    /**
     * The {@link ExecutorService} used to process {@link JarvisAction}s.
     *
     * @see JarvisModule
     * @see JarvisAction
     */
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Constructs a new {@link OrchestrationService} based on the provided {@code orchestrationModel} and {@code
     * jarvisModuleRegistry}.
     * <p>
     * This constructor also takes care of resolving all the proxies in the provided {@link OrchestrationModel},
     * ensuring that concurrent accesses of the model will not produce unexpected behaviors (see
     * <a href="https://www.eclipse.org/forums/index.php/t/1095731/">this post</a>)
     *
     * @param orchestrationModel   the {@link OrchestrationModel} representing the intent-to-action bindings to use
     * @param jarvisModuleRegistry the {@link JarvisModuleRegistry} used to create the {@link JarvisAction}s to execute
     * @throws NullPointerException if the provided {@code orchestrationModel} or {@code jarvisModuleRegistry} is
     *                              {@code null}
     */
    public OrchestrationService(OrchestrationModel orchestrationModel, JarvisModuleRegistry jarvisModuleRegistry) {
        checkNotNull(orchestrationModel, "Cannot construct a %s from the provided %s %s", this.getClass()
                .getSimpleName(), OrchestrationModel.class.getSimpleName(), orchestrationModel);
        checkNotNull(jarvisModuleRegistry, "Cannot construct a %s from the provided %s %s", this.getClass()
                .getSimpleName(), JarvisModuleRegistry.class.getSimpleName(), jarvisModuleRegistry);
        this.orchestrationModel = orchestrationModel;
        this.jarvisModuleRegistry = jarvisModuleRegistry;
        /*
         * Resolve all the proxies in the Resource: this should remove concurrent read issues on the model (see
         * https://www.eclipse.org/forums/index.php/t/1095731/)
         */
        EcoreUtil.resolveAll(orchestrationModel);
    }

    /**
     * Returns the {@link ExecutorService} used to process {@link JarvisAction}s.
     * <p>
     * <b>Note:</b> this method is designed to ease testing, and should not be accessed by client applications.
     * Manipulating {@link JarvisCore}'s {@link ExecutorService} may create consistency issues on currently executed
     * {@link JarvisAction}s.
     *
     * @return the {@link ExecutorService} used to process {@link JarvisAction}s
     */
    protected ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Returns the {@link OrchestrationModel} associated to this {@link OrchestrationService}.
     *
     * @return the {@link OrchestrationModel} associated to this {@link OrchestrationService}
     */
    public OrchestrationModel getOrchestrationModel() {
        return orchestrationModel;
    }

    /**
     * Returns the {@link JarvisModuleRegistry} associated to this {@link OrchestrationService}.
     *
     * @return the {@link JarvisModuleRegistry} associated to this {@link OrchestrationService}
     */
    public JarvisModuleRegistry getJarvisModuleRegistry() {
        return jarvisModuleRegistry;
    }

    /**
     * Handles the provided {@code eventInstance} and executed the corresponding {@link JarvisAction}s defined in the
     * {@link OrchestrationModel}.
     * <p>
     * This method creates an asynchronous task that retrieves the {@link JarvisAction}s to execute from the
     * {@link OrchestrationModel}, and executes them sequentially. Note that all the {@link JarvisAction}s are
     * executed in the same {@link Thread}, in order to ensure that their pre-conditions are respected (i.e. the
     * context variables defined by an action are available for the next ones).
     * <p>
     * Exceptions thrown from the computed {@link JarvisAction}s are logged and ignored, so the calling
     * {@link fr.zelus.jarvis.io.EventProvider} does not have to handle the exception, and can process the next event.
     * <p>
     * The created task also registers the output context values to the provided {@code session}, making them
     * available for the computed actions.
     *
     * @param eventInstance the {@link EventInstance} to handle
     * @param session       the {@link JarvisSession} used to define and access context variables
     * @throws NullPointerException if the provided {@code eventInstance} or {@code session} is {@code null}
     * @see #executeJarvisAction(JarvisAction, ActionInstance, JarvisSession)
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
                    session.getJarvisContext().setContextValue(value);
                }
            }
            List<ActionInstance> actionInstances = this.getActionsFromEvent(eventInstance);
            if (actionInstances.isEmpty()) {
                Log.warn("The intent {0} is not associated to any action", eventInstance.getDefinition().getName());
            }
            for (ActionInstance actionInstance : actionInstances) {
                JarvisAction action = getJarvisActionFromActionInstance(actionInstance, session);
                executeJarvisAction(action, actionInstance, session);
            }
        }, executorService).exceptionally((throwable) -> {
            Log.error("An error occurred when running the actions associated to {0}: {1} {2}", eventInstance
                    .getDefinition().getName(), throwable.getClass().getSimpleName(), throwable.getMessage());
            return null;
        });
    }

    /**
     * Executes <b>synchronously</b> the provided {@code action} with the provided {@code session}.
     * <p>
     * This method executes the provided {@link JarvisAction} in the calling {@link Thread}, and will block the
     * execution until the action completes. This method is called sequentially by the
     * {@link #handleEventInstance(EventInstance, JarvisSession)} method, that wraps all the computation in a single
     * asynchronous task.
     * <p>
     * This method processes the {@link JarvisActionResult} returned by the computed {@code action}. If the {@code
     * action} threw an exception an error message is logged and the {@code onError} {@link ActionInstance}s are
     * retrieved and executed. If the {@code action} terminated successfully the corresponding context variables are
     * set and the {@code onSuccess} {@link ActionInstance}s are retrieved and executed.
     *
     * @param action         the {@link JarvisAction} to execute
     * @param actionInstance the {@link ActionInstance} representing the signature of the {@link JarvisAction} to
     *                       execute
     * @param session        the {@link JarvisSession} used to define and access the context variables associated to the
     *                       provided {@code action}
     * @throws NullPointerException if the provided {@code action} or {@code session} is {@code null}
     */
    private void executeJarvisAction(JarvisAction action, ActionInstance actionInstance, JarvisSession session) {
        checkNotNull(action, "Cannot execute the provided %s %s", JarvisAction.class.getSimpleName(), action);
        checkNotNull(session, "Cannot execute the provided %s with the provided %s %s", JarvisAction.class
                .getSimpleName(), JarvisSession.class.getSimpleName(), session);
        JarvisActionResult result = action.call();
        if (result.isError()) {
            Log.error("An error occurred when executing the action {0}: {1} {2}", action.getClass().getSimpleName
                    (), result.getThrownException().getClass().getSimpleName(), result.getThrownException()
                    .getMessage());
            /*
             * Retrieve the ActionInstances to execute when the computed ActionInstance returns an error and execute
             * them.
             */
            for(ActionInstance onErrorActionInstance : actionInstance.getOnError()) {
                Log.info("Executing fallback action {0}", onErrorActionInstance.getAction().getName());
                JarvisAction onErrorJarvisAction = getJarvisActionFromActionInstance(onErrorActionInstance, session);
                executeJarvisAction(onErrorJarvisAction, onErrorActionInstance, session);
            }
        } else {
            if (nonNull(action.getReturnVariable())) {
                Log.info("Registering context variable {0} with value {1}", action.getReturnVariable(), result);
                session.getJarvisContext().setContextValue("variables", 1, action.getReturnVariable(),
                        result.getResult());
            }
        }
        Log.info("Action {0} executed in {1} ms", action.getClass().getSimpleName(), result.getExecutionTime());
    }

    /**
     * Constructs a {@link JarvisAction} instance corresponding to the provided {@code actionInstance}, initialized
     * in the provided {@code session}.
     * <p>
     * This method is used as a bridge between the {@link ActionInstance}s (from the orchestration model), and the
     * {@link JarvisAction}s (from the internal Jarvis execution engine).
     * @param actionInstance the {@link ActionInstance} to construct a {@link JarvisAction} from
     * @param session the {@link JarvisSession} used to define and access context variables
     * @return the constructed {@link JarvisAction}
     */
    private JarvisAction getJarvisActionFromActionInstance(ActionInstance actionInstance, JarvisSession session) {
        JarvisModule jarvisModule = this.getJarvisModuleRegistry().getJarvisModule((Module) actionInstance
                .getAction().eContainer());
        return jarvisModule.createJarvisAction(actionInstance, session);
    }

    /**
     * Retrieves the {@link ActionInstance}s associated to the provided {@code eventInstance}.
     * <p>
     * This class navigates the underlying {@link OrchestrationModel} and retrieves the {@link ActionInstance}s
     * associated to the provided {@code eventInstance}. These {@link ActionInstance}s are used by the core
     * component to create the concrete {@link JarvisAction} to execute.
     *
     * @param eventInstance the {@link EventInstance} to retrieve the {@link ActionInstance}s from
     * @return a {@link List} containing the instantiated {@link ActionInstance}s associated to the provided {@code
     * recognizedIntent}.
     * @see JarvisModule#createJarvisAction(ActionInstance, JarvisSession)
     */
    private List<ActionInstance> getActionsFromEvent(EventInstance eventInstance) {
        EventDefinition eventDefinition = eventInstance.getDefinition();
        for (OrchestrationLink link : orchestrationModel.getOrchestrationLinks()) {
            if (link.getEvent().getName().equals(eventDefinition.getName())) {
                return link.getActions();
            }
        }
        return Collections.emptyList();
    }

    /**
     * Shuts down the underlying {@link ExecutorService}.
     * <p>
     * Shutting down the {@link OrchestrationService} invalidates it and does not allow to process new
     * {@link JarvisAction}s.
     */
    public void shutdown() {
        this.executorService.shutdownNow();
    }

    /**
     * Returns whether the {@link OrchestrationService} is shutdown.
     *
     * @return whether the {@link OrchestrationService} is shutdown
     */
    public boolean isShutdown() {
        return this.executorService.isShutdown();
    }
}

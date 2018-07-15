package fr.zelus.jarvis.core;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.RecognizedIntent;
import fr.zelus.jarvis.io.InputProvider;
import fr.zelus.jarvis.module.Action;
import fr.zelus.jarvis.module.InputProviderDefinition;
import fr.zelus.jarvis.module.Parameter;
import fr.zelus.jarvis.orchestration.ActionInstance;
import fr.zelus.jarvis.orchestration.ParameterValue;
import fr.zelus.jarvis.orchestration.VariableAccess;
import fr.zelus.jarvis.util.Loader;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.StreamSupport;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * The concrete implementation of a {@link fr.zelus.jarvis.module.Module} definition.
 * <p>
 * A {@link JarvisModule} manages a set of {@link JarvisAction}s that represent the concrete actions that can
 * be executed by the module. This class provides primitives to enable/disable specific actions, and construct
 * {@link JarvisAction} instances from a given {@link RecognizedIntent}.
 * <p>
 * Note that enabling a {@link JarvisAction} will load the corresponding class, that must be stored in the
 * <i>action</i> package of the concrete {@link JarvisModule} implementation. For example, enabling the action
 * <i>MyAction</i> from the {@link JarvisModule} <i>myModulePackage.MyModule</i> will attempt to load the class
 * <i>myModulePackage.action.MyAction</i>.
 */
public abstract class JarvisModule {

    protected Configuration configuration;

    /**
     * The {@link Map} containing the {@link JarvisAction} associated to this module.
     * <p>
     * This {@link Map} is used as a cache to retrieve {@link JarvisAction} that have been previously loaded.
     *
     * @see #enableAction(Action)
     * @see #disableAction(Action)
     * @see #createJarvisAction(ActionInstance, RecognizedIntent, JarvisSession)
     */
    protected Map<String, Class<? extends JarvisAction>> actionMap;

    protected Map<String, Thread> inputProviderMap;


    /**
     * Constructs a new {@link JarvisModule} from the provided {@link Configuration}.
     * <p>
     * <b>Note</b>: this constructor will be called by jarvis internal engine when initializing the
     * {@link JarvisModule}s. Subclasses implementing this constructor typically need additional parameters to be
     * initialized, that can be provided in the {@code configuration}.
     *
     * @param configuration the {@link Configuration} used to initialize the {@link JarvisModule}
     * @see #JarvisModule()
     */
    public JarvisModule(Configuration configuration) {
        this.configuration = configuration;
        this.actionMap = new HashMap<>();
        this.inputProviderMap = new HashMap<>();
    }

    /**
     * Constructs a new {@link JarvisModule}.
     * <p>
     * <b>Note</b>: this constructor should be used by {@link JarvisModule}s that do not require additional
     * parameters to be initialized. In that case see {@link #JarvisModule(Configuration)}.
     *
     * @see #JarvisModule(Configuration)
     */
    public JarvisModule() {
        this(new BaseConfiguration());
    }

    /**
     * Returns the name of the module.
     * <p>
     * This method returns the value of {@link Class#getSimpleName()}, and can not be overridden by concrete
     * subclasses. {@link JarvisModule}'s names are part of jarvis' naming convention, and are used to dynamically
     * load modules and actions.
     *
     * @return the name of the module.
     */
    public final String getName() {
        return this.getClass().getSimpleName();
    }

    public final void startInputProvider(InputProviderDefinition inputProviderDefinition, JarvisCore jarvisCore) {
        Log.info("Starting {0}", inputProviderDefinition.getName());
        String inputProviderQualifiedName = this.getClass().getPackage().getName() + ".io." + inputProviderDefinition
                .getName();
        Class<? extends InputProvider> inputProviderClass = Loader.loadClass(inputProviderQualifiedName,
                InputProvider.class);
        InputProvider inputProvider;
        try {
            inputProvider = Loader.construct(inputProviderClass, Arrays.asList
                    (JarvisCore.class, Configuration.class), Arrays
                    .asList(jarvisCore, configuration));
        } catch (NoSuchMethodException e) {
            Log.warn("Cannot find the method {0}({1},{2}), trying to initialize the InputProvider using its " +
                            "{0}({1}) constructor", inputProviderClass.getSimpleName(), JarvisCore.class
                            .getSimpleName(),
                    Configuration.class.getSimpleName());
            try {
                inputProvider = Loader.construct(inputProviderClass, JarvisCore.class, jarvisCore);
            } catch (NoSuchMethodException e1) {
                String errorMessage = MessageFormat.format("Cannot initialize {0}, the constructor {0}({1}) does " +
                        "not exist", inputProviderClass.getSimpleName(), JarvisCore.class.getSimpleName());
                Log.error(errorMessage);
                throw new JarvisException(errorMessage, e1);
            }
        }
        Log.info("Starting InputProvider {0}", inputProviderClass.getSimpleName());
        Thread inputProviderThread = new Thread(inputProvider);
        inputProviderMap.put(inputProviderDefinition.getName(), inputProviderThread);
        inputProviderThread.start();
    }

    /**
     * Retrieves and loads the {@link JarvisAction} defined by the provided {@link Action}.
     * <p>
     * This method loads the corresponding {@link JarvisAction} based on jarvis' naming convention. The
     * {@link JarvisAction} must be located under the {@code action} sub-package of the {@link JarvisModule}
     * concrete subclass package.
     *
     * @param action the {@link Action} definition representing the {@link JarvisAction} to enable
     * @see Loader#loadClass(String, Class)
     */
    public final void enableAction(Action action) {
        String actionQualifiedName = this.getClass().getPackage().getName() + ".action." + action.getName();
        Class<? extends JarvisAction> jarvisAction = Loader.loadClass(actionQualifiedName, JarvisAction.class);
        actionMap.put(action.getName(), jarvisAction);
    }

    /**
     * Disables the {@link JarvisAction} defined by the provided {@link Action}.
     *
     * @param action the {@link Action} definition representing the {@link JarvisAction} to disable
     */
    public final void disableAction(Action action) {
        actionMap.remove(action.getName());
    }

    /**
     * Disables all the {@link JarvisAction}s of the {@link JarvisModule}.
     */
    public final void disableAllActions() {
        actionMap.clear();
    }

    public final Class<? extends JarvisAction> getAction(String actionName) {
        return actionMap.get(actionName);
    }

    /**
     * Returns all the {@link JarvisAction} {@link Class}es associated to this {@link JarvisModule}.
     * <p>
     * This method returns the {@link Class}es describing the {@link JarvisAction}s associated to this module. To
     * construct a new {@link JarvisAction} from a {@link RecognizedIntent} see
     * {@link #createJarvisAction(ActionInstance, RecognizedIntent, JarvisSession)} .
     *
     * @return all the {@link JarvisAction} {@link Class}es associated to this {@link JarvisModule}
     * @see #createJarvisAction(ActionInstance, RecognizedIntent, JarvisSession)
     */
    public final Collection<Class<? extends JarvisAction>> getActions() {
        return actionMap.values();
    }

    /**
     * Creates a new {@link JarvisAction} instance from the provided {@link RecognizedIntent}.
     * <p>
     * This methods attempts to construct a {@link JarvisAction} defined by the provided {@code actionInstance} by
     * matching the {@code intent} variables to the {@link Action}'s parameters, and reusing the provided
     * {@link ActionInstance#getValues()}.
     *
     * @param actionInstance the {@link ActionInstance} representing the {@link JarvisAction} to create
     * @param intent         the {@link RecognizedIntent} containing the extracted variables
     * @param session        the {@link JarvisSession} associated to the action
     * @return a new {@link JarvisAction} instance from the provided {@link RecognizedIntent}
     * @throws JarvisException if the provided {@link Action} does not match any {@link JarvisAction}, or if the
     *                         provided {@link RecognizedIntent} does not define all the parameters required by the
     *                         action's constructor
     * @see #getParameterValues(ActionInstance, JarvisContext)
     */
    public JarvisAction createJarvisAction(ActionInstance actionInstance, RecognizedIntent intent, JarvisSession
            session) {
        checkNotNull(actionInstance, "Cannot construct a JarvisAction from a null ActionInstance");
        Action action = actionInstance.getAction();
        checkNotNull(intent, "Cannot construct a %s action from a null RecognizedIntent", action.getName());
        Class<? extends JarvisAction> jarvisActionClass = actionMap.get(action.getName());
        if (isNull(jarvisActionClass)) {
            throw new JarvisException(MessageFormat.format("Cannot create the JarvisAction {0}, the action is not " +
                    "loaded in the module", action.getName()));
        }
        Object[] parameterValues = getParameterValues(actionInstance, session.getJarvisContext());
        /*
         * Append the mandatory parameters to the parameter values.
         */
        Object[] fullParameters = new Object[parameterValues.length + 2];
        fullParameters[0] = this;
        fullParameters[1] = session;
        JarvisAction jarvisAction;
        if (parameterValues.length > 0) {
            System.arraycopy(parameterValues, 0, fullParameters, 2, parameterValues.length);
        }
        try {
            /**
             * The types of the parameters are not known, use {@link Loader#construct(Class, Object[])} to try to
             * find a constructor that accepts them.
             */
            jarvisAction = Loader.construct(jarvisActionClass, fullParameters);
        } catch (NoSuchMethodException e) {
            throw new JarvisException(e);
        }
        if (nonNull(actionInstance.getReturnVariable())) {
            jarvisAction.setReturnVariable(actionInstance.getReturnVariable().getReferredVariable().getName());
        }
        return jarvisAction;
    }

    /**
     * Retrieves the {@code actionInstance}'s parameter values from the provided {@code context}.
     * <p>
     * This method iterates through the {@link ActionInstance}'s {@link ParameterValue}s and matches them
     * against the describing {@link Action}'s {@link Parameter}s. The concrete value associated to the
     * {@link ActionInstance}'s {@link ParameterValue}s are retrieved from the provided {@code context}.
     * <p>
     * The retrieved values are used by the {@link JarvisModule} to instantiate concrete {@link JarvisAction}s (see
     * {@link #createJarvisAction(ActionInstance, RecognizedIntent, JarvisSession)}).
     *
     * @param actionInstance the {@link ActionInstance} to match the parameters from
     * @return an array containing the concrete {@link ActionInstance}'s parameters
     * @throws JarvisException if one of the concrete value is not stored in the provided {@code context}, or if the
     *                         {@link ActionInstance}'s {@link ParameterValue}s do not match the describing
     *                         {@link Action}'s {@link Parameter}s.
     * @see #createJarvisAction(ActionInstance, RecognizedIntent, JarvisSession)
     */
    private Object[] getParameterValues(ActionInstance actionInstance, JarvisContext context) {
        Action action = actionInstance.getAction();
        List<Parameter> actionParameters = action.getParameters();
        List<ParameterValue> actionInstanceParameterValues = actionInstance.getValues();
        if ((actionParameters.size() == actionInstanceParameterValues.size())) {
            /*
             * Here some additional checks are needed (parameter types and order).
             * See https://github.com/gdaniel/jarvis/issues/4.
             */
            Object[] actionInstanceParameterValuesArray = StreamSupport.stream(actionInstanceParameterValues
                    .spliterator(), false).map(param -> {
                if (param instanceof VariableAccess) {
                    String variableName = ((VariableAccess) param).getReferredVariable().getName();
                    Future<Object> value = (Future<Object>) context.getContextValue("variables", variableName);
                    try {
                        return value.get().toString();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new JarvisException(e);
                    }
                } else {
                    return context.fillContextValues(param.getValue());
                }
            }).toArray();
            return actionInstanceParameterValuesArray;
        }
        String errorMessage = MessageFormat.format("The action does not define the good amount of parameters: " +
                "expected {0}, found {1}", actionParameters.size(), actionInstanceParameterValues.size());
        Log.error(errorMessage);
        throw new JarvisException(errorMessage);
    }
}

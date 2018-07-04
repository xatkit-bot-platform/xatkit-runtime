package fr.zelus.jarvis.core;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.intent.RecognizedIntent;
import fr.zelus.jarvis.module.Action;
import fr.zelus.jarvis.module.Parameter;
import fr.zelus.jarvis.orchestration.ActionInstance;
import fr.zelus.jarvis.orchestration.ParameterValue;
import fr.zelus.jarvis.orchestration.VariableAccess;
import org.apache.commons.configuration2.Configuration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.StreamSupport;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static fr.zelus.jarvis.utils.LogUtils.prettyPrint;
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

    /**
     * Tha {@link Map} containing the {@link JarvisAction} associated to this module.
     * <p>
     * This {@link Map} is used as a cache to retrieve {@link JarvisAction} that have been previously loaded.
     *
     * @see #enableAction(Action)
     * @see #disableAction(Action)
     * @see #createJarvisAction(ActionInstance, RecognizedIntent, JarvisContext)
     */
    protected Map<String, Class<? extends JarvisAction>> actionMap;


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
        /*
         * Do nothing with the configuration, it can be used by subclasses that require additional initialization
         * information.
         */
        this();
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
        this.actionMap = new HashMap<>();
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

    /**
     * Retrieves and loads the {@link JarvisAction} defined by the provided {@link Action}.
     * <p>
     * This method loads the corresponding {@link JarvisAction} based on jarvis' naming convention. The
     * {@link JarvisAction} must be located under the {@code action} sub-package of the {@link JarvisModule}
     * concrete subclass package (see {@link #loadJarvisActionClass(Action)}).
     *
     * @param action the {@link Action} definition representing the {@link JarvisAction} to enable
     * @see #loadJarvisActionClass(Action)
     */
    public final void enableAction(Action action) {
        Class<JarvisAction> jarvisAction = this.loadJarvisActionClass(action);
        actionMap.put(jarvisAction.getSimpleName(), jarvisAction);
    }

    /**
     * Disables the {@link JarvisAction} defined by the provided {@link Action}.
     *
     * @param action the {@link Action} definition representing the {@link JarvisAction} to disable
     */
    public final void disableAction(Action action) {
        actionMap.remove(this.loadJarvisActionClass(action).getSimpleName());
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
     * {@link #createJarvisAction(ActionInstance, RecognizedIntent, JarvisContext)} .
     *
     * @return all the {@link JarvisAction} {@link Class}es associated to this {@link JarvisModule}
     * @see #createJarvisAction(ActionInstance, RecognizedIntent, JarvisContext)
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
     * @param context        the {@link JarvisContext} associated to the action
     * @return a new {@link JarvisAction} instance from the provided {@link RecognizedIntent}
     * @throws JarvisException if the provided {@link Action} does not match any {@link JarvisAction}, or if the
     *                         provided {@link RecognizedIntent} does not define all the parameters required by the
     *                         action's constructor
     * @see #getParameterValues(ActionInstance, RecognizedIntent, JarvisContext)
     */
    public JarvisAction createJarvisAction(ActionInstance actionInstance, RecognizedIntent intent, JarvisContext
            context) {
        checkNotNull(actionInstance, "Cannot construct a JarvisAction from a null ActionInstance");
        Action action = actionInstance.getAction();
        checkNotNull(intent, "Cannot construct a %s action from a null RecognizedIntent", action.getName());
        Class<? extends JarvisAction> jarvisActionClass = actionMap.get(action.getName());
        if (isNull(jarvisActionClass)) {
            throw new JarvisException(MessageFormat.format("Cannot create the JarvisAction {0}, the action is not " +
                    "loaded in the module", action.getName()));
        }
        Object[] parameterValues = getParameterValues(actionInstance, intent, context);
        Constructor<?>[] constructorList = jarvisActionClass.getConstructors();
        JarvisAction jarvisAction;
        for (int i = 0; i < constructorList.length; i++) {
            Constructor<?> constructor = constructorList[i];
            /*
             * We use constructor.getParameterCount() -2 because the two first parameters of JarvisAction constructors
             * must be their containing JarvisModule and the associated JarvisContext.
             */
            if (constructor.getParameterCount() - 2 == parameterValues.length) {
                /*
                 * The following code assumes that all the Action parameters are instances of String, this should be
                 * fixed by supporting the types returned by the DialogFlow API.
                 */
                try {
                    if (constructor.getParameterCount() > 0) {
                        /*
                         * Construct the full parameter array, that contains this as its first element, followed by
                         * the parameterValues.
                         */
                        Object[] fullParameters = new Object[parameterValues.length + 2];
                        fullParameters[0] = this;
                        fullParameters[1] = context;
                        System.arraycopy(parameterValues, 0, fullParameters, 2, parameterValues.length);
                        Log.info("Constructing {0} with the parameters ({1})", jarvisActionClass.getSimpleName(),
                                prettyPrint(parameterValues));
                        jarvisAction = (JarvisAction) constructor.newInstance(fullParameters);
                    } else {
                        Log.info("Constructing {0}({1}, {2})", jarvisActionClass.getSimpleName(), this.getClass()
                                .getSimpleName(), context);
                        jarvisAction = (JarvisAction) constructor.newInstance(this, context);
                    }
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    String errorMessage = MessageFormat.format("Cannot construct the JarvisAction {0}",
                            jarvisActionClass.getSimpleName());
                    Log.error(errorMessage);
                    throw new JarvisException(errorMessage, e);
                }
                /*
                 * The ActionInstance defines a return variable, we record it in the JarvisAction in order to store
                 * it in the global context.
                 */
                if(nonNull(actionInstance.getReturnVariable())) {
                    jarvisAction.setReturnVariable(actionInstance.getReturnVariable().getReferredVariable().getName());
                }
                return jarvisAction;
            }
        }
        String errorMessage = MessageFormat.format("Cannot find a {0} constructor matching the provided parameters " +
                "({1})", action.getName(), prettyPrint(parameterValues));
        Log.error(errorMessage);
        throw new JarvisException(errorMessage);
    }

    /**
     * Match the {@link RecognizedIntent}'s variable to the provided {@link Action}'s parameters.
     * <p>
     * This method checks that the provided {@code intent} contains all the variables that are required by the
     * {@link Action} definition that are not already defined in the {@link ActionInstance#getValues()} list, and
     * returns them.
     *
     * @param actionInstance the {@link Action} definition to match the parameters from
     * @param intent         the {@link RecognizedIntent} to match the variables from
     * @return an array containing the {@link Action}'s parameters
     * @throws JarvisException if the provided {@link RecognizedIntent} does not define all the parameters required
     *                         by the action's constructor
     * @see #createJarvisAction(ActionInstance, RecognizedIntent, JarvisContext)
     */
    private Object[] getParameterValues(ActionInstance actionInstance, RecognizedIntent intent, JarvisContext context) {
        Action action = actionInstance.getAction();
        List<Parameter> actionParameters = action.getParameters();
        List<ParameterValue> actionInstanceParameterValues = actionInstance.getValues();
        if ((actionParameters.size() == actionInstanceParameterValues.size())) {
            /*
             * Here some additional checks are needed (parameter types and order).
             * See https://github.com/gdaniel/jarvis/issues/4.
             */
            int parameterLength = actionInstanceParameterValues.size();
            Object[] actionInstanceParameterValuesArray = StreamSupport.stream(actionInstanceParameterValues
                    .spliterator(), false).map(param -> {
                        if(param instanceof VariableAccess) {
                            String variableName = ((VariableAccess)param).getReferredVariable().getName();
                            Future<Object> value = (Future<Object>)context.getContextValue("variables", variableName);
                            try {
                                return value.get().toString();
                            } catch(InterruptedException | ExecutionException e) {
                                throw new JarvisException(e);
                            }
                        } else {
                            return param.getValue();
                        }
                    }).toArray();
            return actionInstanceParameterValuesArray;
        }
        String errorMessage = MessageFormat.format("The action does not define the good amount of parameters: " +
                "expected {0}, found {1}", actionParameters.size(), actionInstanceParameterValues.size());
        Log.error(errorMessage);
        throw new JarvisException(errorMessage);
    }

    /**
     * Loads the {@link JarvisAction} defined by the provided {@code action}.
     * <p>
     * This method loads the corresponding {@link JarvisAction} based on jarvis' naming convention. The
     * {@link JarvisAction} must be located under the {@code action} sub-package of the {@link JarvisModule}
     * concrete subclass package.
     *
     * @param action the {@link Action} definition representing the {@link JarvisAction} to load
     * @return the {@link Class} representing the loaded {@link JarvisAction}
     * @throws JarvisException if the {@link JarvisAction} can not be loaded
     */
    private Class<JarvisAction> loadJarvisActionClass(Action action) {
        /*
         * Ensures the Action is in the same package, under the Action/ subpackage
         */
        String actionQualifiedName = this.getClass().getPackage().getName() + ".action." + action.getName();
        try {
            return (Class<JarvisAction>) Class.forName(actionQualifiedName);
        } catch (ClassNotFoundException e) {
            String errorMessage = MessageFormat.format("Cannot load the Action {0} with the qualified name {1}",
                    action.getName(), actionQualifiedName);
            Log.error(errorMessage);
            throw new JarvisException(errorMessage, e);
        }
    }
}

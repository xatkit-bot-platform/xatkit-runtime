package fr.zelus.jarvis.core;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.intent.RecognizedIntent;
import fr.zelus.jarvis.module.Action;
import fr.zelus.jarvis.module.Parameter;
import fr.zelus.jarvis.module.PresetParameter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Objects.isNull;

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
     * @see #createJarvisAction(Action, RecognizedIntent)
     */
    protected Map<String, Class<JarvisAction>> actionMap;

    /**
     * Constructs a new {@link JarvisModule} and initializes its {@link #actionMap}.
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

    /**
     * Returns all the {@link JarvisAction} {@link Class}es associated to this {@link JarvisModule}.
     * <p>
     * This method returns the {@link Class}es describing the {@link JarvisAction}s associated to this module. To
     * construct a new {@link JarvisAction} from a {@link RecognizedIntent} see
     * {@link #createJarvisAction(Action, RecognizedIntent)}.
     *
     * @return all the {@link JarvisAction} {@link Class}es associated to this {@link JarvisModule}
     * @see #createJarvisAction(Action, RecognizedIntent)
     */
    public final Collection<Class<JarvisAction>> getActions() {
        return actionMap.values();
    }

    /**
     * Creates a new {@link JarvisAction} instance from the provided {@link RecognizedIntent}.
     * <p>
     * This methods attempts to construct a {@link JarvisAction} defined by the provided {@code action} by
     * matching the {@code intent} variables to the {@link Action}'s parameters.
     *
     * @param action the {@link Action} definition representing the {@link JarvisAction} to create
     * @param intent the {@link RecognizedIntent} containing the extracted variables
     * @return a new {@link JarvisAction} instance from the provided {@link RecognizedIntent}
     * @throws JarvisException if the provided {@link Action} does not match any {@link JarvisAction}, or if the
     *                         provided {@link RecognizedIntent} does not define all the parameters required by the
     *                         action's constructor
     * @see #getParameterValues(Action, RecognizedIntent)
     */
    public JarvisAction createJarvisAction(Action action, RecognizedIntent intent) {
        Class<JarvisAction> jarvisActionClass = actionMap.get(action.getName());
        if (isNull(jarvisActionClass)) {
            throw new JarvisException("Cannot create the JarvisAction {0}, the action is not loaded in the module");
        }
        Object[] parameterValues = getParameterValues(action, intent);
        Constructor<?>[] constructorList = jarvisActionClass.getConstructors();
        for (int i = 0; i < constructorList.length; i++) {
            Constructor<?> constructor = constructorList[i];
            if (constructor.getParameterCount() == parameterValues.length) {
                /*
                 * The following code assumes that all the Action parameters are instances of String, this should be
                 * fixed by supporting the types returned by the DialogFlow API.
                 */
                try {
                    if (constructor.getParameterCount() > 0) {
                        return (JarvisAction) constructor.newInstance(parameterValues);
                    } else {
                        return (JarvisAction) constructor.newInstance();
                    }
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    String errorMessage = MessageFormat.format("Cannot construct the JarvisAction {0}",
                            jarvisActionClass.getSimpleName());
                    Log.error(errorMessage);
                    throw new JarvisException(errorMessage, e);
                }
            }
        }
        String errorMessage = MessageFormat.format("Cannot find a {0} constructor matching the provided parameters " +
                "{1}", action.getName(), parameterValues);
        Log.error(errorMessage);
        throw new JarvisException(errorMessage);
    }

    /**
     * Match the {@link RecognizedIntent}'s variable to the provided {@link Action}'s parameters.
     * <p>
     * This method checks that the provided {@code intent} contains all the variables that are required by the
     * {@link Action} definition, and returns them.
     *
     * @param action the {@link Action} definition to match the parameters from
     * @param intent the {@link RecognizedIntent} to match the variables from
     * @return an array containing the {@link Action}'s parameters
     * @throws JarvisException if the provided {@link RecognizedIntent} does not define all the parameters required
     *                         by the action's constructor
     * @see #createJarvisAction(Action, RecognizedIntent)
     */
    private Object[] getParameterValues(Action action, RecognizedIntent intent) {
        List<Parameter> actionParameters = StreamSupport.stream(action.getParameters().spliterator(), false).filter
                (param -> !(param instanceof PresetParameter)).collect(Collectors.toList());
        List<String> outContextValues = intent.getOutContextValues();
        if (actionParameters.size() == outContextValues.size()) {
            /*
             * Here some additional checks are needed (parameter types and order).
             * See https://github.com/gdaniel/jarvis/issues/4.
             */
            return outContextValues.toArray();
        }
        /*
         * It should be possible to return an array if the provided intent contains more context values than the
         * Action signature.
         * See https://github.com/gdaniel/jarvis/issues/5.
         */
        String errorMessage = MessageFormat.format("The intent does not define the good amount of context values: " +
                "expected {0}, found {1}", actionParameters.size(), outContextValues.size());
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

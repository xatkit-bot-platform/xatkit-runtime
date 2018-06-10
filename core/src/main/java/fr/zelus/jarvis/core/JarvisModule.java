package fr.zelus.jarvis.core;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.intent.RecognizedIntent;
import fr.zelus.jarvis.module.Action;
import fr.zelus.jarvis.module.Parameter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    protected Map<String, Class<JarvisAction>> actionMap;

    public JarvisModule() {
        this.actionMap = new HashMap<>();
    }

    public final String getName() {
        return this.getClass().getSimpleName();
    }

    public final void enableAction(Action action) {
        Class<JarvisAction> jarvisAction = this.loadJarvisActionClass(action);
        actionMap.put(jarvisAction.getSimpleName(), jarvisAction);
    }

    public final void disableAction(Action action) {
        actionMap.remove(this.loadJarvisActionClass(action).getSimpleName());
    }

    public final void disableAllActions() {
        actionMap.clear();
    }

    public final Collection<Class<JarvisAction>> getActions() {
        return actionMap.values();
    }

    public JarvisAction createJarvisAction(Action action, RecognizedIntent intent) {
        Class<JarvisAction> jarvisActionClass = actionMap.get(action.getName());
        if(isNull(jarvisActionClass)) {
            throw new JarvisException("Cannot create the JarvisAction {0}, the action is not loaded in the module");
        }
        Object[] parameterValues = getParameterValues(action, intent);
        Constructor<?>[] constructorList = jarvisActionClass.getConstructors();
        for(int i = 0; i < constructorList.length; i++) {
            Constructor<?> constructor = constructorList[i];
            if(constructor.getParameterCount() == parameterValues.length) {
                /*
                 * The following code assumes that all the Action parameters are instances of String, this should be
                 * fixed by supporting the types returned by the DialogFlow API.
                 */
                try {
                    if(constructor.getParameterCount() > 0) {
                        return (JarvisAction) constructor.newInstance(parameterValues);
                    } else {
                        return (JarvisAction) constructor.newInstance();
                    }
                } catch(InstantiationException | IllegalAccessException | InvocationTargetException e) {
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

    private Object[] getParameterValues(Action action, RecognizedIntent intent) {
        List<Parameter> actionParameters = action.getParameters();
        List<String> outContextValues = intent.getOutContextValues();
        if(actionParameters.size() == outContextValues.size()) {
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

    private Class<JarvisAction> loadJarvisActionClass(Action action) {
        /*
         * Ensures the Action is in the same package, under the Action/ subpackage
         */
        String actionQualifiedName = this.getClass().getPackage().getName() + ".action." + action.getName();
        try {
            return (Class<JarvisAction>) Class.forName(actionQualifiedName);
        } catch(ClassNotFoundException e) {
            String errorMessage = MessageFormat.format("Cannot load the Action {0} with the qualified name {1}",
                    action.getName(), actionQualifiedName);
            Log.error(errorMessage);
            throw new JarvisException(errorMessage, e);
        }
    }

}

package fr.zelus.jarvis.plugins.generic_chat.module;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.core.JarvisModule;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.EventInstance;
import fr.zelus.jarvis.orchestration.*;
import fr.zelus.jarvis.platform.Action;
import fr.zelus.jarvis.plugins.generic_chat.module.action.GenericChatAction;
import fr.zelus.jarvis.util.Loader;
import org.apache.commons.configuration2.Configuration;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;

/**
 * A generic {@link JarvisModule} class that wraps concrete chatting sub-modules.
 * <p>
 * This class is used to define generic orchestration models that can be bound to specific chatting platforms through
 * the provided {@link Configuration}. {@link ActionInstance}s created by this module wraps concrete
 * {@link ActionInstance}s defined in the concrete sub-modules.
 * <p>
 * This module provides the following generic {@link JarvisAction}s:
 * <ul>
 * <li>PostMessage: post a message to the channels specified in the provided {@link Configuration}</li>
 * </ul>
 * <b>Note:</b> this module only provides a concrete implementation of {@link GenericChatAction}, that is used to
 * represent all the {@link JarvisAction}s specified in the <i>GenericChatModule</i>. {@link GenericChatAction}
 * construction is handled by the {@link #createJarvisAction(ActionInstance, JarvisSession)} method that overrides
 * the default behavior of {@link JarvisModule}.
 *
 * @see GenericChatAction
 */
public class GenericChatModule extends JarvisModule {

    /**
     * The {@link Configuration} key to store the concrete chat sub-module to use.
     *
     * @see #loadSubModule(String)
     */
    public static final String CONCRETE_CHAT_MODULES_KEY = "jarvis.concrete.chat.modules";

    /**
     * The {@link Configuration} key prefix used to bound specific values to generic action parameters.
     * <p>
     * As an example, this prefix is used to specify, for each sub-module, the platform specific channel associated
     * to the generic <i>channel</i> parameter.
     */
    public static final String CONCRETE_CHAT_PROPERTY_PREFIX_KEY = "jarvis.concrete.chat";

    /**
     * The {@link JarvisModule} used to execute the concrete chatting {@link JarvisAction}s.
     *
     * @see #loadSubModule(String)
     */
    private List<JarvisModule> subModules;

    /**
     * Constructs a new {@link GenericChatModule} from the provided {@code jarvisCore} and {@link Configuration}.
     * <p>
     * This constructor initializes the underlying sub-module list with the concrete {@link JarvisModule} paths
     * retrieved from the {@link #CONCRETE_CHAT_MODULES_KEY} {@link Configuration} property. Note that the property
     * can contain a single path or a list.
     * <p>
     * <b>Note:</b> this constructor will throw a {@link JarvisException} if at least one of the concrete sub-modules cannot be
     * built. The provided {@link Configuration} should contain all the sub-module specific properties (e.g. access
     * tokens).
     *
     * @param jarvisCore    the {@link JarvisCore} instance associated to this module
     * @param configuration the {@link Configuration} used to initialize the underlying sub-module list
     * @throws NullPointerException     if the provided {@code jarvisCore} or {@link Configuration} is {@code null}
     * @throws IllegalArgumentException if the provided {@link Configuration} does not contain a
     *                                  {@link #CONCRETE_CHAT_MODULES_KEY} property.
     * @see #CONCRETE_CHAT_MODULES_KEY
     * @see #loadSubModule(String)
     */
    public GenericChatModule(JarvisCore jarvisCore, Configuration configuration) {
        super(jarvisCore, configuration);
        checkArgument(configuration.containsKey(CONCRETE_CHAT_MODULES_KEY), "Cannot construct a %s: the provided %s " +
                "does not contain the %s required property", this.getClass().getSimpleName(), Configuration.class
                .getSimpleName(), CONCRETE_CHAT_MODULES_KEY);
        this.subModules = new ArrayList<>();
        /*
         * We don't need to check if the property exists, it is already done by the checkArgument method.
         */
        Object concreteChatModulesProperty = configuration.getProperty(CONCRETE_CHAT_MODULES_KEY);
        if (concreteChatModulesProperty instanceof List) {
            /*
             * The configuration defines multiple sub-modules
             */
            for (Object o : (List) concreteChatModulesProperty) {
                if (o instanceof String) {
                    subModules.add(loadSubModule((String) o));
                } else {
                    throw new JarvisException(MessageFormat.format("Cannot load the sub-module from the provided " +
                                    "property %s, expected a %s value, found %s", o.toString(), String.class
                                    .getSimpleName(),
                            o.getClass().getSimpleName()));
                }
            }
        } else if (concreteChatModulesProperty instanceof String) {
            /*
             * The configuration defines a single sub-module stored in a String
             */
            subModules.add(loadSubModule((String) concreteChatModulesProperty));
        } else {
            /*
             * The configuration property type is neither a String or a List.
             */
            throw new JarvisException(MessageFormat.format("Cannot load the sub-module from the provided property" +
                    " %s, expected a %s value, found %s", concreteChatModulesProperty.toString(), String.class
                    .getSimpleName(), concreteChatModulesProperty.getClass().getSimpleName()));
        }
    }

    /**
     * Loads and construct the {@link JarvisModule} at the given {@code modulePath}.
     * <p>
     * <b>Note:</b> this method does not check if another instance of the {@link JarvisModule} has been loaded by the
     * {@link JarvisCore} component. This should not create consistency issues because the sub-modules are not
     * interacting with {@link JarvisModule}s loaded by the {@link JarvisCore}.
     *
     * @param modulePath the path of the {@link JarvisModule} to load
     * @return the constructed {@link JarvisModule}
     * @throws JarvisException if an error occurred when loading or constructing the {@link JarvisModule}
     * @see Loader#constructJarvisModule(Class, JarvisCore, Configuration)
     */
    private JarvisModule loadSubModule(String modulePath) {
        Class<? extends JarvisModule> jarvisModuleClass = Loader.loadClass(modulePath, JarvisModule.class);
        return Loader.constructJarvisModule(jarvisModuleClass, this.jarvisCore, this.configuration);
    }

    /**
     * Returns the {@link List} of sub-modules managed by the {@link GenericChatModule}.
     *
     * @return the {@link List} of sub-modules managed by the {@link GenericChatModule}
     */
    public List<JarvisModule> getSubModules() {
        return subModules;
    }

    /**
     * Retrieves an loads the {@link JarvisAction} defined by the provided {@link Action} in all the sub-modules.
     *
     * @param action the {@link Action} definition representing the {@link JarvisAction} to enable in all the
     *               sub-modules
     */
    @Override
    public void enableAction(Action action) {
        for (JarvisModule subModule : subModules) {
            /*
             * This works because the actions have the same name
             */
            subModule.enableAction(action);
        }
    }

    /**
     * Disables the {@link JarvisAction} defined by the provided {@link Action} in all the sub-modules.
     *
     * @param action the {@link Action} definition representing the {@link JarvisAction} to disable in all the
     *               sub-modules
     */
    @Override
    public void disableAction(Action action) {
        for (JarvisModule subModule : subModules) {
            /*
             * This works because the actions have the same name
             */
            subModule.disableAction(action);
        }
    }

    /**
     * Creates a {@link GenericChatAction} wrapping the concrete sub-module {@link JarvisAction}s corresponding to
     * the provided {@link ActionInstance}.
     * <p>
     * This method iterates the sub-module list and calls their
     * {@link JarvisModule#createJarvisAction(ActionInstance, JarvisSession)} method with the provided {@code
     * actionInstance} and {@code session}. The returned {@link JarvisAction}s are then wrapped into a
     * {@link GenericChatAction} that takes care of executing them through the regular
     * {@link fr.zelus.jarvis.core.OrchestrationService#handleEventInstance(EventInstance, JarvisSession)} process.
     *
     * @param actionInstance the {@link ActionInstance} representing the {@link JarvisAction} to create
     * @param session        the {@link JarvisSession} associated to the action
     * @return the {@link GenericChatAction} wrapping the concrete sub-module {@link JarvisAction}s
     * @throws NullPointerException if the provided {@code actionInstance} or {@code session} is {@code null}
     * @throws JarvisException      if the provided {@link ActionInstance} does not match any {@link JarvisAction} for a
     *                              sub-module, or if an error occurred when building the {@link JarvisAction}
     * @see GenericChatAction
     */
    @Override
    public JarvisAction createJarvisAction(ActionInstance actionInstance, JarvisSession session) {
        /*
         * Do not check preconditions here, it is already done in sub-modules.
         */
        List<JarvisAction> concreteActions = new ArrayList<>();
        for (JarvisModule subModule : subModules) {
            concreteActions.add(subModule.createJarvisAction(reifyActionInstance(actionInstance, subModule), session));
        }
        return new GenericChatAction(this, session, concreteActions);
    }

    /**
     * Reifies the provided {@code genericActionInstance} into its {@code concreteModule} implementation.
     * <p>
     * This method performs a deep copy of the provided {@code genericActionInstance}, and searches, for each of its
     * parameter, if a redefined value has been provided in the {@link Configuration} for the specified {@code
     * concreteModule}. If a parameter is not redefined in the {@link Configuration} the generic value is used by the
     * {@code concreteModule} implementation.
     * <p>
     * Module-specific parameters can be provided in the {@link Configuration} following this pattern: {@code
     * {@link #CONCRETE_CHAT_PROPERTY_PREFIX_KEY}.parameterKey.concrete_module_name}. Note that the
     * concrete_module_name must be lowercase.
     *
     * @param genericActionInstance the {@link ActionInstance} to reify into its {@code concreteModule} implementation
     * @param concreteModule        the {@link JarvisModule} containing the target-specific implementation of the {@code
     *                              genericActionInstance}
     * @return the reified {@link ActionInstance}
     * @see #deepCopy(Expression)
     */
    private ActionInstance reifyActionInstance(ActionInstance genericActionInstance, JarvisModule concreteModule) {
        ActionInstance concreteActionInstance = OrchestrationFactory.eINSTANCE.createActionInstance();
        concreteActionInstance.setAction(genericActionInstance.getAction());
        concreteActionInstance.setReturnVariable(genericActionInstance.getReturnVariable());
        for (ParameterValue genericPValue : genericActionInstance.getValues()) {
            ParameterValue concretePValue = OrchestrationFactory.eINSTANCE.createParameterValue();
            concretePValue.setParameter(genericPValue.getParameter());
            concreteActionInstance.getValues().add(concretePValue);
            String parameterPropertyKey = CONCRETE_CHAT_PROPERTY_PREFIX_KEY + "." + genericPValue.getParameter()
                    .getKey().toLowerCase() + "." + concreteModule.getName().toLowerCase();
            Log.info("Checking property {0}", parameterPropertyKey);
            if (configuration.containsKey(parameterPropertyKey)) {
                /*
                 * The configuration provides a module-specific value of the parameter key, using it in the created
                 * ActionInstance.
                 */
                String concreteValue = configuration.getString(parameterPropertyKey);
                StringLiteral stringLiteral = OrchestrationFactory.eINSTANCE.createStringLiteral();
                stringLiteral.setValue(concreteValue);
                concretePValue.setExpression(stringLiteral);
            } else {
                /*
                 * The configuration does not provide a module-specific value of the parameter key, copying the
                 * generic expression.
                 */
                Expression copiedExpression = deepCopy(genericPValue.getExpression());
                concretePValue.setExpression(copiedExpression);
            }
        }
        return concreteActionInstance;
    }

    /**
     * Performs a deep copy of the provided {@code from} {@link Expression}.
     * <p>
     * This method ensures that all the {@link Expression}'s containment tree is duplicated with new objects, and
     * that the initial {@link Expression} has not been changed, allowing to reuse it to build alternative
     * {@link ActionInstance}s.
     *
     * @param from the {@link Expression} to copy
     * @return a new {@link Expression} copied from the provided one
     * @throws JarvisException if the provided {@link Expression} type is not supported
     */
    private Expression deepCopy(Expression from) {
        OrchestrationFactory f = OrchestrationFactory.eINSTANCE;
        if (from instanceof StringLiteral) {
            StringLiteral stringLiteral = f.createStringLiteral();
            stringLiteral.setValue(((StringLiteral) from).getValue());
            return stringLiteral;
        }
        if (from instanceof VariableAccess) {
            VariableAccess variableAccess = f.createVariableAccess();
            variableAccess.setReferredVariable(((VariableAccess) from).getReferredVariable());
            return variableAccess;
        }
        throw new JarvisException(MessageFormat.format("Cannot perform a deep copy of {0} ({1})", from.eClass()
                .getName(), from));
    }

}

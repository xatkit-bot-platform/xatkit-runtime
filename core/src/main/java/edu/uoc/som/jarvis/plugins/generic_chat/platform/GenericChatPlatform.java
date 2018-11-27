package edu.uoc.som.jarvis.plugins.generic_chat.platform;

import edu.uoc.som.jarvis.core.ExecutionService;
import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.JarvisException;
import edu.uoc.som.jarvis.core.platform.RuntimePlatform;
import edu.uoc.som.jarvis.core.platform.action.RuntimeAction;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.execution.*;
import edu.uoc.som.jarvis.intent.EventInstance;
import edu.uoc.som.jarvis.platform.ActionDefinition;
import edu.uoc.som.jarvis.plugins.generic_chat.platform.action.GenericChatAction;
import edu.uoc.som.jarvis.util.Loader;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;

/**
 * A generic {@link RuntimePlatform} class that wraps concrete chatting sub-platforms.
 * <p>
 * This class is used to define generic {@link ExecutionModel}s that can be bound to specific chatting platforms through
 * the provided {@link Configuration}. {@link ActionInstance}s created by this runtimePlatform wraps concrete
 * {@link ActionInstance}s defined in the concrete sub-platforms.
 * <p>
 * This runtimePlatform provides the following generic {@link RuntimeAction}s:
 * <ul>
 * <li>PostMessage: post a message to the channels specified in the provided {@link Configuration}</li>
 * </ul>
 * <b>Note:</b> this runtimePlatform only provides a concrete implementation of {@link GenericChatAction}, that is
 * used to
 * represent all the {@link RuntimeAction}s specified in the <i>GenericChatPlatform</i>. {@link GenericChatAction}
 * construction is handled by the {@link #createRuntimeAction(ActionInstance, JarvisSession)} method that overrides
 * the default behavior of {@link RuntimePlatform}.
 *
 * @see GenericChatAction
 */
public class GenericChatPlatform extends RuntimePlatform {

    /**
     * The {@link Configuration} key to store the concrete chat sub-platforms to use.
     *
     * @see #loadSubPlatform(String)
     */
    public static final String CONCRETE_CHAT_PLATFORMS_KEY = "jarvis.concrete.chat.platforms";

    /**
     * The {@link Configuration} key prefix used to bound specific values to generic action parameters.
     * <p>
     * As an example, this prefix is used to specify, for each sub-platform, the platform specific channel
     * associated to the generic <i>channel</i> parameter.
     */
    public static final String CONCRETE_CHAT_PROPERTY_PREFIX_KEY = "jarvis.concrete.chat";

    /**
     * The {@link RuntimePlatform} used to execute the concrete chatting {@link RuntimeAction}s.
     *
     * @see #loadSubPlatform(String)
     */
    private List<RuntimePlatform> subPlatforms;

    /**
     * Constructs a new {@link GenericChatPlatform} from the provided {@code jarvisCore} and {@link Configuration}.
     * <p>
     * This constructor initializes the underlying sub-platform list with the concrete {@link RuntimePlatform}
     * paths
     * retrieved from the {@link #CONCRETE_CHAT_PLATFORMS_KEY} {@link Configuration} property. Note that the property
     * can contain a single path or a list.
     * <p>
     * <b>Note:</b> this constructor will throw a {@link JarvisException} if at least one of the concrete sub-platforms
     * cannot be
     * built. The provided {@link Configuration} should contain all the sub-platform specific properties (e.g.
     * access
     * tokens).
     *
     * @param jarvisCore    the {@link JarvisCore} instance associated to this runtimePlatform
     * @param configuration the {@link Configuration} used to initialize the underlying sub-platform list
     * @throws NullPointerException     if the provided {@code jarvisCore} or {@link Configuration} is {@code null}
     * @throws IllegalArgumentException if the provided {@link Configuration} does not contain a
     *                                  {@link #CONCRETE_CHAT_PLATFORMS_KEY} property.
     * @see #CONCRETE_CHAT_PLATFORMS_KEY
     * @see #loadSubPlatform(String)
     */
    public GenericChatPlatform(JarvisCore jarvisCore, Configuration configuration) {
        super(jarvisCore, configuration);
        checkArgument(configuration.containsKey(CONCRETE_CHAT_PLATFORMS_KEY), "Cannot construct a %s: the provided %s" +
                " " +
                "does not contain the %s required property", this.getClass().getSimpleName(), Configuration.class
                .getSimpleName(), CONCRETE_CHAT_PLATFORMS_KEY);
        this.subPlatforms = new ArrayList<>();
        /*
         * We don't need to check if the property exists, it is already done by the checkArgument method.
         */
        Object concreteChatPlatformsProperty = configuration.getProperty(CONCRETE_CHAT_PLATFORMS_KEY);
        if (concreteChatPlatformsProperty instanceof List) {
            /*
             * The configuration defines multiple sub-platforms
             */
            for (Object o : (List) concreteChatPlatformsProperty) {
                if (o instanceof String) {
                    subPlatforms.add(loadSubPlatform((String) o));
                } else {
                    throw new JarvisException(MessageFormat.format("Cannot load the sub-platform from the provided " +
                                    "property %s, expected a %s value, found %s", o.toString(), String.class
                                    .getSimpleName(),
                            o.getClass().getSimpleName()));
                }
            }
        } else if (concreteChatPlatformsProperty instanceof String) {
            /*
             * The configuration defines a single sub-platform stored in a String
             */
            subPlatforms.add(loadSubPlatform((String) concreteChatPlatformsProperty));
        } else {
            /*
             * The configuration property type is neither a String or a List.
             */
            throw new JarvisException(MessageFormat.format("Cannot load the sub-platform from the provided " +
                    "property" +
                    " %s, expected a %s value, found %s", concreteChatPlatformsProperty.toString(), String.class
                    .getSimpleName(), concreteChatPlatformsProperty.getClass().getSimpleName()));
        }
    }

    /**
     * Loads and construct the {@link RuntimePlatform} at the given {@code platformPath}.
     * <p>
     * <b>Note:</b> this method does not check if another instance of the {@link RuntimePlatform} has been loaded by the
     * {@link JarvisCore} component. This should not create consistency issues because the sub-platforms are not
     * interacting with {@link RuntimePlatform}s loaded by the {@link JarvisCore}.
     *
     * @param platformPath the path of the {@link RuntimePlatform} to load
     * @return the constructed {@link RuntimePlatform}
     * @throws JarvisException if an error occurred when loading or constructing the {@link RuntimePlatform}
     * @see Loader#constructRuntimePlatform(Class, JarvisCore, Configuration)
     */
    private RuntimePlatform loadSubPlatform(String platformPath) {
        Class<? extends RuntimePlatform> runtimePlatformClass = Loader.loadClass(platformPath, RuntimePlatform.class);
        return Loader.constructRuntimePlatform(runtimePlatformClass, this.jarvisCore, this.configuration);
    }

    /**
     * Returns the {@link List} of sub-platforms managed by the {@link GenericChatPlatform}.
     *
     * @return the {@link List} of sub-platforms managed by the {@link GenericChatPlatform}
     */
    public List<RuntimePlatform> getSubPlatforms() {
        return subPlatforms;
    }

    /**
     * Retrieves an loads the {@link RuntimeAction} defined by the provided {@link ActionDefinition} in all the
     * sub-platforms.
     *
     * @param action the {@link ActionDefinition} definition representing the {@link RuntimeAction} to enable in all the
     *               sub-platforms
     */
    @Override
    public void enableAction(ActionDefinition action) {
        for (RuntimePlatform subPlatform : subPlatforms) {
            /*
             * This works because the actions have the same name
             */
            subPlatform.enableAction(action);
        }
    }

    /**
     * Disables the {@link RuntimeAction} defined by the provided {@link ActionDefinition} in all the sub-platforms.
     *
     * @param actionDefinition the {@link ActionDefinition} definition representing the {@link RuntimeAction} to
     *                         disable in all the sub-platforms
     */
    @Override
    public void disableAction(ActionDefinition actionDefinition) {
        for (RuntimePlatform subPlatform : subPlatforms) {
            /*
             * This works because the actions have the same name
             */
            subPlatform.disableAction(actionDefinition);
        }
    }

    /**
     * Creates a {@link GenericChatAction} wrapping the concrete sub-platform {@link RuntimeAction}s
     * corresponding to
     * the provided {@link ActionInstance}.
     * <p>
     * This method iterates the sub-platform list and calls their
     * {@link RuntimePlatform#createRuntimeAction(ActionInstance, JarvisSession)} method with the provided {@code
     * actionInstance} and {@code session}. The returned {@link RuntimeAction}s are then wrapped into a
     * {@link GenericChatAction} that takes care of executing them through the regular
     * {@link ExecutionService#handleEventInstance(EventInstance, JarvisSession)} process.
     *
     * @param actionInstance the {@link ActionInstance} representing the {@link RuntimeAction} to create
     * @param session        the {@link JarvisSession} associated to the action
     * @return the {@link GenericChatAction} wrapping the concrete sub-platform {@link RuntimeAction}s
     * @throws NullPointerException if the provided {@code actionInstance} or {@code session} is {@code null}
     * @throws JarvisException      if the provided {@link ActionInstance} does not match any {@link RuntimeAction}
     *                              for a sub-platform, or if an error occurred when building the
     *                              {@link RuntimeAction}
     * @see GenericChatAction
     */
    @Override
    public RuntimeAction createRuntimeAction(ActionInstance actionInstance, JarvisSession session) {
        /*
         * Do not check preconditions here, it is already done in sub-platforms.
         */
        List<RuntimeAction> concreteActions = new ArrayList<>();
        for (RuntimePlatform subPlatform : subPlatforms) {
            concreteActions.add(subPlatform.createRuntimeAction(reifyActionInstance(actionInstance, subPlatform),
                    session));
        }
        return new GenericChatAction(this, session, concreteActions);
    }

    /**
     * Reifies the provided {@code genericActionInstance} into its {@code concreteRuntimePlatform} implementation.
     * <p>
     * This method performs a deep copy of the provided {@code genericActionInstance}, and searches, for each of its
     * parameter, if a redefined value has been provided in the {@link Configuration} for the specified {@code
     * concreteRuntimePlatform}. If a parameter is not redefined in the {@link Configuration} the generic value is
     * used by the
     * {@code concreteRuntimePlatform} implementation.
     * <p>
     * Platform-specific parameters can be provided in the {@link Configuration} following this pattern: {@code
     * {@link #CONCRETE_CHAT_PROPERTY_PREFIX_KEY}.parameterKey.concrete_platform_name}. Note that the
     * concrete_platform_name must be lowercase.
     *
     * @param genericActionInstance   the {@link ActionInstance} to reify into its {@code concreteRuntimePlatform}
     *                                implementation
     * @param concreteRuntimePlatform the {@link RuntimePlatform} containing the target-specific implementation of the
     *                                {@code genericActionInstance}
     * @return the reified {@link ActionInstance}
     * @see #deepCopy(Expression)
     */
    private ActionInstance reifyActionInstance(ActionInstance genericActionInstance, RuntimePlatform
            concreteRuntimePlatform) {
        ActionInstance concreteActionInstance = ExecutionFactory.eINSTANCE.createActionInstance();
        concreteActionInstance.setAction(genericActionInstance.getAction());
        concreteActionInstance.setReturnVariable(genericActionInstance.getReturnVariable());
        for (ParameterValue genericPValue : genericActionInstance.getValues()) {
            ParameterValue concretePValue = ExecutionFactory.eINSTANCE.createParameterValue();
            concretePValue.setParameter(genericPValue.getParameter());
            concreteActionInstance.getValues().add(concretePValue);
            String parameterPropertyKey = CONCRETE_CHAT_PROPERTY_PREFIX_KEY + "." + genericPValue.getParameter()
                    .getKey().toLowerCase() + "." + concreteRuntimePlatform.getName().toLowerCase();
            Log.info("Checking property {0}", parameterPropertyKey);
            if (configuration.containsKey(parameterPropertyKey)) {
                /*
                 * The configuration provides a runtimePlatform-specific value of the parameter key, using it in the
                 * created
                 * ActionInstance.
                 */
                String concreteValue = configuration.getString(parameterPropertyKey);
                StringLiteral stringLiteral = ExecutionFactory.eINSTANCE.createStringLiteral();
                stringLiteral.setValue(concreteValue);
                concretePValue.setExpression(stringLiteral);
            } else {
                /*
                 * The configuration does not provide a runtimePlatform-specific value of the parameter key, copying the
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
        ExecutionFactory f = ExecutionFactory.eINSTANCE;
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

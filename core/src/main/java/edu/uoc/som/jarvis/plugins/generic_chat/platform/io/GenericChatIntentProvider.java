package edu.uoc.som.jarvis.plugins.generic_chat.platform.io;

import edu.uoc.som.jarvis.core.JarvisException;
import edu.uoc.som.jarvis.core.platform.RuntimePlatform;
import edu.uoc.som.jarvis.core.platform.io.RuntimeEventProvider;
import edu.uoc.som.jarvis.core.platform.io.RuntimeIntentProvider;
import edu.uoc.som.jarvis.plugins.generic_chat.platform.GenericChatPlatform;
import edu.uoc.som.jarvis.util.Loader;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;

import java.text.MessageFormat;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

/**
 * A generic {@link RuntimeIntentProvider} that wraps a concrete chatting {@link RuntimeIntentProvider}.
 * <p>
 * This class is used to define generic execution models that can be bound to specific chatting platforms through
 * the provided {@link Configuration}.
 * <p>
 * The specific {@link RuntimeIntentProvider} wrapped by this class should be defined in the
 * {@link #CONCRETE_CHAT_INTENT_PROVIDER_KEY} property of the provided {@link Configuration}. Note that only the name
 * of the {@link RuntimeIntentProvider} is required in the {@link Configuration}, the {@link GenericChatIntentProvider} will
 * take care of retrieving the corresponding class in its {@code runtimePlatform}'s sub-platform classpath and
 * constructing it.
 */
public class GenericChatIntentProvider extends RuntimeIntentProvider<GenericChatPlatform> {

    /**
     * The {@link Configuration} key to store the concrete {@link RuntimeIntentProvider} to use.
     */
    public static final String CONCRETE_CHAT_INTENT_PROVIDER_KEY = "jarvis.concrete.chat.intent.provider";

    /**
     * The concrete {@link RuntimeIntentProvider} that extracts {@link edu.uoc.som.jarvis.intent.RecognizedIntent}s from the
     * chatting platform.
     */
    private RuntimeIntentProvider concreteRuntimeIntentProvider;

    /**
     * Constructs a new {@link GenericChatIntentProvider} with the provided {@code runtimePlatform} and {@code
     * configuration}.
     * <p>
     * This constructor initializes the concrete chatting {@link RuntimeIntentProvider} from the
     * {@link #CONCRETE_CHAT_INTENT_PROVIDER_KEY} {@link Configuration} property.
     * <p>
     * <b>Note:</b> this constructor will throw a {@link JarvisException} if an error occurred when building the
     * concrete {@link RuntimeIntentProvider}. The provided {@link Configuration} should contain all the
     * {@link RuntimeIntentProvider} specific properties (e.g. access tokens).
     *
     * @param runtimePlatform the {@link RuntimePlatform} containing this {@link GenericChatIntentProvider}
     * @param configuration    the {@link Configuration} used to initialize the concrete {@link RuntimeIntentProvider}
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code configuration} is {@code
     *                                  null}
     * @throws IllegalArgumentException if the provided {@link Configuration} does not contain a
     *                                  {@link #CONCRETE_CHAT_INTENT_PROVIDER_KEY} property
     */
    public GenericChatIntentProvider(GenericChatPlatform runtimePlatform, Configuration configuration) {
        super(runtimePlatform, configuration);
        checkNotNull(configuration, "Cannot construct a %s from the provided %s %s", this.getClass().getSimpleName(),
                Configuration.class.getSimpleName(), configuration);
        checkArgument(configuration.containsKey(CONCRETE_CHAT_INTENT_PROVIDER_KEY), "Cannot construct a %s: the " +
                        "provided %s does not contain the %s required property", this.getClass().getSimpleName(),
                Configuration.class.getSimpleName(), CONCRETE_CHAT_INTENT_PROVIDER_KEY);
        String concreteIntentProviderName = configuration.getString(CONCRETE_CHAT_INTENT_PROVIDER_KEY);
        RuntimeEventProvider runtimeEventProvider = null;
        for (RuntimePlatform subPlatform : this.runtimePlatform.getSubPlatforms()) {
            String eventProviderQualifiedName = subPlatform.getClass().getPackage().getName() + ".io." +
                    concreteIntentProviderName;
            try {
                Class<? extends RuntimeEventProvider> eventProviderClass = Loader.loadClass(eventProviderQualifiedName,
                        RuntimeEventProvider.class);
                try {
                    runtimeEventProvider = Loader.constructRuntimeEventProvider(eventProviderClass, subPlatform, configuration);
                } catch (JarvisException e1) {
                    Log.error(e1, "An error occurred when constructing the %s instance", eventProviderClass
                            .getSimpleName());
                    throw e1;
                }
                /*
                 * The class has been loaded and constructed, we don't need to iterate the other sub-platforms.
                 */
                break;
            } catch (JarvisException e) {
                /*
                 * Unable to load the class. This exception is thrown if the RuntimeIntentProvider is not defined in the
                 * current sub-runtimePlatform (which is possible since all the sub-platforms are iterated), or if an
                 * error occurred when constructing the RuntimeIntentProvider instance.
                 * We can silently ignore this exception, and continue the iteration on sub-platforms in order to find
                 * an RuntimeIntentProvider that can be created. If no RuntimeIntentProvider is found the method will throw an
                 * exception when checking the constructed instance.
                 */
            }
        }
        if (isNull(runtimeEventProvider)) {
            /*
             * Unable to load and construct the RuntimeIntentProvider.
             */
            throw new JarvisException(MessageFormat.format("Cannot construct the {0}: unable to load and construct a " +
                    "concrete {1}", this.getClass().getSimpleName(), RuntimeIntentProvider.class.getSimpleName()));
        }
        if (runtimeEventProvider instanceof RuntimeIntentProvider) {
            concreteRuntimeIntentProvider = (RuntimeIntentProvider) runtimeEventProvider;
        } else {
            /*
             * The constructed instance is not an RuntimeIntentProvider.
             */
            throw new JarvisException(MessageFormat.format("Cannot construct the {0}: the underlying {1} is not an " +
                    "{2}", this.getClass().getSimpleName(), runtimeEventProvider.getClass().getSimpleName(), RuntimeIntentProvider
                    .class.getSimpleName()));
        }
    }

    /**
     * Runs the underlying {@link RuntimeIntentProvider}.
     */
    @Override
    public void run() {
        concreteRuntimeIntentProvider.run();
    }

    /**
     * Closes the underlying {@link RuntimeIntentProvider}.
     */
    @Override
    public void close() {
        concreteRuntimeIntentProvider.close();
    }
}

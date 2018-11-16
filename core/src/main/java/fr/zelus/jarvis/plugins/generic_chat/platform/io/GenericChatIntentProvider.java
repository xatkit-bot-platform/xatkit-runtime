package fr.zelus.jarvis.plugins.generic_chat.platform.io;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.core.RuntimePlatform;
import fr.zelus.jarvis.io.EventProvider;
import fr.zelus.jarvis.io.IntentProvider;
import fr.zelus.jarvis.plugins.generic_chat.platform.GenericChatPlatform;
import fr.zelus.jarvis.util.Loader;
import org.apache.commons.configuration2.Configuration;

import java.text.MessageFormat;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

/**
 * A generic {@link IntentProvider} that wraps a concrete chatting {@link IntentProvider}.
 * <p>
 * This class is used to define generic execution models that can be bound to specific chatting platforms through
 * the provided {@link Configuration}.
 * <p>
 * The specific {@link IntentProvider} wrapped by this class should be defined in the
 * {@link #CONCRETE_CHAT_INTENT_PROVIDER_KEY} property of the provided {@link Configuration}. Note that only the name
 * of the {@link IntentProvider} is required in the {@link Configuration}, the {@link GenericChatIntentProvider} will
 * take care of retrieving the corresponding class in its {@code runtimePlatform}'s sub-platform classpath and
 * constructing it.
 */
public class GenericChatIntentProvider extends IntentProvider<GenericChatPlatform> {

    /**
     * The {@link Configuration} key to store the concrete {@link IntentProvider} to use.
     */
    public static final String CONCRETE_CHAT_INTENT_PROVIDER_KEY = "jarvis.concrete.chat.intent.provider";

    /**
     * The concrete {@link IntentProvider} that extracts {@link fr.zelus.jarvis.intent.RecognizedIntent}s from the
     * chatting platform.
     */
    private IntentProvider concreteIntentProvider;

    /**
     * Constructs a new {@link GenericChatIntentProvider} with the provided {@code runtimePlatform} and {@code
     * configuration}.
     * <p>
     * This constructor initializes the concrete chatting {@link IntentProvider} from the
     * {@link #CONCRETE_CHAT_INTENT_PROVIDER_KEY} {@link Configuration} property.
     * <p>
     * <b>Note:</b> this constructor will throw a {@link JarvisException} if an error occurred when building the
     * concrete {@link IntentProvider}. The provided {@link Configuration} should contain all the
     * {@link IntentProvider} specific properties (e.g. access tokens).
     *
     * @param runtimePlatform the {@link RuntimePlatform} containing this {@link GenericChatIntentProvider}
     * @param configuration    the {@link Configuration} used to initialize the concrete {@link IntentProvider}
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
        EventProvider eventProvider = null;
        for (RuntimePlatform subPlatform : this.runtimePlatform.getSubPlatforms()) {
            String eventProviderQualifiedName = subPlatform.getClass().getPackage().getName() + ".io." +
                    concreteIntentProviderName;
            try {
                Class<? extends EventProvider> eventProviderClass = Loader.loadClass(eventProviderQualifiedName,
                        EventProvider.class);
                try {
                    eventProvider = Loader.constructEventProvider(eventProviderClass, subPlatform, configuration);
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
                 * Unable to load the class. This exception is thrown if the IntentProvider is not defined in the
                 * current sub-runtimePlatform (which is possible since all the sub-platforms are iterated), or if an
                 * error occurred when constructing the IntentProvider instance.
                 * We can silently ignore this exception, and continue the iteration on sub-platforms in order to find
                 * an IntentProvider that can be created. If no IntentProvider is found the method will throw an
                 * exception when checking the constructed instance.
                 */
            }
        }
        if (isNull(eventProvider)) {
            /*
             * Unable to load and construct the IntentProvider.
             */
            throw new JarvisException(MessageFormat.format("Cannot construct the {0}: unable to load and construct a " +
                    "concrete {1}", this.getClass().getSimpleName(), IntentProvider.class.getSimpleName()));
        }
        if (eventProvider instanceof IntentProvider) {
            concreteIntentProvider = (IntentProvider) eventProvider;
        } else {
            /*
             * The constructed instance is not an IntentProvider.
             */
            throw new JarvisException(MessageFormat.format("Cannot construct the {0}: the underlying {1} is not an " +
                    "{2}", this.getClass().getSimpleName(), eventProvider.getClass().getSimpleName(), IntentProvider
                    .class.getSimpleName()));
        }
    }

    /**
     * Runs the underlying {@link IntentProvider}.
     */
    @Override
    public void run() {
        concreteIntentProvider.run();
    }

    /**
     * Closes the underlying {@link IntentProvider}.
     */
    @Override
    public void close() {
        concreteIntentProvider.close();
    }
}

package com.xatkit.core.recognition;

import com.xatkit.core.EventDefinitionRegistry;
import com.xatkit.core.XatkitBot;
import com.xatkit.core.XatkitException;
import com.xatkit.core.recognition.processor.InputPreProcessor;
import com.xatkit.core.recognition.processor.IntentPostProcessor;
import com.xatkit.core.recognition.regex.RegExIntentRecognitionProvider;
import com.xatkit.core.server.XatkitServer;
import com.xatkit.util.Loader;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

/**
 * Builds {@link IntentRecognitionProvider}s from the provided {@code configuration}.
 * <p>
 * This factory inspects the provided {@code configuration} and finds the concrete {@link IntentRecognitionProvider}
 * to construct. If the provided {@code configuration} does not define any {@link IntentRecognitionProvider}, a
 * {@link RegExIntentRecognitionProvider} is returned, providing minimal NLP support.
 *
 * @see IntentRecognitionProvider
 * @see RegExIntentRecognitionProvider
 */
public final class IntentRecognitionProviderFactory {

    /**
     * Disables the default constructor, this class only provides static methods and should not be constructed.
     */
    private IntentRecognitionProviderFactory() {
    }

    /**
     * The database that will be used by this instance of Xatkit to store logs.
     * <p>
     * If this property is not set the bot won't use logging.
     * <p>
     * The value of this property is the fully-qualified name of the {@link RecognitionMonitor} subclass to
     * instantiate. Libraries providing such implementations typically provide a constant value for that.
     */
    public static final String LOGS_DATABASE = "xatkit.logs.database";

    /**
     * The intent provider that will be used for this instance of Xatkit.
     * <p>
     * If this property isn't set the {@link RegExIntentRecognitionProvider} will be used.
     * <p>
     * The value of this property is the fully-qualified name of the {@link IntentRecognitionProvider} subclass to
     * instantiate. Libraries providing such implementations typically provide a constant value for that in their
     * configuration.
     */
    public static final String INTENT_PROVIDER_KEY = "xatkit.intent.provider";

    /**
     * Returns the {@link AbstractIntentRecognitionProvider} matching the provided {@code configuration}.
     * <p>
     * If the provided {@code configuration} does not define any {@link AbstractIntentRecognitionProvider}, a
     * {@link RegExIntentRecognitionProvider} is returned, providing minimal NLP support.
     * <p>
     * The created {@link AbstractIntentRecognitionProvider} embeds a {@link RecognitionMonitor} that logs monitoring
     * information regarding the intent recognition. The {@link RecognitionMonitor} can be disabled by setting the
     * {@link IntentRecognitionProviderFactoryConfiguration#ENABLE_RECOGNITION_ANALYTICS} property to {@code false}
     * in the configuration.
     * <p>
     * This method retrieves the list of {@link InputPreProcessor}s and {@link IntentPostProcessor}s from the
     * provided {@code configuration} and bind them to the returned {@link AbstractIntentRecognitionProvider}. Pre/post
     * -processors are specified with the configuration keys
     * {@link IntentRecognitionProviderFactoryConfiguration#RECOGNITION_PREPROCESSORS_KEY} and
     * {@link IntentRecognitionProviderFactoryConfiguration#RECOGNITION_POSTPROCESSORS_KEY}, respectively, and are
     * specified as comma-separated list of processor's names.
     *
     * @param xatkitBot         the {@link XatkitBot} instance to build the
     *                          {@link AbstractIntentRecognitionProvider} from
     * @param baseConfiguration the {@link Configuration} used to define the
     *                          {@link AbstractIntentRecognitionProvider} to build
     * @return the {@link AbstractIntentRecognitionProvider} matching the provided {@code configuration}
     * @throws XatkitException      if an error occurred when loading the pre/post processors.
     * @throws NullPointerException if the provided {@code xatkitBot} is {@code null}
     * @see IntentRecognitionProviderFactoryConfiguration#RECOGNITION_PREPROCESSORS_KEY
     * @see IntentRecognitionProviderFactoryConfiguration#RECOGNITION_POSTPROCESSORS_KEY
     */
    public static IntentRecognitionProvider getIntentRecognitionProvider(@NonNull XatkitBot xatkitBot,
                                                                         @NonNull Configuration baseConfiguration) {
        IntentRecognitionProviderFactoryConfiguration configuration =
                new IntentRecognitionProviderFactoryConfiguration(baseConfiguration);

        RecognitionMonitor recognitionMonitor = getRecognitionMonitor(xatkitBot, configuration);

        List<InputPreProcessor> preProcessors;
        List<IntentPostProcessor> postProcessors;

        try {
            preProcessors = loadPreProcessors(configuration);
            postProcessors = loadPostProcessors(configuration);
        } catch (Exception e) {
            /*
             * Make sure we close the recognition monitor in case something bad happens while loading the processors.
             * We don't want to run into lock errors, especially in the test cases.
             */
            if (nonNull(recognitionMonitor)) {
                recognitionMonitor.shutdown();
            }
            throw e;
        }

        IntentRecognitionProvider provider;

        if (baseConfiguration.containsKey(INTENT_PROVIDER_KEY)) {
            try {
                String providerClassName = baseConfiguration.getString(INTENT_PROVIDER_KEY);
                Class<? extends IntentRecognitionProvider> providerClass =
                        (Class<? extends IntentRecognitionProvider>) Class.forName(providerClassName);
                Constructor<? extends IntentRecognitionProvider> providerConstructor =
                        providerClass.getConstructor(EventDefinitionRegistry.class, Configuration.class,
                                RecognitionMonitor.class);
                provider = providerConstructor.newInstance(xatkitBot.getEventDefinitionRegistry(), baseConfiguration,
                        recognitionMonitor);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                    IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            /*
             * The provided configuration does not contain any IntentRecognitionProvider information, returning a
             * RegExIntentRecognitionProvider.
             */
            provider = new RegExIntentRecognitionProvider(baseConfiguration, recognitionMonitor);
        }
        provider.setPreProcessors(preProcessors);
        for (InputPreProcessor preProcessor : preProcessors) {
            /*
             * Initialize the pre-processors once they have all been constructed, this way we can initialize third
             * party services that are used by multiple pre-processors.
             */
            preProcessor.init();
        }
        provider.setPostProcessors(postProcessors);
        for (IntentPostProcessor postProcessor : postProcessors) {
            /*
             * Initialize the post-processors once they have all been constructed, this way we can initialize third
             * party services that are used by multiple post-processors.
             */
            postProcessor.init();
        }
        return provider;
    }

    /**
     * Retrieves and creates the {@link RecognitionMonitor} from the provided {@link Configuration}.
     *
     * @param xatkitBot     the {@link XatkitBot} used to initialize the {@link RecognitionMonitor}
     * @param configuration the {@link Configuration} used to initialize the {@link RecognitionMonitor}
     * @return the created {@link RecognitionMonitor}, or {@code null} intent recognition monitoring is disabled in
     * the provided {@link Configuration}
     * @see IntentRecognitionProviderFactoryConfiguration#ENABLE_RECOGNITION_ANALYTICS
     */
    @Nullable
    private static RecognitionMonitor getRecognitionMonitor(XatkitBot xatkitBot,
                                                            IntentRecognitionProviderFactoryConfiguration configuration) {
        /*
         * TODO this should be extracted in XatkitBot
         */
        RecognitionMonitor monitor = null;
        if (configuration.isEnableRecognitionAnalytics()) {
            if (configuration.getBaseConfiguration().containsKey(LOGS_DATABASE)) {
                try {
                    String databaseClassName = configuration.getBaseConfiguration().getString(LOGS_DATABASE);
                    Class<? extends RecognitionMonitor> databaseClass =
                            (Class<? extends RecognitionMonitor>) Class.forName(databaseClassName);
                    Constructor<? extends RecognitionMonitor> databaseConstructor =
                            databaseClass.getConstructor(XatkitServer.class, Configuration.class);
                    monitor = databaseConstructor.newInstance(xatkitBot.getXatkitServer(),
                            configuration.getBaseConfiguration());
                } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                        IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else {
                Log.error("Analytics are enabled but no database provided for storing logs. Please provide a value "
                        + "for {0}", LOGS_DATABASE);
            }
        } else {
            Log.debug("Analytics are disabled");
        }
        return monitor;
    }

    private static List<InputPreProcessor> loadPreProcessors(
            IntentRecognitionProviderFactoryConfiguration configuration) {
        return configuration.getPreProcessorNames().stream().map(preProcessorName -> {
            Class<? extends InputPreProcessor> processor;
            try {
                processor = Loader.loadClass(preProcessorName, InputPreProcessor.class);
            } catch (XatkitException e) {
                throw new XatkitException(e);
            }
            try {
                return Loader.construct(processor, new Object[]{configuration.getBaseConfiguration()});
            } catch (NoSuchMethodException e) {
                /*
                 * Try to construct the processor without the configuration
                 */
                return Loader.construct(processor);
            } catch (InvocationTargetException e) {
                /*
                 * Wrap the exception into an unchecked exception
                 */
                throw new XatkitException(e);
            }
        }).collect(Collectors.toList());
    }

    private static List<IntentPostProcessor> loadPostProcessors(IntentRecognitionProviderFactoryConfiguration configuration) {
        return configuration.getPostProcessorNames().stream().map(postProcessorName -> {
            Class<? extends IntentPostProcessor> processor;
            try {
                processor = Loader.loadClass(postProcessorName, IntentPostProcessor.class);
            } catch (XatkitException e) {
                throw new XatkitException(e);
            }
            try {
                return Loader.construct(processor, new Object[]{configuration.getBaseConfiguration()});
            } catch (NoSuchMethodException e) {
                /*
                 * Try to construct the processor without the configuration
                 */
                return Loader.construct(processor);
            } catch (InvocationTargetException e) {
                /*
                 * Wrap the exception into an unchecked exception
                 */
                throw new XatkitException(e);
            }
        }).collect(Collectors.toList());
    }
}

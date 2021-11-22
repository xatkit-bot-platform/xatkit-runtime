package com.xatkit.core.recognition;

import com.xatkit.core.XatkitBot;
import com.xatkit.core.XatkitException;
import com.xatkit.core.recognition.dialogflow.DialogFlowConfiguration;
import com.xatkit.core.recognition.dialogflow.DialogFlowIntentRecognitionProvider;
import com.xatkit.core.recognition.nlpjs.NlpjsConfiguration;
import com.xatkit.core.recognition.nlpjs.NlpjsIntentRecognitionProvider;
import com.xatkit.core.recognition.processor.InputPreProcessor;
import com.xatkit.core.recognition.processor.IntentPostProcessor;
import com.xatkit.core.recognition.regex.RegExIntentRecognitionProvider;
import com.xatkit.util.Loader;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;

import javax.annotation.Nullable;
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
     * The database model that will be used for this instance of Xatkit.
     * <p>
     * This property is optional, and it's default value is set to "MAPDB" if not specified
     */
    public static final String DATABASE_MODEL_KEY = "xatkit.database.model";

    /**
     * The default value for xatkit.database.model in case it's not specified in the properties file.
     */
    public static final String DATABASE_MODEL_MAPDB = "mapdb";

    public static final String DATABASE_MODEL_INFLUXDB = "influxdb";

    public static final String DATABASE_MODEL_POSTGRESDB = "postgresdb";

    public static final String DEFAULT_DATABASE_MODEL = DATABASE_MODEL_MAPDB;

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

        if (baseConfiguration.containsKey(DialogFlowConfiguration.PROJECT_ID_KEY)) {
            /*
             * The provided configuration contains DialogFlow-related information.
             */
            provider = new DialogFlowIntentRecognitionProvider(xatkitBot.getEventDefinitionRegistry(),
                    baseConfiguration,
                    recognitionMonitor);
        } else if (baseConfiguration.containsKey(NlpjsConfiguration.AGENT_ID_KEY)) {
            /*
             * The provided configuration contains NLP.js-related information.
             */
            provider = new NlpjsIntentRecognitionProvider(xatkitBot.getEventDefinitionRegistry(), baseConfiguration,
                    recognitionMonitor);
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
            if (configuration.getBaseConfiguration().getString(DATABASE_MODEL_KEY, DEFAULT_DATABASE_MODEL)
                    .toLowerCase().equals(DATABASE_MODEL_INFLUXDB)) {
                Log.info("Using InfluxDB to store monitoring data");
                monitor = new RecognitionMonitorInflux(xatkitBot.getXatkitServer(),
                        configuration.getBaseConfiguration());
            } else {
                Log.info("Using MapDB to store monitoring data");
                monitor = new RecognitionMonitorMapDB(xatkitBot.getXatkitServer(),
                        configuration.getBaseConfiguration());
            }
        }
        return monitor;
    }

    private static List<InputPreProcessor> loadPreProcessors(
            IntentRecognitionProviderFactoryConfiguration configuration) {
        return configuration.getPreProcessorNames().stream().map(preProcessorName -> {
            Class<? extends InputPreProcessor> processor;
            try {
                processor = Loader.loadClass("com.xatkit.core.recognition.processor." + preProcessorName
                                + "PreProcessor",
                        InputPreProcessor.class);
            } catch (XatkitException e) {
                /*
                 * Try to load it without the suffix
                 */
                processor = Loader.loadClass("com.xatkit.core.recognition.processor." + preProcessorName,
                        InputPreProcessor.class);
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
                processor = Loader.loadClass("com.xatkit.core.recognition.processor." + postProcessorName
                        + "PostProcessor", IntentPostProcessor.class);
            } catch (XatkitException e) {
                /*
                 * Try to load it without the suffix
                 */
                processor = Loader.loadClass("com.xatkit.core.recognition.processor." + postProcessorName,
                        IntentPostProcessor.class);
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

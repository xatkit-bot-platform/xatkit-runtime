package com.xatkit.core.recognition;

import com.xatkit.core.XatkitBot;
import com.xatkit.core.XatkitException;
import com.xatkit.core.recognition.dialogflow.DialogFlowConfiguration;
import com.xatkit.core.recognition.dialogflow.DialogFlowIntentRecognitionProvider;
import com.xatkit.core.recognition.processor.InputPreProcessor;
import com.xatkit.core.recognition.processor.IntentPostProcessor;
import com.xatkit.core.recognition.regex.RegExIntentRecognitionProvider;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.util.Loader;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds {@link IntentRecognitionProvider}s from the provided {@code configuration}.
 * <p>
 * This factory inspects the provided {@code configuration} and finds the concrete {@link IntentRecognitionProvider}
 * to construct. If the provided {@code configuration} does not define any {@link IntentRecognitionProvider}, a
 * {@link RegExIntentRecognitionProvider} is returned, providing minimal NLP support.
 * <p>
 * <b>Note:</b> {@link RegExIntentRecognitionProvider} does not handle {@link IntentDefinition} and
 * {@link RecognizedIntent} computation. If the bot application requires such features a valid
 * {@link IntentRecognitionProvider} must be specified in the provided configuration.
 *
 * @see IntentRecognitionProvider
 * @see RegExIntentRecognitionProvider
 */
public class IntentRecognitionProviderFactory {

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

        List<? extends InputPreProcessor> preProcessors = loadPreProcessors(configuration.getPreProcessorNames());
        List<? extends IntentPostProcessor> postProcessors = loadPostProcessors(configuration.getPostProcessorNames());

        IntentRecognitionProvider provider;

        if (baseConfiguration.containsKey(DialogFlowConfiguration.PROJECT_ID_KEY)) {
            /*
             * The provided configuration contains DialogFlow-related information.
             */
            provider = new DialogFlowIntentRecognitionProvider(xatkitBot.getEventDefinitionRegistry(),
                    baseConfiguration,
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
            monitor = new RecognitionMonitor(xatkitBot.getXatkitServer(), configuration.getBaseConfiguration());
        }
        return monitor;
    }

    private static List<? extends InputPreProcessor> loadPreProcessors(List<String> preProcessorNames) {
        return preProcessorNames.stream().map(preProcessorName -> {
            Class<? extends InputPreProcessor> processor;
            try {
                processor = Loader.loadClass("com.xatkit.core.recognition.processor." + preProcessorName +
                                "PreProcessor",
                        InputPreProcessor.class);
            } catch (XatkitException e) {
                /*
                 * Try to load it without the suffix
                 */
                processor = Loader.loadClass("com.xatkit.core.recognition.processor." + preProcessorName,
                        InputPreProcessor.class);
            }
            return Loader.construct(processor);
        }).collect(Collectors.toList());
    }

    private static List<? extends IntentPostProcessor> loadPostProcessors(List<String> postProcessorNames) {
        return postProcessorNames.stream().map(postProcessorName -> {
            Class<? extends IntentPostProcessor> processor;
            try {
                processor = Loader.loadClass("com.xatkit.core.recognition.processor." + postProcessorName +
                        "PostProcessor", IntentPostProcessor.class);
            } catch (XatkitException e) {
                /*
                 * Try to load it without the suffix
                 */
                processor = Loader.loadClass("com.xatkit.core.recognition.processor." + postProcessorName,
                        IntentPostProcessor.class);
            }
            return Loader.construct(processor);
        }).collect(Collectors.toList());
    }


}

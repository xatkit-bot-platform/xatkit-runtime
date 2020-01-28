package com.xatkit.core.recognition;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.XatkitException;
import com.xatkit.core.recognition.dialogflow.DialogFlowApi;
import com.xatkit.core.recognition.processor.InputPreProcessor;
import com.xatkit.core.recognition.processor.IntentPostProcessor;
import com.xatkit.core.recognition.regex.RegExIntentRecognitionProvider;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.util.Loader;
import org.apache.commons.configuration2.Configuration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

/**
 * Builds {@link IntentRecognitionProvider}s from the provided {@code configuration}.
 * <p>
 * This factory inspects the provided {@code configuration} and finds the concrete {@link IntentRecognitionProvider}
 * to construct. If the provided {@code configuration} does not define any {@link IntentRecognitionProvider}, a
 * {@link RegExIntentRecognitionProvider} is returned, providing minimal support to
 * {@link XatkitSession} management.
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
     * The {@link Configuration} key used to specify whether to enable intent recognition monitoring.
     * <p>
     * Intent recognition monitoring is enabled by default, and stores the results in the {@code data/analytics}
     * folder. It can be disabled by explicitly setting this property to {@code false} in the {@link Configuration}
     * file.
     */
    public static String ENABLE_RECOGNITION_ANALYTICS = "xatkit.recognition.enable_monitoring";

    /**
     * The {@link Configuration} key used to specify the {@link InputPreProcessor}s associated to the created
     * {@link IntentRecognitionProvider}.
     * <p>
     * {@link InputPreProcessor}s are specified as a comma-separated list of processor's names.
     */
    public static String RECOGNITION_PREPROCESSORS_KEY = "xatkit.recognition.preprocessors";

    /**
     * The {@link Configuration} key used to specify the {@link IntentPostProcessor}s associated to the created
     * {@link IntentRecognitionProvider}.
     * <p>
     * {@link IntentPostProcessor}s are specified as a comma-separated list of processor's names.
     */
    public static String RECOGNITION_POSTPROCESSORS_KEY = "xatkit.recognition.postprocessors";

    /**
     * Returns the {@link IntentRecognitionProvider} matching the provided {@code configuration}.
     * <p>
     * If the provided {@code configuration} does not define any {@link IntentRecognitionProvider}, a
     * {@link RegExIntentRecognitionProvider} is returned, providing minimal support to
     * {@link XatkitSession} management.
     * <p>
     * The created {@link IntentRecognitionProvider} embeds a {@link RecognitionMonitor} that logs monitoring
     * information regarding the intent recognition. The {@link RecognitionMonitor} can be disabled by setting the
     * {@link #ENABLE_RECOGNITION_ANALYTICS} property to {@code false} in the configuration.
     * <p>
     * This method retrieves the list of {@link InputPreProcessor}s and {@link IntentPostProcessor}s from the
     * provided {@code configuration} and bind them to the returned {@link IntentRecognitionProvider}. Pre/post
     * -processors are specified with the configuration keys {@link #RECOGNITION_PREPROCESSORS_KEY} and
     * {@link #RECOGNITION_POSTPROCESSORS_KEY}, respectively, and are specified as comma-separated list of
     * processor's names.
     *
     * @param xatkitCore    the {@link XatkitCore} instance to build the {@link IntentRecognitionProvider} from
     * @param configuration the {@link Configuration} used to define the {@link IntentRecognitionProvider} to build
     * @return the {@link IntentRecognitionProvider} matching the provided {@code configuration}
     * @throws XatkitException if an error occurred when loading the pre/post processors.
     * @see #getRecognitionMonitor(XatkitCore, Configuration)
     * @see #getPreProcessors(Configuration)
     * @see #getPostProcessors(Configuration)
     * @see #RECOGNITION_PREPROCESSORS_KEY
     * @see #RECOGNITION_POSTPROCESSORS_KEY
     */
    public static IntentRecognitionProvider getIntentRecognitionProvider(XatkitCore xatkitCore, Configuration
            configuration) {
        checkNotNull(xatkitCore, "Cannot get an %s from the provided %s %s", IntentRecognitionProvider.class
                .getSimpleName(), XatkitCore.class.getSimpleName(), xatkitCore);
        checkNotNull(configuration, "Cannot get an %s the provided %s %s", IntentRecognitionProvider.class
                .getSimpleName(), Configuration.class.getSimpleName(), configuration);
        RecognitionMonitor recognitionMonitor = getRecognitionMonitor(xatkitCore, configuration);

        List<? extends InputPreProcessor> preProcessors = getPreProcessors(configuration);
        List<? extends IntentPostProcessor> postProcessors = getPostProcessors(configuration);

        IntentRecognitionProvider provider;

        if (configuration.containsKey(DialogFlowApi.PROJECT_ID_KEY)) {
            /*
             * The provided configuration contains DialogFlow-related information.
             */
            provider = new DialogFlowApi(xatkitCore, configuration, recognitionMonitor);
        } else {
            /*
             * The provided configuration does not contain any IntentRecognitionProvider information, returning a
             * RegExIntentRecognitionProvider.
             */
            provider = new RegExIntentRecognitionProvider(configuration, recognitionMonitor);
        }
        provider.setPreProcessors(preProcessors);
        provider.setPostProcessors(postProcessors);
        return provider;
    }

    /**
     * Retrieves and creates the {@link RecognitionMonitor} from the provided {@link Configuration}.
     *
     * @param xatkitCore    the {@link XatkitCore} used to initialize the {@link RecognitionMonitor}
     * @param configuration the {@link Configuration} used to initialize the {@link RecognitionMonitor}
     * @return the created {@link RecognitionMonitor}, or {@code null} intent recognition monitoring is disabled in
     * the provided {@link Configuration}
     * @see #ENABLE_RECOGNITION_ANALYTICS
     */
    @Nullable
    private static RecognitionMonitor getRecognitionMonitor(XatkitCore xatkitCore,
                                                            Configuration configuration) {
        boolean enableRecognitionAnalytics = configuration.getBoolean(ENABLE_RECOGNITION_ANALYTICS, true);
        RecognitionMonitor monitor = null;
        if (enableRecognitionAnalytics) {
            monitor = new RecognitionMonitor(xatkitCore.getXatkitServer(), configuration);
        }
        return monitor;
    }

    private static List<? extends InputPreProcessor> getPreProcessors(Configuration configuration) {
        List<String> preProcessorNames = getList(configuration, RECOGNITION_PREPROCESSORS_KEY);
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

    private static List<? extends IntentPostProcessor> getPostProcessors(Configuration configuration) {
        List<String> postProcessorNames = getList(configuration, RECOGNITION_POSTPROCESSORS_KEY);
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

    /**
     * Returns a {@link List} extracted from the value associated to the provided {@code key} in the given {@code
     * configuration}.
     * <p>
     * <b>Note</b>: this method is a workaround, the correct way to implement it is to use {@code configuration
     * .setListDelimiterHandler}, but this method does not work with the DefaultConversionHandler embedded in the
     * configuration.
     *
     * @param configuration the {@link Configuration} to get the {@link List} from
     * @param key           the key to retrieve the {@link List} from
     * @return the {@link List}, or {@code null} if the provided {@code key} is not contained in the {@code
     * configuration}
     */
    private static @Nonnull
    List<String> getList(Configuration configuration, String key) {
        String value = configuration.getString(key);
        if (isNull(value)) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toList());
    }
}

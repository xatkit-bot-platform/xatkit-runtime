package com.xatkit.core.recognition;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.monitoring.RecognitionMonitor;
import com.xatkit.core.recognition.dialogflow.DialogFlowApi;
import com.xatkit.core.recognition.regex.RegExIntentRecognitionProvider;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.RecognizedIntent;
import org.apache.commons.configuration2.Configuration;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

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
     * Returns the {@link IntentRecognitionProvider} matching the provided {@code configuration}.
     * <p>
     * If the provided {@code configuration} does not define any {@link IntentRecognitionProvider}, a
     * {@link RegExIntentRecognitionProvider} is returned, providing minimal support to
     * {@link XatkitSession} management.
     * <p>
     * The created {@link IntentRecognitionProvider} embeds a {@link RecognitionMonitor} that logs monitoring
     * information regarding the intent recognition. The {@link RecognitionMonitor} can be disabled by setting the
     * {@link #ENABLE_RECOGNITION_ANALYTICS} property to {@code false} in the configuration.
     *
     * @param xatkitCore    the {@link XatkitCore} instance to build the {@link IntentRecognitionProvider} from
     * @param configuration the {@link Configuration} used to define the {@link IntentRecognitionProvider} to build
     * @return the {@link IntentRecognitionProvider} matching the provided {@code configuration}
     * @see #getRecognitionMonitor(XatkitCore, Configuration)
     */
    public static IntentRecognitionProvider getIntentRecognitionProvider(XatkitCore xatkitCore, Configuration
            configuration) {
        checkNotNull(xatkitCore, "Cannot get an %s from the provided %s %s", IntentRecognitionProvider.class
                .getSimpleName(), XatkitCore.class.getSimpleName(), xatkitCore);
        checkNotNull(configuration, "Cannot get an %s the provided %s %s", IntentRecognitionProvider.class
                .getSimpleName(), Configuration.class.getSimpleName(), configuration);
        RecognitionMonitor recognitionMonitor = xatkitCore.getRecognitionMonitor();
        if (configuration.containsKey(DialogFlowApi.PROJECT_ID_KEY)) {
            /*
             * The provided configuration contains DialogFlow-related information.
             */
            return new DialogFlowApi(xatkitCore, configuration, recognitionMonitor);
        } else {
            /*
             * The provided configuration does not contain any IntentRecognitionProvider information, returning a
             * RegExIntentRecognitionProvider.
             */
            return new RegExIntentRecognitionProvider(configuration, recognitionMonitor);
        }
    }


}

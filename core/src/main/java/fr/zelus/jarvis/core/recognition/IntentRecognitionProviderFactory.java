package fr.zelus.jarvis.core.recognition;

import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.recognition.dialogflow.DialogFlowApi;
import org.apache.commons.configuration2.Configuration;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * Builds {@link IntentRecognitionProvider}s from the provided {@code configuration}.
 * <p>
 * This factory inspects the provided {@code configuration} and finds the concrete {@link IntentRecognitionProvider}
 * to construct. If the provided {@code configuration} does not define any {@link IntentRecognitionProvider}, a
 * {@link DefaultIntentRecognitionProvider} is returned, providing minimal support to
 * {@link fr.zelus.jarvis.core.session.JarvisSession} management.
 * <p>
 * <b>Note:</b> {@link DefaultIntentRecognitionProvider} does not handle
 * {@link fr.zelus.jarvis.intent.IntentDefinition} and {@link fr.zelus.jarvis.intent.RecognizedIntent} computation.
 * If the bot application requires such features a valid {@link IntentRecognitionProvider} must be specified in the
 * provided configuration.
 *
 * @see IntentRecognitionProvider
 * @see DefaultIntentRecognitionProvider
 */
public class IntentRecognitionProviderFactory {

    /**
     * Returns the {@link IntentRecognitionProvider} matching the provided {@code configuration}.
     * <p>
     * If the provided {@code configuration} does not define any {@link IntentRecognitionProvider}, a
     * {@link DefaultIntentRecognitionProvider} is returned, providing minimal support to
     * {@link fr.zelus.jarvis.core.session.JarvisSession} management.
     *
     * @param jarvisCore    the {@link JarvisCore} instance to build the {@link IntentRecognitionProvider} from
     * @param configuration the {@link Configuration} used to define the {@link IntentRecognitionProvider} to build
     * @return the {@link IntentRecognitionProvider} matching the provided {@code configuration}
     */
    public static IntentRecognitionProvider getIntentRecognitionProvider(JarvisCore jarvisCore, Configuration
            configuration) {
        checkNotNull(jarvisCore, "Cannot get an %s from the provided %s %s", IntentRecognitionProvider.class
                .getSimpleName(), JarvisCore.class.getSimpleName(), jarvisCore);
        checkNotNull(configuration, "Cannot get an %s the provided %s %s", IntentRecognitionProvider.class
                        .getSimpleName(), Configuration.class.getSimpleName(), configuration);
        if (configuration.containsKey(DialogFlowApi.PROJECT_ID_KEY)) {
            /*
             * The provided configuration contains DialogFlow-related information.
             */
            return new DialogFlowApi(jarvisCore, configuration);
        } else {
            /*
             * The provided configuration does not contain any IntentRecognitionProvider information, returning a
             * DefaultIntentRecognitionProvider.
             */
            return new DefaultIntentRecognitionProvider(configuration);
        }
    }
}

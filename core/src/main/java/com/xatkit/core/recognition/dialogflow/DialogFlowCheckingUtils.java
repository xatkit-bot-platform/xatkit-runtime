package com.xatkit.core.recognition.dialogflow;

import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.MappingEntityDefinition;
import fr.inria.atlanmod.commons.log.Log;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

/**
 * An utility class that provides checking methods for DialogFlow models.
 */
public class DialogFlowCheckingUtils {

    /**
     * Disables the default constructor, this class only provides static methods and should not be constructed.
     */
    private DialogFlowCheckingUtils() {
    }

    /**
     * Checks the out {@link Context}s of the provided {@code intentDefinition}.
     * <p>
     * This method searches for consistency issues in the provided {@code intentDefinition} out contexts, e.g. text
     * fragment in {@link ContextParameter}s that does not correspond to their corresponding entity value, or text
     * fragments that are not contained in the intentDefinition training sentences.
     * <p>
     * Non-critical errors are logged as warning. Critical errors (i.e. errors that will generate a non-working bot)
     * throw an exception.
     *
     * @param intentDefinition the {@link IntentDefinition} to check the out {@link Context} of
     * @throws IntentRecognitionProviderException if there is no training sentence containing a provided {@code
     *                                            intentDefinition}'s parameter fragment
     */
    public static void checkOutContexts(IntentDefinition intentDefinition) throws IntentRecognitionProviderException {
        for (Context outContext : intentDefinition.getOutContexts()) {
            for (ContextParameter contextParameter : outContext.getParameters()) {
                checkContextParameterFragmentIsValidMappingEntityValue(contextParameter);
                checkContextParameterFragmentIsInTrainingSentence(contextParameter);
            }
        }
    }

    /**
     * Checks that the provided {@code parameter}'s fragment is a valid value of its corresponding
     * {@link MappingEntityDefinition}.
     * <p>
     * Using placeholders to represent {@link MappingEntityDefinition} is allowed by the framework, but it may
     * generate consistency issues at the recognition level (e.g. the DialogFlowApi can match sentences containing
     * the placeholder value). This is not a critical issue and a warning message is logged.
     *
     * @param parameter the {@link ContextParameter} to check
     */
    private static void checkContextParameterFragmentIsValidMappingEntityValue(ContextParameter parameter) {
        String textFragment = parameter.getTextFragment();
        EntityDefinition referredEntity = parameter.getEntity().getReferredEntity();
        Context context = (Context) parameter.eContainer();
        EventDefinition eventDefinition = (EventDefinition) context.eContainer();
        if (referredEntity instanceof MappingEntityDefinition) {
            MappingEntityDefinition mappingEntityDefinition = (MappingEntityDefinition) referredEntity;
            List<String> mappingValues = mappingEntityDefinition.getEntryValues();
            if (!mappingValues.contains(textFragment)) {
                Log.warn("The text fragment {0} of intent {1} is not a valid value of its corresponding " +
                        "mapping {2}, the intent will still be deployed, but inconsistencies may arise during" +
                        " the recognition", textFragment, eventDefinition.getName(), mappingEntityDefinition
                        .getName());
            }
        }
    }

    /**
     * Checks that the provided {@code parameter}'s fragment is contained in a training sentence of its containing
     * {@link IntentDefinition}.
     * <p>
     * This method throws a {@link IntentRecognitionProviderException} if there is no training sentence containing
     * the provided {@code parameter}'s fragment. Such consistency issue prevents the bot deployment, because it
     * would generate context parameters that are never matched by the recognition engine.
     *
     * @param parameter the {@link ContextParameter} to check
     * @throws IntentRecognitionProviderException if there is no training sentence containing the provided {@code
     *                                            paramter}'s fragment
     */
    private static void checkContextParameterFragmentIsInTrainingSentence(ContextParameter parameter) throws IntentRecognitionProviderException {
        String textFragment = parameter.getTextFragment();
        Context context = (Context) parameter.eContainer();
        if (context.eContainer() instanceof IntentDefinition) {
            IntentDefinition intentDefinition = (IntentDefinition) context.eContainer();
            Optional<String> fragmentTrainingSentence = intentDefinition.getTrainingSentences().stream().filter
                    (trainingSentence -> trainingSentence.contains(textFragment)).findAny();
            if (!fragmentTrainingSentence.isPresent()) {
                throw new IntentRecognitionProviderException(MessageFormat.format("The text fragment {0} is not " +
                        "contained in a training sentence of intent {1}, cannot deploy the bot, the context parameter" +
                        " {2} will never be matched", textFragment, intentDefinition.getName(), parameter.getName()));
            }
        }
    }
}

package edu.uoc.som.jarvis.core.recognition.dialogflow;

import edu.uoc.som.jarvis.intent.ContextParameter;
import edu.uoc.som.jarvis.intent.EntityDefinition;
import edu.uoc.som.jarvis.intent.IntentDefinition;
import edu.uoc.som.jarvis.intent.MappingEntityDefinition;
import fr.inria.atlanmod.commons.log.Log;

import java.util.List;

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
     * Checks the out {@link edu.uoc.som.jarvis.intent.Context}s of the provided {@code intentDefinition}.
     * <p>
     * This method searches for consistency issues in the provided {@code intentDefinition} out contexts, e.g. text
     * fragment in {@link ContextParameter}s that does not correspond to their corresponding entity value, or text
     * fragments that are not contained in the intentDefinition training sentences.
     * <p>
     * Non-critical errors are logged as warning. Critical errors (i.e. errors that will generate a non-working bot)
     * throw an exception.
     *
     * @param intentDefinition the {@link IntentDefinition} to check the out
     *                         {@link edu.uoc.som.jarvis.intent.Context} of
     */
    public static void checkOutContexts(IntentDefinition intentDefinition) {
        for (edu.uoc.som.jarvis.intent.Context outContext : intentDefinition.getOutContexts()) {
            for (ContextParameter contextParameter : outContext.getParameters()) {
                String textFragment = contextParameter.getTextFragment();
                EntityDefinition referredEntity = contextParameter.getEntity().getReferredEntity();
                if (referredEntity instanceof MappingEntityDefinition) {
                    MappingEntityDefinition mappingEntityDefinition = (MappingEntityDefinition) referredEntity;
                    List<String> mappingValues = mappingEntityDefinition.getEntryValues();
                    if (!mappingValues.contains(textFragment)) {
                        Log.warn("The text fragment {0} of intent {1} is not a valid value of its corresponding " +
                                "mapping {2}, the intent will still be deployed, but inconsistencies may arise during" +
                                " the recognition", textFragment, intentDefinition.getName(), mappingEntityDefinition
                                .getName());
                    }
                }
            }
        }
    }
}

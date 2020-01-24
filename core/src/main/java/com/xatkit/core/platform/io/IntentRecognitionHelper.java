package com.xatkit.core.platform.io;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.recognition.IntentRecognitionProvider;
import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.core.session.RuntimeContexts;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;

/**
 * A helper class that provides method to extract intents from input text.
 * <p>
 * This class can be used by {@link RuntimeEventProvider}s that receive user messages and translate them to
 * intents. It automatically manages the recognition as well as the session attributes that need to be updated
 * once an intent has been recognized.
 */
public class IntentRecognitionHelper {

    /**
     * Returns the {@link RecognizedIntent} from the provided user {@code input} and {@code session}.
     * <p>
     * This uses the provided {@code xatkitCore} to wrap the access to the underlying
     * {@link IntentRecognitionProvider}, and avoid uncontrolled accesses to the
     * {@link IntentRecognitionProvider} from {@link RuntimeEventProvider}s (such as intent creation,
     * removal, and context manipulation).
     * <p>
     * <b>Note:</b> this method decrements the lifespan counts of the variables in the current context (context
     * lifespan are used to represent the number of user interaction to handled before deleting the variable).
     * <b>Client classes must call this method before setting any context variable</b> otherwise there lifespan
     * counts may be inconsistent from their expected values (e.g. context variables with a lifespan count of {@code
     * 1} will be immediately removed by the {@link RuntimeContexts#decrementLifespanCounts()} call).
     *
     * @param input   the textual user input to extract the {@link RecognizedIntent} from
     * @param session the {@link XatkitSession} wrapping the underlying {@link IntentRecognitionProvider}'s session
     * @return the {@link RecognizedIntent} computed by the {@link IntentRecognitionProvider}
     * @throws NullPointerException               if the provided {@code text} or {@code session} is {@code null}
     * @throws IllegalArgumentException           if the provided {@code text} is empty
     * @throws IntentRecognitionProviderException if the {@link IntentRecognitionProvider} is shutdown or if an
     *                                            exception is thrown by the underlying intent recognition engine
     */
    public static RecognizedIntent getRecognizedIntent(String input, XatkitSession session, XatkitCore xatkitCore) {
        session.getRuntimeContexts().decrementLifespanCounts();
        RecognizedIntent recognizedIntent = xatkitCore.getIntentRecognitionProvider().getIntent(input, session);
        Log.info("Detected Intent {0} (confidence {1}) from query text \"{2}\" ({3})",
                recognizedIntent.getDefinition().getName(), recognizedIntent.getRecognitionConfidence(),
                recognizedIntent.getMatchedInput(), session.toString());
        return recognizedIntent;
    }
}

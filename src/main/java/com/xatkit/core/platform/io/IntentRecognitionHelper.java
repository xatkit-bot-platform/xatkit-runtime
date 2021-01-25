package com.xatkit.core.platform.io;

import com.xatkit.core.XatkitBot;
import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;

import java.util.stream.Collectors;

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
     * This uses the provided {@code xatkitBot} to wrap the access to the underlying
     * {@link com.xatkit.core.recognition.IntentRecognitionProvider}, and avoid uncontrolled accesses to the
     * {@link com.xatkit.core.recognition.IntentRecognitionProvider} from {@link RuntimeEventProvider}s (such as
     * intent creation, removal, and context manipulation).
     *
     * @param input   the textual user input to extract the {@link RecognizedIntent} from
     * @param context the {@link StateContext} wrapping the underlying
     *                {@link com.xatkit.core.recognition.IntentRecognitionProvider}'s session
     * @return the {@link RecognizedIntent} computed by the
     * {@link com.xatkit.core.recognition.IntentRecognitionProvider}
     * @throws NullPointerException               if the provided {@code text} or {@code session} is {@code null}
     * @throws IllegalArgumentException           if the provided {@code text} is empty
     * @throws IntentRecognitionProviderException if the
     *                                            {@link com.xatkit.core.recognition.IntentRecognitionProvider} is
     *                                            shutdown or if an exception is thrown by the
     *                                            underlying intent recognition engine
     */
    public static RecognizedIntent getRecognizedIntent(@NonNull String input, @NonNull StateContext context,
                                                       @NonNull XatkitBot xatkitBot) throws IntentRecognitionProviderException {
        RecognizedIntent recognizedIntent = xatkitBot.getIntentRecognitionProvider().getIntent(input, context);
        Log.info("Detected Intent {0} (confidence {1}) from query text \"{2}\"{3}",
                recognizedIntent.getDefinition().getName(), recognizedIntent.getRecognitionConfidence(),
                recognizedIntent.getMatchedInput(), printIntentParameters(recognizedIntent));
        return recognizedIntent;
    }

    /**
     * Prints a {@link String} representation of the provided {@code recognizedIntent}'s parameter values.
     *
     * @param recognizedIntent the {@link RecognizedIntent} to print the parameter values of
     * @return a {@link String} representation of the provided {@code recognizedIntent}'s parameter values
     */
    private static String printIntentParameters(RecognizedIntent recognizedIntent) {
        if (recognizedIntent.getValues().isEmpty()) {
            return "";
        } else {
            String parameters =
                    recognizedIntent.getValues().stream()
                            .map(p -> p.getContextParameter().getName() + "=\"" + p.getValue().toString() + "\"")
                            .collect(Collectors.joining(", ", "{", "}"));
            return "\nIntent Parameters: " + parameters;
        }
    }
}

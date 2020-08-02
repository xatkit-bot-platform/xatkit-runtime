package com.xatkit.util;

import com.xatkit.execution.StateContext;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.RecognizedIntent;
import lombok.Getter;

import java.text.MessageFormat;
import java.util.function.Predicate;

import static java.util.Objects.nonNull;

public class IsIntentDefinitionPredicate implements Predicate<StateContext> {

    @Getter
    private IntentDefinition intentDefinition;

    public IsIntentDefinitionPredicate(IntentDefinition intentDefinition) {
        this.intentDefinition = intentDefinition;
    }

    @Override
    public boolean test(StateContext stateContext) {
        RecognizedIntent recognizedIntent = stateContext.getIntent();
        if(nonNull(recognizedIntent)) {
            if(nonNull(recognizedIntent.getDefinition())) {
                /*
                 * TODO check equals works fine for IntentDefinition.
                 */
                return recognizedIntent.getDefinition().equals(this.intentDefinition);
            } else {
                throw new IllegalStateException(MessageFormat.format("The current {0}'s definition is null",
                        RecognizedIntent.class.getSimpleName()));
            }
        }
        return false;
    }
}

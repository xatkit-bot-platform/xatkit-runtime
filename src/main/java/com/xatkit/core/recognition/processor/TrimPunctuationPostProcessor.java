package com.xatkit.core.recognition.processor;


import com.xatkit.execution.StateContext;
import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.EntityType;
import com.xatkit.intent.RecognizedIntent;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TrimPunctuationPostProcessor implements IntentPostProcessor {

    private static final String[] PUNCTUATION = {"?", ".", "!"};

    private final String punctuationPattern;

    public TrimPunctuationPostProcessor() {
        punctuationPattern = Arrays.stream(PUNCTUATION).map(Pattern::quote).collect(Collectors.joining("|"));
    }


    @Override
    public RecognizedIntent process(RecognizedIntent recognizedIntent, StateContext context) {
        recognizedIntent.getValues().forEach(v -> {
            EntityDefinition referredEntity = v.getContextParameter().getEntity().getReferredEntity();
            if (referredEntity instanceof BaseEntityDefinition) {
                BaseEntityDefinition baseEntityDefinition = (BaseEntityDefinition) referredEntity;
                if (baseEntityDefinition.getEntityType().equals(EntityType.ANY)) {
                    if (v.getValue() instanceof String) {
                        String value = (String) v.getValue();
                        String trimmedValue = value.replaceAll(punctuationPattern, "");
                        v.setValue(trimmedValue);
                    }
                }
            }
        });
        return recognizedIntent;
    }
}

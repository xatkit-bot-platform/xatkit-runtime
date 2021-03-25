package com.xatkit.util;

import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.EntityType;
import com.xatkit.intent.IntentDefinition;
import lombok.NonNull;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;

/**
 * Utility methods to ease {@link IntentDefinition} manipulation.
 */
public final class IntentUtils {

    /**
     * Disables the default constructor, this class only provides static methods and should not be constructed.
     */
    private IntentUtils() { }

    /**
     * Returns {@code true} if the provided {@code parameter} refers to an {@link EntityType#ANY} entity.
     *
     * @param parameter the {@link ContextParameter} to check
     * @return {@code true} if the provided {@code parameter} refers to an {@link EntityType#ANY} entity
     * @throws NullPointerException if the provided {@code parameter} is {@code null}
     */
    public static boolean isAnyParameter(@NonNull ContextParameter parameter) {
        if (parameter.getEntity().getReferredEntity() instanceof BaseEntityDefinition) {
            BaseEntityDefinition entity = (BaseEntityDefinition) parameter.getEntity().getReferredEntity();
            return entity.getEntityType().equals(EntityType.ANY);
        }
        return false;
    }

    /**
     * Returns {@code true} if the provided {@code intent} contains a pure any training sentence.
     * <p>
     * A pure any training sentence is a training sentence that entirely matches one of the
     * {@link ContextParameter#getTextFragments()} of an {@link EntityType#ANY} parameter of its containing intent.
     * <p>
     * Example:
     * <pre>
     * {@code
     * intent(MyIntent)
     *     .trainingSentence("VALUE")
     *     .parameter("p").fromFragments("VALUE").entity(any());
     * }
     * </pre>
     * <p>
     * Pure any training sentences typically match any user input.
     * <p>
     * <b>Note</b>: this method returns {@code true} if the provided {@code intent} contains a mix of pure any and
     * regular training sentences.
     *
     * @param intent the {@link IntentDefinition} to check
     * @return {@code true} if the provided {@code intent} contains a pure any training sentence
     * @throws NullPointerException if the provided {@code intent} is {@code null}
     * @see #isAnyParameter(ContextParameter)
     * @see #isPureAnyTrainingSentence(IntentDefinition, String)
     */
    public static boolean hasPureAnyTrainingSentence(@NonNull IntentDefinition intent) {
        return intent.getTrainingSentences().stream().anyMatch(ts -> IntentUtils.isPureAnyTrainingSentence(intent, ts));
    }

    /**
     * Returns {@code true} if the provided {@code trainingSentence} is a pure any training sentence of {@code intent}.
     * <p>
     * A pure any training sentence is a training sentence that entirely matches one of the
     * {@link ContextParameter#getTextFragments()} of an {@link EntityType#ANY} parameter of its containing intent.
     * <p>
     * Example:
     * <pre>
     * {@code
     * intent(MyIntent)
     *     .trainingSentence("VALUE")
     *     .parameter("p").fromFragments("VALUE").entity(any());
     * }
     * </pre>
     * <p>
     * Pure any training sentences typically match any user input.
     *
     * @param intent           the {@link IntentDefinition} to containing the training sentence to check
     * @param trainingSentence the training sentence to check
     * @return {@code true} if the provided {@code trainingSentence} is a pure any training sentence of {@code intent}
     * @throws NullPointerException     if the provided {@code intent} or {@code trainingSentence} is {@code null}
     * @throws IllegalArgumentException if the provided {@code intent} does not contain the {@code trainingSentence}
     * @see #isAnyParameter(ContextParameter)
     */
    public static boolean isPureAnyTrainingSentence(@NonNull IntentDefinition intent,
                                                    @NonNull String trainingSentence) {
        checkArgument(intent.getTrainingSentences().contains(trainingSentence), "Intent %s does not contain training "
                + "sentence\"%s\"", intent.getName(), trainingSentence);
        return intent.getParameters().stream()
                .anyMatch(p -> isAnyParameter(p) && trainingSentence.equals(p.getTextFragments().get(0)));
    }
}

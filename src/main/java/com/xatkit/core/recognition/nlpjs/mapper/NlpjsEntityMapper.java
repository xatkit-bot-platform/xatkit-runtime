package com.xatkit.core.recognition.nlpjs.mapper;

import com.xatkit.core.recognition.nlpjs.model.BetweenCondition;
import com.xatkit.core.recognition.nlpjs.model.Entity;
import com.xatkit.core.recognition.nlpjs.model.EntityType;
import com.xatkit.core.recognition.nlpjs.model.EntityValue;
import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.CustomEntityDefinition;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.MappingEntityDefinition;
import com.xatkit.intent.MappingEntityDefinitionEntry;
import lombok.NonNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Maps {@link EntityDefinition}s to NLP.js {@link Entity} instances.
 * <p>
 * This class is used to translate generic {@link EntityDefinition}s to platform-specific construct representing NLP
 * .js entities.
 * <p>
 * This class provides to mapping methods:
 * <ul>
 *     <li>{@link #mapEntityDefinition(EntityDefinition)}: maps any kind of {@link EntityDefinition}. This method
 *     is typically used to map {@link CustomEntityDefinition}s, and ignores {@link BaseEntityDefinition}s (that
 *     are already part of NLP.js engine)</li>
 *     <li>{@link #mapAnyEntityDefinition(IntentDefinition)}: creates the {@code any} entities associated to the
 *     provided {@link IntentDefinition}. NLP.js requires an {@code any} entity for each intent using {@code any} in
 *     its parameters (instead of a generic any entity defined for all the intents). This means that we need the
 *     {@link IntentDefinition} to create any entities.
 *      </li>
 * </ul>
 * <p>
 * This class does not support {@link CompositeEntityDefinition}s.
 * <p>
 * <b>Note</b>: this class does not create references to existing entities, see {@link NlpjsEntityReferenceMapper}
 * for more information.
 *
 * @see NlpjsEntityReferenceMapper
 */
public class NlpjsEntityMapper {

    /**
     * The {@link NlpjsEntityReferenceMapper} used to store {@code any} entities created with
     * {@link #mapAnyEntityDefinition(IntentDefinition)}.
     * <p>
     * These entities need a particular reference mapping since they are bound to a specific {@link ContextParameter}
     * , and cannot be defined at a generic level like other built-in entities.
     */
    private NlpjsEntityReferenceMapper referenceMapper;

    /**
     * Creates a {@link NlpjsEntityMapper} with the provided {@code referenceMapper}.
     * <p>
     * <b>Note</b>: the provided {@link NlpjsEntityReferenceMapper} is used to store {@code any} entities created
     * with {@link #mapAnyEntityDefinition(IntentDefinition)}. This means that this class updates the provided {@code
     * referenceMapper}. See {@link NlpjsEntityReferenceMapper#getMappingFor(ContextParameter)} to retrieve the
     * mapping for a given {@link ContextParameter}.
     *
     * @param referenceMapper the {@link NlpjsEntityReferenceMapper} used to store {@code any} entities created with
     *                        {@link #mapAnyEntityDefinition(IntentDefinition)}
     * @throws NullPointerException if the provided {@code referenceMapper} is {@code null}
     */
    public NlpjsEntityMapper(@NonNull NlpjsEntityReferenceMapper referenceMapper) {
        this.referenceMapper = referenceMapper;
    }

    /**
     * Maps the provided {@code entityDefinition} to a NLP.js {@link Entity}.
     * <p>
     * This method does not support {@link BaseEntityDefinition}s, because base entities are already deployed in NLP
     * .js agents (and called built-in entities).
     * <p>
     * This method does not support {@link CompositeEntityDefinition}s.
     *
     * @param entityDefinition the {@link EntityDefinition} to map
     * @return the created {@link Entity}
     * @throws NullPointerException     if the provided {@code entityDefinition} is {@code null}
     * @throws IllegalArgumentException if the provided {@code entityDefinition} is a {@link BaseEntityDefinition} or
     *                                  a {@link CompositeEntityDefinition}
     */
    public Entity mapEntityDefinition(@NonNull EntityDefinition entityDefinition) {
        if (entityDefinition instanceof BaseEntityDefinition) {
            throw new IllegalArgumentException(MessageFormat.format("Cannot map the provided {0} {1}. Base entities "
                            + "are already mapped in NLP.js", EntityDefinition.class.getSimpleName(),
                    entityDefinition.toString()));
        } else if (entityDefinition instanceof CustomEntityDefinition) {
            return mapCustomEntityDefinition((CustomEntityDefinition) entityDefinition);
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Cannot register the provided {0}, unsupported "
                            + "{1}", entityDefinition.getClass().getSimpleName(),
                    EntityDefinition.class.getSimpleName()));
        }
    }

    /**
     * Creates a NLP.js {@link Entity} from the provided {@code customEntityDefinition}.
     * <p>
     * This method does not support {@link CompositeEntityDefinition}.
     *
     * @param customEntityDefinition the {@link CustomEntityDefinition} to create an {@link Entity} from
     * @return the created {@link Entity}
     * @throws NullPointerException     if the provided {@code customEntityDefinition} is {@code null}
     * @throws IllegalArgumentException if the provided {@code customEntityDefinition} is a
     *                                  {@link CompositeEntityDefinition}
     * @see #createEntityValuesFromMapping(MappingEntityDefinition)
     */
    private Entity mapCustomEntityDefinition(@NonNull CustomEntityDefinition customEntityDefinition) {
        String entityName = customEntityDefinition.getName();
        Entity.EntityBuilder builder = Entity.builder();
        builder.entityName(entityName);
        if (customEntityDefinition instanceof MappingEntityDefinition) {
            MappingEntityDefinition mappingEntityDefinition = (MappingEntityDefinition) customEntityDefinition;
            List<EntityValue> entityValues = createEntityValuesFromMapping(mappingEntityDefinition);
            builder.type(EntityType.ENUM).references(entityValues);

        } else if (customEntityDefinition instanceof CompositeEntityDefinition) {
            throw new IllegalArgumentException(MessageFormat.format("Cannot register the entity {0}. "
                    + "Composite entities are not supported by NLP.js", customEntityDefinition));
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Cannot register the provided {0}, unsupported {1}",
                    customEntityDefinition.getClass().getSimpleName(), EntityDefinition.class.getSimpleName()));
        }
        return builder.build();
    }

    /**
     * Creates the NLP.js {@link EntityValue}s representing the provided {@code mappingEntityDefinition}.
     * <p>
     * Each {@link EntityValue} in the returned list corresponds to a {@link MappingEntityDefinition}'s entry.
     *
     * @param mappingEntityDefinition the {@link MappingEntityDefinition} to create the {@link EntityValue}s from
     * @return the created {@link EntityValue}s
     * @throws NullPointerException if the provided {@code mappingEntityDefinition} is {@code null}
     */
    private List<EntityValue> createEntityValuesFromMapping(@NonNull MappingEntityDefinition mappingEntityDefinition) {
        List<EntityValue> entityValues = new ArrayList<>();
        for (MappingEntityDefinitionEntry entry : mappingEntityDefinition.getEntries()) {
            EntityValue.EntityValueBuilder builder = EntityValue.builder()
                    .value(entry.getReferenceValue())
                    .synonyms(entry.getSynonyms())
                    .synonym(entry.getReferenceValue());
            entityValues.add(builder.build());
        }
        return entityValues;
    }

    /**
     * Maps the {@link ContextParameter}s of the provided {@code intentDefinition} to {@code any} entities.
     * <p>
     * This method maps all the {@link ContextParameter}s that refer to an {@link com.xatkit.intent.EntityType#ANY}
     * entity to NLP.js {@link Entity} instances. NLP.js requires a dedicated {@link Entity} for each
     * {@link ContextParameter} (instead of a generic any entity defined for all the intents). These {@link Entity}
     * instances need contextual information such as the {@link IntentDefinition} and {@link ContextParameter} names.
     * <p>
     * Fragments that are part of a training sentence (e.g. {@code "I want VALUE"}) are mapped to trim
     * {@link Entity} instances, where the pre/post words are configured from the training sentence.
     * <p>
     * This method cannot map fragments corresponding to entire training sentences (e.g. {@code "VALUE"}), and
     * ignores them.
     * <p>
     * This method updates the {@link NlpjsEntityReferenceMapper} associated to this class to allow other mappers
     * to get references to the produced {@link Entity} instances.
     * <p>
     * <b>Note</b>: this method only consider the first element in {@link ContextParameter#getTextFragments()}.
     *
     * @param intentDefinition the {@link IntentDefinition} containing the {@link ContextParameter}s to map
     * @return the created {@link Entity} instances
     * @throws NullPointerException  if the provided {@code intentDefinition} is {@code null}
     */
    public Collection<Entity> mapAnyEntityDefinition(@NonNull IntentDefinition intentDefinition) {
        Map<String, Entity> createdEntities = new HashMap<>();
        for (ContextParameter parameter : intentDefinition.getParameters()) {
            EntityDefinition parameterEntity = parameter.getEntity().getReferredEntity();
            if (parameterEntity instanceof BaseEntityDefinition
                    && ((BaseEntityDefinition) parameterEntity).getEntityType()
                    .equals(com.xatkit.intent.EntityType.ANY)) {
                for (String trainingSentence : intentDefinition.getTrainingSentences()) {
                    String entityName = intentDefinition.getName() + parameter.getName() + "Any";
                    /*
                     * TODO handle multiple text fragments
                     */
                    if (trainingSentence.contains(parameter.getTextFragments().get(0))) {
                        Optional<String> beforeLast = getBeforeLast(trainingSentence, parameter);
                        Optional<String> afterLast = getAfterLast(trainingSentence, parameter);
                        if (beforeLast.isPresent() || afterLast.isPresent()) {
                            /*
                             * There are words before and/or after the parameter text fragment. We will use them to
                             * configure a TRIM entity.
                             */
                            Entity entity = createdEntities.computeIfAbsent(entityName, k ->
                                    Entity.builder()
                                            .entityName(entityName)
                                            .type(EntityType.TRIM)
                                            .build()

                            );
                            /*
                             * These complex (and redundant) conditions are somehow necessary, because we don't call
                             * the same methods if there is 1 or 2 values set.
                             */
                            if (beforeLast.isPresent() && afterLast.isPresent()) {
                                BetweenCondition betweenCondition = entity.getBetween();
                                if (isNull(betweenCondition)) {
                                    betweenCondition = new BetweenCondition();
                                    entity.setBetween(betweenCondition);
                                }
                                betweenCondition.getLeft().add(afterLast.get());
                                betweenCondition.getRight().add(beforeLast.get());
                            } else if (beforeLast.isPresent()) {
                                entity.getBeforeLast().add(beforeLast.get());
                            } else {
                                /*
                                 * afterLast.isPresent()
                                 */
                                entity.getAfterLast().add(afterLast.get());
                            }
                            /*
                             * Register the created entity in the NlpjsEntityReferenceMapper. We need to access it to
                             * create references to this entity when creating intents.
                             * Since the created entities are bound to a specific ContextParameter we can't use the
                             * generic
                             * entity mapping algorithm already implemented in EntityMapper and we need to go for this
                             * custom method.
                             */
                            this.referenceMapper.addAnyEntityMapping(parameter, entity.getEntityName());
                        } else {
                            /*
                             * The any entity isn't between other words, this means that we are dealing with a pure
                             * any training sentence. The NlpjsIntentRecognitionProvider has a native way to
                             * handle these training sentences, and we don't need to register them in the NLP.js
                             * server.
                             */
                            continue;
                        }
                    }
                }
            } else {
                /*
                 * The parameter is not an any entity, we can ignore it.
                 */
                continue;
            }
        }
        return createdEntities.values();
    }

    /**
     * Retrieves the word following the {@code parameter}'s text fragment in {@code trainingSentence}.
     * <p>
     * This method is called by {@link #mapAnyEntityDefinition(IntentDefinition)} to configure NLP.js trim entities.
     *
     * @param trainingSentence the training sentence to inspect
     * @param parameter        the {@link ContextParameter} containing the text fragment to look for
     * @return the word following the {@code parameter}'s text fragment in {@code trainingSentence}
     * @throws NullPointerException if {@code trainingSentence} or {@code parameter} is {@code null}
     */
    private Optional<String> getBeforeLast(@NonNull String trainingSentence, @NonNull ContextParameter parameter) {
        // TODO handle multiple text fragments
        String textFragment = parameter.getTextFragments().get(0);
        int textFragmentStartIndex = trainingSentence.indexOf(textFragment);
        int textFragmentEndIndex = textFragmentStartIndex + textFragment.length();
        if (textFragmentEndIndex != trainingSentence.length()) {
            String[] postFragments = trainingSentence.substring(textFragmentEndIndex).split(" ");
            String beforeLast = Arrays.stream(postFragments).filter(v -> !v.isEmpty()).findFirst().orElse(null);
            if (nonNull(beforeLast)) {
                if (beforeLast.equals("?")) {
                    /*
                     * NLP.js requires to escape "?" in regex.
                     */
                    beforeLast = "\\?";
                }
                return Optional.of(beforeLast);
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves the word preceding the {@code parameter}'s text fragment in {@code trainingSentence}.
     * <p>
     * This method is called by {@link #mapAnyEntityDefinition(IntentDefinition)} to configure NLP.js trim entities.
     *
     * @param trainingSentence the training sentence to inspect
     * @param parameter        the {@link ContextParameter} containing the text fragment to look for
     * @return the word preceding the {@code parameter}'s text fragment in {@code trainingSentence}
     * @throws NullPointerException if the {@code trainingSentence} or {@code parameter} is {@code null}
     */
    private Optional<String> getAfterLast(@NonNull String trainingSentence, @NonNull ContextParameter parameter) {
        // TODO handle multiple text fragments
        String textFragment = parameter.getTextFragments().get(0);
        int textFragmentStartIndex = trainingSentence.indexOf(textFragment);
        if (textFragmentStartIndex != 0) {
            String[] preFragments = trainingSentence.substring(0, textFragmentStartIndex).split(" ");
            if (preFragments.length > 0) {
                return Optional.of(preFragments[preFragments.length - 1]);
            }
        }
        return Optional.empty();
    }
}

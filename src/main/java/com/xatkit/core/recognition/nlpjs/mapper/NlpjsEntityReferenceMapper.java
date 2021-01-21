package com.xatkit.core.recognition.nlpjs.mapper;

import com.xatkit.core.recognition.EntityMapper;
import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.CustomEntityDefinition;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.EntityType;
import com.xatkit.intent.IntentDefinition;
import lombok.NonNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.xatkit.intent.EntityType.ANY;
import static com.xatkit.intent.EntityType.DATE;
import static com.xatkit.intent.EntityType.DATE_PERIOD;
import static com.xatkit.intent.EntityType.DATE_TIME;
import static com.xatkit.intent.EntityType.EMAIL;
import static com.xatkit.intent.EntityType.NUMBER;
import static com.xatkit.intent.EntityType.PERCENTAGE;
import static com.xatkit.intent.EntityType.PHONE_NUMBER;
import static com.xatkit.intent.EntityType.URL;
import static java.util.Objects.nonNull;

/**
 * An {@link EntityMapper} initialized with NLP.js' built-in entities.
 * <p>
 * This class provides a mapping of {@link EntityType}s to NLP.js built-in entities. This mapper also translates
 * {@link CustomEntityDefinition} to the NLP.js parameter representation that can be embedded in
 * {@link com.xatkit.core.recognition.nlpjs.model.IntentExample}s and
 * {@link com.xatkit.core.recognition.nlpjs.model.IntentParameter}s.
 * <p>
 * This class exposes the {@link #getMappingFor(ContextParameter)} method, which does not exist in
 * {@link EntityMapper}. This method wraps {@link #getMappingFor(EntityDefinition)}, and integrates specific
 * reference mapping for {@code any} entities (which is not covered by {@link #getMappingFor(EntityDefinition)},
 * because this mapping requires information from the parameter itself). <b>Client code should use
 * {@link #getMappingFor(ContextParameter)} when possible</b>, especially when mapping {@code any} entities.
 * <p>
 * NLP.js does not have built-in support for all the Xatkit {@link BaseEntityDefinition}s. Unsupported entities are
 * mapped to {@code "none"}. Client code can use this information to degrade the entities of an
 * {@link IntentDefinition} (e.g. to {@code any}). Note that we use {@code none} as a fallback instead of {@code any}
 * because any entity references depend on the {@link IntentDefinition} embedding them, and this information is not
 * available when constructing an instance of this class.
 */
public class NlpjsEntityReferenceMapper extends EntityMapper {

    private Map<String, List<String>> reversedEntities;

    /**
     * The stores the {@code any} entity references associated to {@link ContextParameter}.
     * <p>
     * This attribute is updated when calling {@link #addAnyEntityMapping(ContextParameter, String)}. Parameters
     * referring to {@code any} entities are returned by {@link #getMappingFor(ContextParameter)}.
     *
     * @see #addAnyEntityMapping(ContextParameter, String)
     * @see #getMappingFor(ContextParameter)
     */
    private Map<ContextParameter, String> anyEntities = new HashMap<>();

    /**
     * Constructs a {@link NlpjsEntityReferenceMapper} initialized with NLP.js built-in entities.
     * <p>
     * NLP.js does not have built-in support for all the Xatkit {@link BaseEntityDefinition}s. Unsupported entities are
     * mapped to {@code "none"}. Client code can use this information to degrade the entities of an
     * {@link IntentDefinition} (e.g. to {@code any}). Note that we use {@code none} as a fallback instead of {@code
     * any} because any entity references depend on the {@link IntentDefinition} embedding them, and this information
     * is not available when constructing an instance of this class.
     *
     * @see #getMappingFor(ContextParameter)
     */
    public NlpjsEntityReferenceMapper() {
        super();
        this.registerEntities();
        /*
         * We cannot use "any" as a fallback because any entities require a specific processing involving the intent
         * that contains them. Returning "any" would create mapping issue because "any" is not a valid reference in
         * NLP.js.
         * The NlpjsIntentRecognitionProvider takes care of degrading Xatkit BaseEntityDefinitions to any if they
         * are not supported by NLP.js.
         */
        this.setFallbackEntityMapping("none");
        this.reversedEntities = this.reverseEntityTypes();
    }

    /**
     * Registers NLP.js built-in entities.
     */
    private void registerEntities() {
        this.addEntityMapping(ANY, "any");
        this.addEntityMapping(EMAIL, "email");
        this.addEntityMapping(PHONE_NUMBER, "phonenumber");
        this.addEntityMapping(URL, "url");
        this.addEntityMapping(NUMBER, "number");
        this.addEntityMapping(PERCENTAGE, "percentage");
        this.addEntityMapping(DATE, "date");
        this.addEntityMapping(DATE_PERIOD, "date");
        this.addEntityMapping(DATE_TIME, "date");
    }

    /**
     * Registers {@code concreteEntity} as a mapping for {@code parameter}.
     * <p>
     * Any entities require a particular processing with NLP.js: there is no generic {@code any} reference that can
     * be used in intent examples. Instead each reference is computed from the training sentence that contains it as
     * well as the {@link IntentDefinition} containing the training sentence. This means that there are multiple any
     * entity references in a NLP.js bot. These references are stored with this method, and can be retrieved with
     * {@link #getMappingFor(ContextParameter)}.
     *
     * @param parameter      the {@link ContextParameter} to map to an any entity
     * @param concreteEntity the any entity reference to map to the parameter
     * @throws NullPointerException if the provided {@code parameter} or {@code concreteEntity} is {@code null}
     */
    public void addAnyEntityMapping(@NonNull ContextParameter parameter, @NonNull String concreteEntity) {
        anyEntities.put(parameter, concreteEntity);
    }

    /**
     * Returns the mapping for the provided {@code parameter}.
     * <p>
     * This method wraps {@link EntityMapper#getMappingFor(EntityDefinition)} and extends it with
     * {@link ContextParameter} support. Client code should use this method instead of
     * {@link EntityMapper#getMappingFor(EntityDefinition)}, especially if the NLP.js bot contains any entities.
     * <p>
     * If the provided {@code parameter} does not refer to an {@code any} entity this method is equivalent to
     * {@code getMappingFor(parameter.getEntity().getReferredEntity()}.
     *
     * @param parameter the {@link ContextParameter} to retrieve the mapping of
     * @return the mapping for the provided {@code parameter}
     */
    public Optional<String> getMappingFor(ContextParameter parameter) {
        if (parameter.getEntity().getReferredEntity() instanceof BaseEntityDefinition) {
            BaseEntityDefinition baseEntityDefinition =
                    (BaseEntityDefinition) parameter.getEntity().getReferredEntity();
            if (baseEntityDefinition.getEntityType().equals(ANY)) {
                return Optional.ofNullable(this.anyEntities.get(parameter));
            }
        }
        String mapping = super.getMappingFor(parameter.getEntity().getReferredEntity());
        if (nonNull(mapping)) {
            IntentDefinition intentDefinition = (IntentDefinition) parameter.eContainer();
            if (this.getEntityCount(parameter.getEntity().getReferredEntity(), intentDefinition) > 1) {
                /*
                 * Note: any entities cannot have an index.
                 */
                mapping += "_";
                mapping += this.getEntityTypeIndex(parameter.getTextFragments().get(0),
                        parameter.getEntity().getReferredEntity(), intentDefinition);
            }
        }
        return Optional.ofNullable(mapping);
    }


    /**
     * This method is disabled for {@link NlpjsEntityReferenceMapper}.
     * <p>
     * NLP.js entities are registered independently from the intents. This two-step process allows to reference
     * entities with their names, and do not require to store any mapping information in the
     * {@link NlpjsEntityReferenceMapper}.
     * <p>
     * See {@link #getMappingFor(ContextParameter)} to retrieve a mapping.
     *
     * @param entityDefinition the {@link CustomEntityDefinition} to map
     * @param concreteEntity   the mapped value associated to the provided {@code entityDefinition}
     * @throws UnsupportedOperationException
     * @see #getMappingFor(ContextParameter)
     */
    @Override
    public void addCustomEntityMapping(CustomEntityDefinition entityDefinition, String concreteEntity) {
        throw new UnsupportedOperationException(MessageFormat.format("{0} does not allow to register custom entity "
                        + "mappings, use getMappingFor(EntityDefinition) to get NLP.js-compatible mapping of {1}",
                this.getClass().getSimpleName(), CustomEntityDefinition.class.getSimpleName()));
    }

    /**
     * Maps the provided {@code customEntityDefinition} to its NLP.js implementation.
     *
     * @param customEntityDefinition the {@link CustomEntityDefinition} to retrieve the concrete entity
     *                               {@link String} from
     * @return a {@link String} identifying the NLP.js entity corresponding to the provided {@code
     * customEntityDefinition}
     */
    @Override
    public String getMappingForCustomEntity(CustomEntityDefinition customEntityDefinition) {
        return customEntityDefinition.getName();
    }

    private Map<String, List<String>> reverseEntityTypes() {
        Map<String, List<String>> reversedEntities = new HashMap<>();
        for (Map.Entry<String, String> entityEntry : entities.entrySet()) {
            if (reversedEntities.containsKey(entityEntry.getValue())) {
                List<String> entityTypes = reversedEntities.get(entityEntry.getValue());
                entityTypes.add(entityEntry.getKey());
            } else {
                List<String> entityTypes = new ArrayList<>();
                entityTypes.add(entityEntry.getKey());
                reversedEntities.put(entityEntry.getValue(), entityTypes);
            }
        }
        return reversedEntities;
    }

    public List<String> getReversedEntity(@NonNull String nlpjsEntityType) {
        return reversedEntities.get(nlpjsEntityType);
    }

    /**
     * Returns the number of {@code entityType} in {@code intentDefinition}'s parameters.
     *
     * @param entityType       the {@link EntityDefinition} to search
     * @param intentDefinition the {@link IntentDefinition} to inspect
     * @return the number of {@code entityType} in {@code intentDefinition}'s parameters
     */
    private int getEntityCount(EntityDefinition entityType, IntentDefinition intentDefinition) {
        int count = 0;
        for (ContextParameter parameter : intentDefinition.getParameters()) {
            if (entityType.getName().equals(parameter.getEntity().getReferredEntity().getName()))
                count++;
        }
        return count;
    }

    /**
     * Returns the index of the provided entity in the {@code intentDefinition}'s parameters.
     * <p>
     * This method retrieves the index of the {@link ContextParameter} that matches both the provided {@code
     * textFragment} and {@code entityType}.
     *
     * @param textFragment     the text fragment corresponding to the entity to search
     * @param entityType       the {@link EntityDefinition} to search
     * @param intentDefinition the {@link IntentDefinition} to inspect
     * @return the index of the provided entity in the {@code intentDefinition}'s parameters
     */
    private int getEntityTypeIndex(String textFragment, EntityDefinition entityType,
                                   IntentDefinition intentDefinition) {
        int suffix = 0;
        for (ContextParameter parameter : intentDefinition.getParameters()) {
            if (entityType.getName().equals(parameter.getEntity().getReferredEntity().getName())) {
                suffix++;
            }
            if (textFragment.equals(parameter.getTextFragments().get(0))) {
                break;
            }
        }
        return suffix;
    }

}

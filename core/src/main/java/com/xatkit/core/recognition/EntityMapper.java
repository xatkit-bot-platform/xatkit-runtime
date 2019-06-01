package edu.uoc.som.jarvis.core.recognition;

import edu.uoc.som.jarvis.core.JarvisException;
import edu.uoc.som.jarvis.intent.BaseEntityDefinition;
import edu.uoc.som.jarvis.intent.CustomEntityDefinition;
import edu.uoc.som.jarvis.intent.EntityDefinition;
import edu.uoc.som.jarvis.intent.EntityType;
import fr.inria.atlanmod.commons.log.Log;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A mapper that bind concrete entity names to their abstract definitions.
 * <p>
 * This class is configured through the {@link #addEntityMapping(EntityType, String)} method, that allows to specify a
 * mapping from an abstract entity to a concrete entity name. This mapping is used to retrieve
 * {@link IntentRecognitionProvider} specific entities, and deploy jarvis generically on different platforms.
 * <p>
 * This class also defines the {@link #setFallbackEntityMapping(String)}, that allows to handle abstract entities
 * that do not have a concrete mapping in the {@link IntentRecognitionProvider} platform.
 * <p>
 * The mapping for a given abstract entity can be retrieved by calling the {@link #getMappingFor(EntityType)} with the
 * {@code abstractEntity} type as parameter.
 * <p>
 * <b>Note:</b> this class does not support {@link CustomEntityDefinition} mapping (see
 * {@link #getMappingForCustomEntity(CustomEntityDefinition)}). Since these entities are defined dynamically there is
 * no facilities to map them in a static way. Concrete {@link EntityMapper}s tailored to a given intent recognition
 * platform can override the method {@link #getMappingForCustomEntity(CustomEntityDefinition)} to implement this
 * dynamic mapping.
 *
 * @see IntentRecognitionProvider
 */
public class EntityMapper {

    /**
     * The key used to register the concrete fallback entity.
     *
     * @see #entities
     */
    protected static String FALLBACK_ENTITY_KEY = "edu.uoc.som.jarvis.core.recognition.entity.fallback";

    /**
     * The {@link Map} storing the abstract-to-concrete entity mappings.
     */
    protected Map<String, String> entities;

    /**
     * Constructs a new {@link EntityMapper}.
     * <p>
     * This constructor initializes this class with an empty mapping, that can be populated using
     * {@link #addEntityMapping(EntityType, String)} and {@link #setFallbackEntityMapping(String)}.
     *
     * @see #addEntityMapping(EntityType, String)
     * @see #setFallbackEntityMapping(String)
     */
    public EntityMapper() {
        this.entities = new HashMap<>();
    }

    /**
     * Adds a new mapping between the provided {@code abstractEntityType} and the given {@code concreteEntity}.
     * <p>
     * The stored mapping can be accessed by calling {@link #getMappingFor(EntityDefinition)} with {@code
     * abstractEntityType} as parameter.
     *
     * @param abstractEntityType the {@link EntityType} representing the abstract entity to map
     * @param concreteEntity     the {@link String} representing the concrete entity to associate to the provided {@code
     *                           abstractEntityType}
     * @throws NullPointerException if the provided {@code abstractEntityType} or {@code concreteEntity} is {@code null}
     * @see #getMappingFor(EntityDefinition)
     */
    public void addEntityMapping(EntityType abstractEntityType, String concreteEntity) {
        checkNotNull(abstractEntityType, "Cannot register a mapping for the abstract entity %s please provide a " +
                "non-null %s", abstractEntityType, EntityType.class.getSimpleName());
        checkNotNull(concreteEntity, "Cannot register %s as a mapping for the %s %s, please provide a " +
                "non-null String", concreteEntity, EntityType.class.getSimpleName(), abstractEntityType);
        String abstractEntityLiteral = abstractEntityType.getLiteral();
        addMapping(abstractEntityLiteral, concreteEntity);
    }

    /**
     * Adds a new mapping between the provided {@code entityDefinition} and the given {@code concreteEntity}.
     * <p>
     * The stored mapping can be accessed by calling {@link #getMappingFor(EntityDefinition)} with {@code
     * entityDefinition} as parameter.
     *
     * @param entityDefinition the {@link EntityDefinition} to map
     * @param concreteEntity   the mapped value associated to the provided {@code entityDefinition}
     * @throws NullPointerException if the provided {@code entityDefinition} or {@code concreteEntity} is {@code null}
     * @see #getMappingFor(EntityDefinition)
     */
    public void addEntityMapping(EntityDefinition entityDefinition, String concreteEntity) {
        checkNotNull(entityDefinition, "Cannot register a mapping for the provided %s (%s) please provide a non-null " +
                "%s", EntityDefinition.class.getSimpleName(), entityDefinition, EntityDefinition.class.getSimpleName());
        checkNotNull(concreteEntity, "Cannot register %s as a mapping for the %s %s, please provide a non-null " +
                "mapping", concreteEntity, EntityDefinition.class.getSimpleName(), entityDefinition.getName());
        if (entityDefinition instanceof BaseEntityDefinition) {
            this.addEntityMapping(((BaseEntityDefinition) entityDefinition).getEntityType(), concreteEntity);
        } else if (entityDefinition instanceof CustomEntityDefinition) {
            this.addCustomEntityMapping((CustomEntityDefinition) entityDefinition, concreteEntity);
        }
    }

    /**
     * Adds a new mapping between the provided {@code entityDefinition} and the given {@code concreteEntity}.
     * <p>
     * The stored mapping can be accessed by calling {@link #getMappingFor(EntityDefinition)} with {@code
     * entityDefinition} as parameter.
     *
     * @param entityDefinition the {@link CustomEntityDefinition} to map
     * @param concreteEntity   the mapped value associated to the provided {@code entityDefinition}
     * @throws NullPointerException if the provided {@code entityDefinition} or {@code concreteEntity} is {@code null}
     * @see #getMappingFor(EntityDefinition)
     */
    public void addCustomEntityMapping(CustomEntityDefinition entityDefinition, String concreteEntity) {
        checkNotNull(entityDefinition, "Cannot register a mapping for the provided %s (%s) please provide a non-null " +
                        "%s", CustomEntityDefinition.class.getSimpleName(), entityDefinition,
                CustomEntityDefinition.class.getSimpleName());
        checkNotNull(concreteEntity, "Cannot register %s as a mapping for the %s %s, please provide a non-null " +
                "mapping", concreteEntity, EntityDefinition.class.getSimpleName(), entityDefinition.getName());
        addMapping(entityDefinition.getName(), concreteEntity);
    }

    /**
     * Registers the provided mapping.
     * <p>
     * This method logs a warning message if another mapping is already registered for the provided {@code key}.
     *
     * @param key   the name of the mapped entity
     * @param value the mapped value associated to the provided {@code entityName}
     * @throws NullPointerException if the provided {@code key} or {@code value} is {@code null}
     */
    private void addMapping(String key, String value) {
        checkNotNull(key, "Cannot register a mapping for the provided key %s", key);
        checkNotNull(value, "Cannot register a mapping with the provided value %s", value);
        String existingMapping = this.entities.get(key);
        if (nonNull(existingMapping)) {
            Log.warn("{0} already contains a mapping for {1} ({2}), overriding it with the given value {3}", this
                    .getClass().getSimpleName(), key, this.entities.get
                    (key), value);
        }
        this.entities.put(key, value);
    }

    /**
     * Sets the default mapping for {@code abstractEntities} that are not mapped to concrete implementations.
     * <p>
     * This method allows to specify a {@code concreteEntity} that will be mapped to all the {@code abstractEntities}
     * that does not have a direct implementation in the {@link IntentRecognitionProvider} platform. (see
     * {@link #getMappingFor(EntityType)}).
     *
     * @param concreteEntity the {@link String} representing the concrete entity to use as fallback entity
     * @throws NullPointerException     if the provided {@code concreteEntity} is {@code null}
     * @throws IllegalArgumentException if a {@code concreteEntity} is already registered as fallback entity
     * @see #getMappingFor(EntityType)
     */
    public void setFallbackEntityMapping(String concreteEntity) {
        checkNotNull(concreteEntity, "Cannot register the concrete fallback entity %s, please provide a non-null " +
                "String", concreteEntity);
        checkArgument(!entities.containsKey(FALLBACK_ENTITY_KEY), "Cannot register the concrete fallback entity %s, " +
                "fallback entity is already mapped to %s", concreteEntity, entities.get(FALLBACK_ENTITY_KEY));
        this.entities.put(FALLBACK_ENTITY_KEY, concreteEntity);
    }

    /**
     * Returns the {@code concreteEntity} mapped to the provided {@code abstractEntityType}.
     * <p>
     * This method looks in the registered mappings (set by calling {@link #addEntityMapping(EntityType, String)}) and
     * returns the one associated to the provided {@code abstractEntityType}. If there is no direct mapping for the
     * {@code abstractEntityType}, the fallback entity (set by calling {@link #setFallbackEntityMapping(String)}) is
     * returned.
     *
     * @param abstractEntityType the {@link String} representing the abstract entity to retrieve the concrete mapping
     *                           from
     * @return the {@code concreteEntity} mapped to the provided {@code abstractEntityType}, or {@code null} if there is
     * no such mapping
     * @throws NullPointerException if the provided {@code abstractEntityType} is {@code null}
     * @see #addEntityMapping(EntityType, String)
     * @see #setFallbackEntityMapping(String)
     */
    public String getMappingFor(EntityType abstractEntityType) {
        checkNotNull(abstractEntityType, "Cannot retrieve the concrete mapping for the abstract entity %s, please " +
                "provide" +
                " a non-null %s", abstractEntityType, EntityType.class.getSimpleName());
        String abstractEntityTypeLiteral = abstractEntityType.getLiteral();
        String concreteEntity = this.entities.get(abstractEntityTypeLiteral);
        if (isNull(concreteEntity)) {
            concreteEntity = this.entities.get(FALLBACK_ENTITY_KEY);
            Log.warn("Cannot find a mapping for the abstract entity {0}, returning the fallback entity ({1})",
                    abstractEntityTypeLiteral, concreteEntity);
        }
        return concreteEntity;
    }

    /**
     * Returns the {@code concreteEntity} mapped to the provided {@code abstractEntity}.
     * <p>
     * This method looks in the registered mappings (set by calling {@link #addEntityMapping(EntityType, String)}) and
     * returns the one associated to name extracted from the provided {@code abstractEntity}. Name extraction will
     * downcast the provided {@link EntityDefinition} to {@link BaseEntityDefinition}, and compute the
     * {@link EntityDefinition} name from its associated {@link EntityType} literal. Note that other subclasses of
     * {@link EntityDefinition} are not supported for now (see
     * <a href="https://github.com/gdaniel/jarvis/issues/145">#145</a>).
     * <p>
     * If there is no direct mapping for the {@code abstractEntity} extracted name, the fallback entity (set by
     * calling {@link #setFallbackEntityMapping(String)} is returned.
     *
     * @param abstractEntity the {@link EntityDefinition} representing the abstract entity to retrieve the concrete
     *                       mapping from
     * @return the {@code concreteEntity} mapped to the provided {@code abstractEntity}, or {@code null} if there is
     * no such mapping
     * @throws NullPointerException     if the provided {@code abstractEntity} is {@code null}
     * @throws IllegalArgumentException if the provided {@link EntityDefinition} is a {@link BaseEntityDefinition}
     *                                  and its {@code entityType} reference is {@code null}
     * @throws JarvisException          if the provided {@code abstractEntity} is not an instance of
     *                                  {@link BaseEntityDefinition} (see
     *                                  <href="https://github.com/gdaniel/jarvis/issues/145">#145</href="">)
     */
    public String getMappingFor(EntityDefinition abstractEntity) {
        checkNotNull(abstractEntity, "Cannot retrieve the concrete mapping for the provided %s %s", EntityDefinition
                .class.getSimpleName(), abstractEntity);
        if (abstractEntity instanceof BaseEntityDefinition) {
            BaseEntityDefinition coreEntity = (BaseEntityDefinition) abstractEntity;
            /*
             * Should not be triggered, null enums are set with their default value in Ecore.
             */
            checkArgument(nonNull(coreEntity.getEntityType()), "Cannot retrieve the concrete mapping for the provided" +
                            " %s: %s needs to define a valid %s", BaseEntityDefinition.class.getSimpleName(),
                    BaseEntityDefinition.class.getSimpleName(), EntityType.class.getSimpleName());
            return this.getMappingFor(((BaseEntityDefinition) abstractEntity).getEntityType());
        } else if (abstractEntity instanceof CustomEntityDefinition) {
            return getMappingForCustomEntity((CustomEntityDefinition) abstractEntity);
        } else {
            throw new JarvisException(MessageFormat.format("{0} does not support the provided {1} {2}", this.getClass
                    ().getSimpleName(), EntityDefinition.class.getSimpleName(), abstractEntity.getClass()
                    .getSimpleName()));
        }
    }

    /**
     * Returns the {@link String} representing the entity mapped from the provided {@code customEntityDefinition}.
     * <p>
     * <b>Note:</b> this method looks for a registered mapping associated to the provided {@code
     * customEntityDefinition}'s name. Concrete {@link EntityMapper}s tailored to a given intent recognition platform
     * can override this method to compute advanced mapping for {@link CustomEntityDefinition}s.
     *
     * @param customEntityDefinition the {@link CustomEntityDefinition} to retrieve the concrete entity
     *                               {@link String} from
     * @return the mapped concrete entity {@link String}
     */
    protected String getMappingForCustomEntity(CustomEntityDefinition customEntityDefinition) {
        return this.entities.get(customEntityDefinition.getName());
    }
}

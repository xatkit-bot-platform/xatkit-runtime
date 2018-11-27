package edu.uoc.som.jarvis.core.recognition;

import edu.uoc.som.jarvis.core.JarvisException;
import edu.uoc.som.jarvis.intent.BaseEntityDefinition;
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
 * This class is configured through the {@link #addEntityMapping(String, String)} method, that allows to specify a
 * mapping from an abstract entity to a concrete entity name. This mapping is used to retrieve
 * {@link IntentRecognitionProvider} specific entities, and deploy jarvis generically on different platforms.
 * <p>
 * This class also defines the {@link #setFallbackEntityMapping(String)}, that allows to handle abstract entities
 * that do not have a concrete mapping in the {@link IntentRecognitionProvider} platform.
 * <p>
 * The mapping for a given abstract entity can be retrieved by calling the {@link #getMappingFor(String)} with the
 * {@code abstractEntity} name as parameter.
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
     * {@link #addEntityMapping(String, String)} and {@link #setFallbackEntityMapping(String)}.
     *
     * @see #addEntityMapping(String, String)
     * @see #setFallbackEntityMapping(String)
     */
    public EntityMapper() {
        this.entities = new HashMap<>();
    }

    /**
     * Adds a new mapping between the provided {@code abstractEntity} and the given {@code concreteEntity}.
     * <p>
     * The stored mapping can be accessed by calling {@link #getMappingFor(String)} with {@code abstractEntity} as
     * parameter.
     *
     * @param abstractEntity the {@link String} representing the abstract entity to map
     * @param concreteEntity the {@link String} representing the concrete entity to associate to the provided {@code
     *                       abstractEntity}
     * @throws NullPointerException if the provided {@code abstractEntity} or {@code concreteEntity} is {@code null}
     * @see #getMappingFor(String)
     */
    public void addEntityMapping(String abstractEntity, String concreteEntity) {
        checkNotNull(abstractEntity, "Cannot register a mapping for the abstract entity %s, please provide a " +
                "non-null String", abstractEntity);
        checkNotNull(concreteEntity, "Cannot register a mapping for the concrete entity %s, please provide a " +
                "non-null String", concreteEntity);
        if (this.entities.containsKey(abstractEntity)) {
            Log.warn("{0} already contains a mapping for {1} ({2}), overriding it with the given value {3}", this
                    .getClass().getSimpleName(), abstractEntity, this.entities.get(abstractEntity), concreteEntity);
        }
        this.entities.put(abstractEntity, concreteEntity);
    }

    /**
     * Sets the default mapping for {@code abstractEntities} that are not mapped to concrete implementations.
     * <p>
     * This method allows to specify a {@code concreteEntity} that will be mapped to all the {@code abstractEntities}
     * that does not have a direct implementation in the {@link IntentRecognitionProvider} platform. (see
     * {@link #getMappingFor(String)}).
     *
     * @param concreteEntity the {@link String} representing the concrete entity to use as fallback entity
     * @throws NullPointerException     if the provided {@code concreteEntity} is {@code null}
     * @throws IllegalArgumentException if a {@code concreteEntity} is already registered as fallback entity
     * @see #getMappingFor(String)
     */
    public void setFallbackEntityMapping(String concreteEntity) {
        checkNotNull(concreteEntity, "Cannot register the concrete fallback entity %s, please provide a non-null " +
                "String", concreteEntity);
        checkArgument(!entities.containsKey(FALLBACK_ENTITY_KEY), "Cannot register the concrete fallback entity %s, " +
                "fallback entity is already mapped to %s", concreteEntity, entities.get(FALLBACK_ENTITY_KEY));
        this.entities.put(FALLBACK_ENTITY_KEY, concreteEntity);
    }

    /**
     * Returns the {@code concreteEntity} mapped to the provided {@code abstractEntity}.
     * <p>
     * This method looks in the registered mappings (set by calling {@link #addEntityMapping(String, String)}) and
     * returns the one associated to the provided {@code abstractEntity}. If there is no direct mapping for the
     * {@code abstractEntity}, the fallback entity (set by calling {@link #setFallbackEntityMapping(String)}) is
     * returned.
     *
     * @param abstractEntity the {@link String} representing the abstract entity to retrieve the concrete mapping from
     * @return the {@code concreteEntity} mapped to the provided {@code abstractEntity}, or {@code null} if there is
     * no such mapping
     * @throws NullPointerException if the provided {@code abstractEntity} is {@code null}
     * @see #addEntityMapping(String, String)
     * @see #setFallbackEntityMapping(String)
     */
    public String getMappingFor(String abstractEntity) {
        checkNotNull(abstractEntity, "Cannot retrieve the concrete mapping for the abstract entity %s, please provide" +
                " a non-null String", abstractEntity);
        String concreteEntity = this.entities.get(abstractEntity);
        if (isNull(concreteEntity)) {
            concreteEntity = this.entities.get(FALLBACK_ENTITY_KEY);
            Log.warn("Cannot find a mapping for the abstract entity {0}, returning the fallback entity ({1})",
                    abstractEntity, concreteEntity);
        }
        return concreteEntity;
    }

    /**
     * Returns the {@code concreteEntity} mapped to the provided {@code abstractEntity}.
     * <p>
     * This method looks in the registered mappings (set by calling {@link #addEntityMapping(String, String)}) and
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
            return this.getMappingFor(((BaseEntityDefinition) abstractEntity).getEntityType().getLiteral());
        } else {
            /*
             * Non-CoreEntity instances are not supported for now (see #145)
             */
            throw new JarvisException(MessageFormat.format("{0} does not support the provided {1} {2}", this.getClass
                    ().getSimpleName(), EntityDefinition.class.getSimpleName(), abstractEntity.getClass()
                    .getSimpleName()));
        }
    }
}

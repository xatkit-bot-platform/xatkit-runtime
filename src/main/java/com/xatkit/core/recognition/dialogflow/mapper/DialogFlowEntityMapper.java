package com.xatkit.core.recognition.dialogflow.mapper;

import com.google.cloud.dialogflow.v2.EntityType;
import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.CompositeEntityDefinitionEntry;
import com.xatkit.intent.CustomEntityDefinition;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.EntityTextFragment;
import com.xatkit.intent.LiteralTextFragment;
import com.xatkit.intent.MappingEntityDefinition;
import com.xatkit.intent.MappingEntityDefinitionEntry;
import com.xatkit.intent.TextFragment;
import lombok.NonNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps {@link EntityDefinition} instances to DialogFlow {@link EntityType}s.
 * <p>
 * This class is used to translate generic {@link EntityDefinition}s to platform-specific construct representing
 * DialogFlow entities.
 * <p>
 * <b>Note</b>: this class does not create references to existing entities, see
 * {@link DialogFlowEntityReferenceMapper} for more information.
 *
 * @see DialogFlowEntityReferenceMapper
 */
public class DialogFlowEntityMapper {

    /**
     * The {@link DialogFlowEntityReferenceMapper} used to map internal references to other entities.
     * <p>
     * These references are typically found in {@link CompositeEntityDefinition}s, that can contain other entities as
     * part of their values.
     */
    private DialogFlowEntityReferenceMapper entityReferenceMapper;

    /**
     * Constructs a {@link DialogFlowEntityMapper} with the provided {@code entityReferenceMapper}.
     *
     * @param entityReferenceMapper the {@link DialogFlowEntityReferenceMapper} used to map internal references to
     *                              other entities
     * @throws NullPointerException if the provided {@code entityReferenceMapper} is {@code null}
     */
    public DialogFlowEntityMapper(@NonNull DialogFlowEntityReferenceMapper entityReferenceMapper) {
        this.entityReferenceMapper = entityReferenceMapper;
    }

    /**
     * Maps the provided {@code entityDefinition} to a DialogFlow {@link EntityType}.
     * <p>
     * This method does not support {@link BaseEntityDefinition}s, because base entities are already deployed in
     * DialogFlow agents (and called system entities).
     *
     * @param entityDefinition the {@link EntityDefinition} to map
     * @return the created {@link EntityType}
     * @throws NullPointerException     if the provided {@code entityDefinition} is {@code null}
     * @throws IllegalArgumentException if the provided {@code entityDefinition} is a
     *                                  {@link BaseEntityDefinition}, or if the provided {@code entityDefinition}'s
     *                                  type is not supported
     */
    public EntityType mapEntityDefinition(@NonNull EntityDefinition entityDefinition) {
        if (entityDefinition instanceof BaseEntityDefinition) {
            throw new IllegalArgumentException(MessageFormat.format("Cannot map the provided {0} {1}, base entities "
                            + "are already mapped in DialogFlow", EntityDefinition.class.getSimpleName(),
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
     * Creates a DialogFlow {@link EntityType} from the provided {@code customEntityDefinition}.
     *
     * @param customEntityDefinition the {@link CustomEntityDefinition} to create an {@link EntityType} from
     * @return the created {@link EntityType}
     * @throws NullPointerException     if the provided {@code entityDefinition} is {@code null}
     * @throws IllegalArgumentException if the {@code customEntityDefinition}'s type is not supported
     * @see #createEntitiesForMapping(MappingEntityDefinition)
     * @see #createEntitiesForComposite(CompositeEntityDefinition)
     */
    private EntityType mapCustomEntityDefinition(@NonNull CustomEntityDefinition customEntityDefinition) {
        String entityName = customEntityDefinition.getName();
        EntityType.Builder builder = EntityType.newBuilder().setDisplayName(entityName);
        if (customEntityDefinition instanceof MappingEntityDefinition) {
            MappingEntityDefinition mappingEntityDefinition = (MappingEntityDefinition) customEntityDefinition;
            List<EntityType.Entity> entities = createEntitiesForMapping(mappingEntityDefinition);
            builder.setKind(EntityType.Kind.KIND_MAP).addAllEntities(entities);
        } else if (customEntityDefinition instanceof CompositeEntityDefinition) {
            CompositeEntityDefinition compositeEntityDefinition = (CompositeEntityDefinition) customEntityDefinition;
            List<EntityType.Entity> entities = createEntitiesForComposite(compositeEntityDefinition);
            builder.setKind(EntityType.Kind.KIND_LIST).addAllEntities(entities);
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Cannot register the provided {0}, unsupported {1}",
                    customEntityDefinition.getClass().getSimpleName(), EntityDefinition.class.getSimpleName()));
        }
        return builder.build();
    }

    /**
     * Creates the DialogFlow {@link EntityType.Entity} instances from the provided {@code mappingEntityDefinition}.
     * <p>
     * {@link EntityType.Entity} instances are created from the provided {@link MappingEntityDefinition}'s entries,
     * and contain the specified <i>referredValue</i> as well as the list of <i>synonyms</i>. The created
     * {@link EntityType.Entity} instances correspond to DialogFlow's
     * <a href="https://dialogflow.com/docs/entities/developer-entities#developer_mapping">Developer Mapping
     * Entities</a>.
     *
     * @param mappingEntityDefinition the {@link MappingEntityDefinition} to create the {@link EntityType.Entity}
     *                                instances from
     * @return the created {@link List} of DialogFlow {@link EntityType.Entity} instances
     * @throws NullPointerException if the provided {@code mappingEntityDefinition} is {@code null}
     */
    private List<EntityType.Entity> createEntitiesForMapping(@NonNull MappingEntityDefinition mappingEntityDefinition) {
        List<EntityType.Entity> entities = new ArrayList<>();
        for (MappingEntityDefinitionEntry entry : mappingEntityDefinition.getEntries()) {
            EntityType.Entity.Builder builder = EntityType.Entity.newBuilder().setValue(entry.getReferenceValue())
                    .addAllSynonyms(entry.getSynonyms()).addSynonyms(entry.getReferenceValue());
            entities.add(builder.build());
        }
        return entities;
    }

    /**
     * Creates the DialogFlow {@link EntityType.Entity} instances from the provided {@code compositeEntity}.
     * <p>
     * {@link EntityType.Entity} instances are created from the provided {@link CompositeEntityDefinition}'s entries,
     * and contain a valid String representation of their <i>value</i> (see
     * {@link #createEntityValue(CompositeEntityDefinitionEntry)}). The created {@link EntityType.Entity} instances
     * correspond to DialogFlow's
     * <a href="https://dialogflow.com/docs/entities/developer-entities#developer_enum">Developer Enums Entities</a>.
     * <p>
     * <b>Note</b>: this method does not check whether referred entities are deployed in the DialogFlow agent.
     *
     * @param compositeEntity the {@link CompositeEntityDefinition} to create the {@link EntityType.Entity}
     *                        instances from
     * @return the create {@link List} of DialogFlow {@link EntityType.Entity} instances
     * @throws NullPointerException if the provided {@code compositeEntity} is {@code null}
     */
    private List<EntityType.Entity> createEntitiesForComposite(@NonNull CompositeEntityDefinition compositeEntity) {
        List<EntityType.Entity> entities = new ArrayList<>();
        for (CompositeEntityDefinitionEntry entry : compositeEntity.getEntries()) {
            String valueString = createEntityValue(entry);
            /*
             * Add the created value as the only synonym for the created Entity: DialogFlow does not allow to create
             * Entities that does not contain their value in their synonym list.
             */
            EntityType.Entity.Builder builder =
                    EntityType.Entity.newBuilder().setValue(valueString).addSynonyms(valueString);
            entities.add(builder.build());
        }
        return entities;
    }

    /**
     * Creates a valid String entity value from the provided {@code entry}.
     * <p>
     * This method iterates the {@code entry}'s {@link TextFragment}s and merge them in a String that can be used as
     * the value of a DialogFlow {@link EntityType.Entity}. Note that {@link EntityTextFragment}s are translated
     * based on the mapping defined in the {@link DialogFlowEntityReferenceMapper}, and the name of their corresponding
     * variable is set based on their name.
     *
     * @param entry the {@link CompositeEntityDefinition} to create an entity value from
     * @return the create entity value
     * @throws NullPointerException if the provided {@code entry} is {@code null}
     */
    private String createEntityValue(@NonNull CompositeEntityDefinitionEntry entry) {
        StringBuilder sb = new StringBuilder();
        for (TextFragment fragment : entry.getFragments()) {
            if (fragment instanceof LiteralTextFragment) {
                sb.append(((LiteralTextFragment) fragment).getValue());
            } else if (fragment instanceof EntityTextFragment) {
                /*
                 * Builds a String with the entity name and a default parameter value. The parameter value is set
                 * with the name of the entity itself (eg. @Class:Class). This is fine for composite entities
                 * referring once to their entities, but does not scale to more complex ones with multiple references
                 * to the same entity (see #199).
                 */
                EntityDefinition fragmentEntity = ((EntityTextFragment) fragment).getEntityReference()
                        .getReferredEntity();
                String mappedEntity = entityReferenceMapper.getMappingFor(fragmentEntity);
                String mappedEntityParameterName = fragmentEntity.getName();
                sb.append(mappedEntity);
                sb.append(":");
                sb.append(mappedEntityParameterName);
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}

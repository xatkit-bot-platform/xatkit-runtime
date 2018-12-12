package edu.uoc.som.jarvis.core.recognition.dialogflow;

import edu.uoc.som.jarvis.core.recognition.EntityMapper;
import edu.uoc.som.jarvis.intent.EntityDefinition;
import edu.uoc.som.jarvis.intent.EntityType;

/**
 * An {@link EntityMapper} initializes with DialogFlow's system entities.
 * <p>
 * This class provides a mapping of {@link EntityType}s to DialogFlow's system entities. Mapped entities can be
 * accessed by calling {@link #getMappingFor(EntityDefinition)}.
 */
public class DialogFlowEntityMapper extends EntityMapper {

    /**
     * Constructs a {@link DialogFlowEntityMapper} initialized with DialogFlow's system entities.
     */
    public DialogFlowEntityMapper() {
        super();
        this.addEntityMapping(EntityType.CITY.getLiteral(), "@sys.geo-city");
        this.addEntityMapping(EntityType.ANY.getLiteral(), "@sys.any");
        this.setFallbackEntityMapping("@sys.any");
    }
}

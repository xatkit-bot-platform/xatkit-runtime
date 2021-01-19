package com.xatkit.core.recognition.nlpjs.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class Entity {

    private String entityName;

    private EntityType type;

    @Singular
    private List<EntityValue> references;

    @Singular("addAfterLast")
    private List<String> afterLast;

    @Singular("addBeforeLast")
    private List<String> beforeLast;

    private BetweenCondition between;

    private String regex;
}

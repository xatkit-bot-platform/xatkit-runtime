package com.xatkit.core.recognition.nlpjs.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class Entity {

    private String entityName;

    private EntityType type;

    @Singular
    private List<EntityValue> references;

    @Builder.Default
    private List<String> afterLast = new ArrayList<>();

    @Builder.Default
    private List<String> beforeLast = new ArrayList<>();

    private BetweenCondition between;

    private String regex;
}

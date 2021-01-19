package com.xatkit.core.recognition.nlpjs.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class EntityValue {

    private String value;

    @Singular
    private List<String> synonyms;
}

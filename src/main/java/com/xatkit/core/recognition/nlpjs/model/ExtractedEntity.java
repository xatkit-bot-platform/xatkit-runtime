package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

@Data
public class ExtractedEntity {

    private Float accuracy;

    private String entity;

    private String type;

    private String subType;

    private String sourceText;

    private String utteranceText;

    private Object value;

}
package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

@Data
public class ExtractedEntity {

    private Double start;

    private Double end;

    private Double len;

    private Double accuracy;

    private String entity;

    private String type;

    private String option;

    private String sourceText;

    private String utteranceText;

    private Resolution resolution;

}
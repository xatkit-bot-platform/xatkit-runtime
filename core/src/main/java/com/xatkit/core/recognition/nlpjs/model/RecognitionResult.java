package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

import java.util.List;

@Data
public class RecognitionResult {

    private String local;

    private String utterance;

    private String languageGuessed;

    private String localIso2;

    private String language;

    private List<Classification> classifications;

    private Float score;

    private String domain;

    private List<ExtractedEntity> entities;
}

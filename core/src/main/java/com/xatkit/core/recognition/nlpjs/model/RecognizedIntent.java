package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

import java.util.List;

@Data
public class RecognizedIntent {
    private String local;
    private String utterance;
    private String languageGuessed;
    private String localIso2;
    private String language;
    private List<Classification> classification;
    private String intent;
    private Double score;
    private String domaine;
    private List<ExtractedEntity> entities;
}

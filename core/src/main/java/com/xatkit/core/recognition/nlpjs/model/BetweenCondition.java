package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BetweenCondition {

    public BetweenCondition() {
        left = new ArrayList<>();
        right = new ArrayList<>();
    }

    private List<String> left;

    private List<String> right;

}

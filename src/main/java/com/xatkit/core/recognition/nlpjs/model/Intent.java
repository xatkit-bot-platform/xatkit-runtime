package com.xatkit.core.recognition.nlpjs.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Intent {

    private String intentName;

    private List<IntentExample> examples;

    private List<IntentParameter> parameters;

    public static Builder newBuilder(){
        return new Intent.Builder();
    }


    public static class Builder {
        private String _intentName;
        private List<IntentExample> _examples;
        private List<IntentParameter> _parameters;

        public Builder(){
            _examples = new ArrayList<>();
            _parameters = new ArrayList<>();
        }

        public Intent.Builder intentName(String intentName){
            _intentName = intentName;
            return this;
        }

        public Intent.Builder examples(List<IntentExample> examples){
            _examples = examples;
            return this;
        }

        public Intent.Builder addExample(IntentExample intentExample){
            _examples.add(intentExample);
            return this;
        }

        public Intent.Builder parameters(List<IntentParameter> parameters){
            _parameters = parameters;
            return this;
        }

        public Intent.Builder addParameter(IntentParameter intentParameter){
            _parameters.add(intentParameter);
            return this;
        }

        public Intent build(){
            Intent intent = new Intent();
            intent.setIntentName(_intentName);
            intent.setExamples(_examples);
            intent.setParameters(_parameters);
            return intent;

        }

    }

}

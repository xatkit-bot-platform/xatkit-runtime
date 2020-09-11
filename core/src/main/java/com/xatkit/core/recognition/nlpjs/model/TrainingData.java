package com.xatkit.core.recognition.nlpjs.model;

import com.xatkit.intent.IntentDefinition;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static fr.inria.atlanmod.commons.Preconditions.checkArgument;

@Data
public class TrainingData {

    private AgentConfig config;

    private List<Intent> intents;

    private List<Entity> entities;

    private TrainingData() {}
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private AgentConfig _config;

        private List<Intent> _intents;

        private List<Entity> _entities;

        public Builder config(AgentConfig _config) {
            this._config = _config;
            return this;
        }

        public Builder intents(List<Intent> _intents) {
            this._intents = _intents;
            return this;
        }

        public Builder entities(List<Entity> _entities) {
            this._entities = _entities;
            return this;
        }

        public TrainingData build() {
            checkNotNull(_config, "Cannot train the bot with the provided configuration %s",
                    AgentConfig.class.getSimpleName());
            checkNotNull(_intents, "Cannot train the bot with the provided intents %s",
                    Intent.class.getSimpleName());
            checkArgument(!_intents.isEmpty(), "Cannot train the bot with an empty intent list %s",
                    Intent.class.getSimpleName());
            TrainingData trainingData = new TrainingData();
            trainingData.setConfig(_config);
            trainingData.setEntities(_entities);
            trainingData.setIntents(_intents);
            return trainingData;
        }



    }



}

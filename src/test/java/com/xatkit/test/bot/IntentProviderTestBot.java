package com.xatkit.test.bot;

import com.xatkit.execution.ExecutionModel;
import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.CustomEntityDefinitionReference;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.MappingEntityDefinition;
import lombok.Value;
import lombok.val;

import static com.xatkit.dsl.DSL.any;
import static com.xatkit.dsl.DSL.city;
import static com.xatkit.dsl.DSL.composite;
import static com.xatkit.dsl.DSL.fallbackState;
import static com.xatkit.dsl.DSL.intent;
import static com.xatkit.dsl.DSL.intentIs;
import static com.xatkit.dsl.DSL.mapping;
import static com.xatkit.dsl.DSL.model;
import static com.xatkit.dsl.DSL.state;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

@Value
public class IntentProviderTestBot {

    IntentDefinition simpleIntent;

    IntentDefinition systemEntityIntent;

    MappingEntityDefinition mappingEntity;

    IntentDefinition mappingEntityIntent;

    CompositeEntityDefinition compositeEntity;

    IntentDefinition compositeEntityIntent;

    ExecutionModel model;

    public IntentProviderTestBot() {
        this.simpleIntent = createSimpleIntent();
        this.systemEntityIntent = createSystemEntityIntent();
        this.mappingEntity = createMappingEntity();
        this.mappingEntityIntent = createMappingEntityIntent();
        this.compositeEntity = createCompositeEntity();
        this.compositeEntityIntent = createCompositeEntityIntent();
        this.model = createExecutionModel();

    }

    private IntentDefinition createSimpleIntent() {
        return intent("SimpleIntent")
                .trainingSentence("Greetings")
                .getIntentDefinition();
    }

    private IntentDefinition createSystemEntityIntent() {
        return intent("SystemEntityIntent")
                .trainingSentence("Hello World")
                .parameter("helloTo")
                .fromFragment("World")
                .entity(any())
                .getIntentDefinition();
    }

    private MappingEntityDefinition createMappingEntity() {
        return (MappingEntityDefinition) mapping("XatkitFounder")
                .entry()
                .value("Gwendal").synonym("gdaniel")
                .entry()
                .value("Jordi").synonym("jcabot")
                .getEntity();
    }

    private IntentDefinition createMappingEntityIntent() {
        checkNotNull(this.mappingEntity, "Cannot create a mapping entity intent: the mapping entity hasn't been set");
        CustomEntityDefinitionReference reference = IntentFactory.eINSTANCE.createCustomEntityDefinitionReference();
        reference.setCustomEntity(this.mappingEntity);
        return intent("MappingEntityIntent")
                .trainingSentence("Give me some information about FOUNDER")
                .parameter("name")
                .fromFragment("FOUNDER")
                .entity(reference)
                .getIntentDefinition();
    }

    private CompositeEntityDefinition createCompositeEntity() {
        checkNotNull(this.mappingEntity, "Cannot create the composite entity: the used mapping entity hasn't been set");
        CustomEntityDefinitionReference reference = IntentFactory.eINSTANCE.createCustomEntityDefinitionReference();
        reference.setCustomEntity(this.mappingEntity);
        return (CompositeEntityDefinition) composite("FounderCity")
                .entry()
                    .entity(reference).text("knows").entity(city())
                .getEntity();
    }

    private IntentDefinition createCompositeEntityIntent() {
        checkNotNull(this.compositeEntity, "Cannot create a composite entity intent: the composite entity hasn't been" +
                " set");
        CustomEntityDefinitionReference reference = IntentFactory.eINSTANCE.createCustomEntityDefinitionReference();
        reference.setCustomEntity(this.compositeEntity);
        return intent("CompositeEntityIntent")
                .trainingSentence("Does Jordi knows Barcelona?")
                .parameter("founderCity")
                .fromFragment("Jordi knows Barcelona")
                .entity(reference)
                .getIntentDefinition();
    }

    private ExecutionModel createExecutionModel() {
        val initState = state("Init");
        val endState = state("End");
        initState
                .next()
                    .when(intentIs(this.simpleIntent)).moveTo(endState)
                    .when(intentIs(this.systemEntityIntent)).moveTo(endState)
                    .when(intentIs(this.mappingEntityIntent)).moveTo(endState)
                    .when(intentIs(this.compositeEntityIntent)).moveTo(endState);
        endState
                .next()
                    .moveTo(initState);

        val fallback = fallbackState();

        return model()
                .useIntent(this.simpleIntent)
                .useIntent(this.mappingEntityIntent)
                .useIntent(this.compositeEntityIntent)
                .useState(endState)
                .initState(initState)
                .defaultFallbackState(fallback)
                .getExecutionModel();
    }


}

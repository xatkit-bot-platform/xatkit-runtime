package com.xatkit.test.util;

import com.xatkit.execution.ExecutionModel;
import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.MappingEntityDefinition;
import com.xatkit.util.ExecutionModelHelper;
import lombok.Getter;

import java.text.MessageFormat;

import static java.util.Objects.isNull;

public class TestBotExecutionModel {

    @Getter
    private ExecutionModel baseModel;

    @Getter
    private IntentDefinition simpleIntent;

    @Getter
    private IntentDefinition systemEntityIntent;

    @Getter
    private IntentDefinition mappingEntityIntent;

    @Getter
    private IntentDefinition compositeEntityIntent;

    @Getter
    private MappingEntityDefinition mappingEntity;

    @Getter
    private CompositeEntityDefinition compositeEntity;

    public TestBotExecutionModel(ExecutionModel baseModel) {
        ExecutionModelHelper executionModelHelper = ExecutionModelHelper.create(baseModel);
        for (EventDefinition eventDefinition : executionModelHelper.getAllAccessedEvents()) {
            if (eventDefinition instanceof IntentDefinition) {
                IntentDefinition intent = (IntentDefinition) eventDefinition;
                switch (intent.getName()) {
                    case "SimpleIntent":
                        if (isNull(simpleIntent)) {
                            simpleIntent = intent;
                        } else {
                            throw new IllegalStateException(MessageFormat.format("Cannot initialize the simpleIntent " +
                                            "field with {0}, the field has been set already (value: {1})",
                                    intent.toString(),
                                    simpleIntent.toString()));
                        }
                        break;
                    case "SystemEntityIntent":
                        if (isNull(systemEntityIntent)) {
                            systemEntityIntent = intent;
                        } else {
                            throw new IllegalStateException(MessageFormat.format("Cannot initialize the " +
                                    "systemEntityIntent field with {0}, the field has been set already " +
                                    "(value: {1})", intent.toString(), systemEntityIntent.toString()));
                        }
                        break;
                    case "MappingEntityIntent":
                        if (isNull(mappingEntityIntent)) {
                            mappingEntityIntent = intent;
                        } else {
                            throw new IllegalStateException(MessageFormat.format("Cannot initialize the " +
                                    "mappingEntityIntent field with {0}, the field has been set already " +
                                    "(value: {1})", intent.toString(), mappingEntityIntent.toString()));
                        }
                        break;
                    case "CompositeEntityIntent":
                        if (isNull(compositeEntityIntent)) {
                            compositeEntityIntent = intent;
                        } else {
                            throw new IllegalStateException(MessageFormat.format("Cannot initialize the " +
                                    "compositeEntityIntent field with {0}, the field has been set already " +
                                    "(value: {1})", intent.toString(), compositeEntityIntent.toString()));
                        }
                        break;
                }
            }
        }
        baseModel.eResource().getResourceSet().getAllContents().forEachRemaining(o -> {
            if (o instanceof MappingEntityDefinition) {
                if (isNull(mappingEntity)) {
                    mappingEntity = (MappingEntityDefinition) o;
                } else {
                    throw new IllegalStateException(MessageFormat.format("Cannot initialize the {0} with {1}, the " +
                                    "field has been set already (value: {2})",
                            MappingEntityDefinition.class.getSimpleName(),
                            o.toString(), mappingEntity.toString()));
                }
            } else if (o instanceof CompositeEntityDefinition) {
                if (isNull(compositeEntity)) {
                    compositeEntity = (CompositeEntityDefinition) o;
                } else {
                    throw new IllegalStateException(MessageFormat.format("Cannot initialize the {0} with {1}, the " +
                                    "field has been set already (value: {2})",
                            CompositeEntityDefinition.class.getSimpleName(),
                            o.toString(), compositeEntity.toString()));
                }
            }
        });
    }
}

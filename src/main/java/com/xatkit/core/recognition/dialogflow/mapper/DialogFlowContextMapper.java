package com.xatkit.core.recognition.dialogflow.mapper;

import com.google.cloud.dialogflow.v2.Context;
import com.google.cloud.dialogflow.v2.ContextName;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.xatkit.core.recognition.dialogflow.DialogFlowConfiguration;
import com.xatkit.core.recognition.dialogflow.DialogFlowStateContext;
import com.xatkit.execution.State;
import com.xatkit.intent.IntentDefinition;
import lombok.NonNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maps {@link DialogFlowStateContext} instances to DialogFlow {@link Context}s.
 * <p>
 * This mapper is used to create an {@link Iterable} of {@link Context}s representing a snapshot of a given
 * {@link DialogFlowStateContext}, that can be used to deploy contexts on DialogFlow, or ensure the contexts are
 * correctly
 * set before detecting an intent.
 */
public class DialogFlowContextMapper {

    /**
     * The {@link DialogFlowConfiguration}.
     * <p>
     * This configuration is used to retrieve the DialogFlow project ID, and use it to generate the {@link Context}
     * names.
     */
    private DialogFlowConfiguration configuration;

    /**
     * Constructs a {@link DialogFlowContextMapper} with the provided {@code configuration}.
     *
     * @param configuration the {@link DialogFlowConfiguration}
     * @throws NullPointerException if the provided {@code configuration} is {@code null}
     */
    public DialogFlowContextMapper(@NonNull DialogFlowConfiguration configuration) {
        this.configuration = configuration;
    }

    // TODO documentation
    public @NonNull Iterable<Context> createOutContextsForState(@NonNull DialogFlowStateContext context) {
        List<Context> result = new ArrayList<>();
        State state = context.getState();
        Iterable<IntentDefinition> accessedIntents = state.getAllAccessedIntents();
        accessedIntents.forEach(intent -> {
            // TODO check the lifespan count, should it be 2 or 1?
            Context.Builder builder = Context.newBuilder().setName(ContextName.of(this.configuration.getProjectId(),
                    context.getSessionName().getSession(), "Enable" + intent.getName()).toString()).setLifespanCount(1);
            result.add(builder.build());
        });
        return result;
    }

    /**
     * Creates a protobuf {@link Value} from the provided {@link Object}.
     * <p>
     * This method supports {@link String} and {@link Map} as input, other data types should not be passed to this
     * method, because all the values returned by DialogFlow are translated into {@link String} or {@link Map}.
     *
     * @param from the {@link Object} to translate to a protobuf {@link Value}
     * @return the protobuf {@link Value}
     * @throws IllegalArgumentException if the provided {@link Object}'s type is not supported
     * @see #buildStruct(Map)
     */
    private Value buildValue(Object from) {
        Value.Builder valueBuilder = Value.newBuilder();
        if (from instanceof String) {
            valueBuilder.setStringValue((String) from);
            return valueBuilder.build();
        } else if (from instanceof Map) {
            Struct struct = buildStruct((Map<String, Object>) from);
            return valueBuilder.setStructValue(struct).build();
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Cannot build a protobuf value from {0}", from));
        }
    }

    /**
     * Creates a protobuf {@link Struct} from the provided {@link Map}.
     * <p>
     * This method deals with nested {@link Map}s, as long as their values are {@link String}s. The returned
     * {@link Struct} reflects the {@link Map} nesting hierarchy.
     *
     * @param fromMap the {@link Map} to translate to a protobuf {@link Struct}
     * @return the protobuf {@link Struct}
     * @throws IllegalArgumentException if a nested {@link Map}'s value type is not {@link String} or {@link Map}
     */
    private Struct buildStruct(Map<String, Object> fromMap) {
        Struct.Builder structBuilder = Struct.newBuilder();
        for (Map.Entry<String, Object> entry : fromMap.entrySet()) {
            if (entry.getValue() instanceof String) {
                structBuilder.putFields(entry.getKey(),
                        Value.newBuilder().setStringValue((String) entry.getValue()).build());
            } else if (entry.getValue() instanceof Map) {
                structBuilder.putFields(entry.getKey(), Value.newBuilder().setStructValue(buildStruct((Map<String,
                        Object>) entry.getValue())).build());
            } else {
                throw new IllegalArgumentException(MessageFormat.format("Cannot build a protobuf struct from {0}, "
                        + "unsupported data type", fromMap));
            }
        }
        return structBuilder.build();
    }
}

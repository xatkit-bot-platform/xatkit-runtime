package com.xatkit.core.recognition.dialogflow.mapper;

import com.google.cloud.dialogflow.v2.Context;
import com.google.cloud.dialogflow.v2.ContextName;
import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.xatkit.core.recognition.dialogflow.DialogFlowConfiguration;
import com.xatkit.core.recognition.dialogflow.DialogFlowSession;
import lombok.NonNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps {@link DialogFlowSession} instances to DialogFlow {@link Context}s.
 * <p>
 * This mapper is used to create an {@link Iterable} of {@link Context}s representing a snapshot of a given
 * {@link DialogFlowSession}, that can be used to deploy contexts on DialogFlow, or ensure the contexts are correctly
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

    /**
     * Maps the provided {@link DialogFlowSession} to an {@link Iterable} of DialogFlow {@link Context}s.
     * <p>
     * The returned {@link Iterable} is a mirror of the runtime {@code session} that can be deployed on DialogFlow.
     * It is typically used to make sure contexts are correctly set when calling
     * {@link com.google.cloud.dialogflow.v2.SessionsClient#detectIntent(DetectIntentRequest)}.
     * <p>
     * <b>Note</b>: this method does not call the DialogFlow API to deploy the created {@link Context}s.
     *
     * @param session the {@link DialogFlowSession} to map
     * @return the created {@link Iterable} of {@link Context}
     * @throws NullPointerException if the provided {@code session} is {@code null}
     */
    public Iterable<Context> mapDialogFlowSession(@NonNull DialogFlowSession session) {
        List<Context> result = new ArrayList<>();
        session.getRuntimeContexts().getContextMap().entrySet().stream().forEach(contextEntry ->
        {
            String contextName = contextEntry.getKey();
            int contextLifespanCount = session.getRuntimeContexts().getContextLifespanCount
                    (contextName);
            Context.Builder builder =
                    Context.newBuilder().setName(ContextName.of(this.configuration.getProjectId(),
                            session.getSessionName().getSession(), contextName).toString());
            Map<String, Object> contextVariables = contextEntry.getValue();
            Map<String, Value> dialogFlowContextVariables = new HashMap<>();
            contextVariables.entrySet().stream().forEach(contextVariableEntry -> {
                Value value = buildValue(contextVariableEntry.getValue());
                dialogFlowContextVariables.put(contextVariableEntry.getKey(), value);
            });
            /*
             * Need to put the lifespanCount otherwise the context is ignored.
             */
            builder.setParameters(Struct.newBuilder().putAllFields(dialogFlowContextVariables))
                    .setLifespanCount(contextLifespanCount);
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
                throw new IllegalArgumentException(MessageFormat.format("Cannot build a protobuf struct " +
                        "from {0}, unsupported data type", fromMap));
            }
        }
        return structBuilder.build();
    }
}

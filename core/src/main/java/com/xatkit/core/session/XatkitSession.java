package com.xatkit.core.session;

import com.xatkit.core.XatkitCore;
import com.xatkit.execution.State;
import com.xatkit.execution.StateContext;
import com.xatkit.execution.Transition;
import com.xatkit.execution.impl.StateContextImpl;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.util.ExecutionModelUtils;
import fr.inria.atlanmod.commons.log.Log;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ConfigurationConverter;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A session holding user-related information.
 * <p>
 * A {@link XatkitSession} is bound to a user, and holds all the volatile information related to the current
 * conversation. A {@link XatkitSession} contains a {@link RuntimeContexts}, that represents the contextual variables
 * of the current conversation.
 *
 * @see RuntimeContexts
 * @see XatkitCore#getOrCreateXatkitSession(String)
 */
public class XatkitSession extends StateContextImpl {

    /**
     * The {@link RuntimeContexts} used to store context-related variables.
     */
    @Getter
    private RuntimeContexts runtimeContexts;

    /**
     * Constructs a new, empty {@link XatkitSession} with the provided {@code sessionId}.
     * See {@link #XatkitSession(String, Configuration)} to construct a {@link XatkitSession} with a given
     * {@link Configuration}.
     *
     * @param contextId the unique identifier of the {@link XatkitSession}
     */
    public XatkitSession(@NonNull String contextId) {
        this(contextId, new BaseConfiguration());
    }

    /**
     * Constructs a new, empty {@link XatkitSession} with the provided {@code sessionId} and {@code configuration}.
     * <p>
     * This constructor forwards the provided {@link Configuration} to the underlying {@link RuntimeContexts} and can
     * be used to customize {@link RuntimeContexts} properties.
     * <p>
     * <b>Note</b>: this method does <i>not</i> set the {@link State} associated to the {@link XatkitSession}. This
     * can be done by calling {@link XatkitSession#setState(State)}. {@link XatkitSession}s created with
     * {@link com.xatkit.core.ExecutionService#initSession(XatkitSession)} are automatically initialized with the {@code
     * Init} {@link State} of the bot's execution model.
     *
     * @param contextId     the unique identifier of the {@link XatkitSession}
     * @param configuration the {@link Configuration} parameterizing the {@link XatkitSession}
     * @throws NullPointerException if the provided {@code sessionId} or {@code configuration} is {@code null}
     * @see #setState(State)
     * @see com.xatkit.core.ExecutionService#initSession(XatkitSession)
     */
    public XatkitSession(@NonNull String contextId, @NonNull Configuration configuration) {
        this.contextId = contextId;
        this.state = null;
        this.setConfiguration(ConfigurationConverter.getMap(configuration));
        this.setSession(new HashMap<>());
        this.runtimeContexts = new RuntimeContexts(configuration);
        this.origin = null;
        Log.info("{0} {1} created", XatkitSession.class.getSimpleName(), this.contextId);
    }

    /**
     * Sets the session's {@link State}.
     * <p>
     * The session's state represents the point in the conversation graph where the user is. It is used by the Xatkit
     * {@link com.xatkit.core.ExecutionService} to execute the logic of the bot and compute actionable transitions.
     * <p>
     * This method also sets the context parameters needed to match the next {@link IntentDefinition}s, if they exist.
     *
     * @param state the {@link State} to set
     */
    @Override
    public void setState(@NonNull State state) {
        Log.debug("Session {0} - State set to {1}", this.getContextId(), state.getName());
        this.state = state;
        for (Transition t : state.getTransitions()) {
            IntentDefinition intentDefinition = ExecutionModelUtils.getAccessedIntent(t);
            if(nonNull(intentDefinition)) {
                this.runtimeContexts.setContext("Enable" + intentDefinition.getName(), 2);
            }
        }
    }

    @Override
    public Map<String, Map<String, Object>> getNlpContext() {
        /*
         * TODO this has been implemented using the outdated RuntimeContexts to save development time, it should be
         * cleaned.
         */
        return runtimeContexts.getContextMap();
    }

    /**
     * Merges this {@link XatkitSession} with the provided {@code other}.
     *
     * @param other the {@link StateContext} to merge in the current session
     * @throws IllegalArgumentException if {@code other} is not a {@link XatkitSession} instance
     */
    @Override
    public void merge(@NonNull StateContext other) {
        checkArgument(other instanceof XatkitSession, "Xannot merge the provided %s %s: expected a %s, found %s",
                StateContext.class.getSimpleName(), other, XatkitSession.class.getSimpleName(),
                other.getClass().getSimpleName());
        XatkitSession otherSesion = (XatkitSession) other;
        otherSesion.session.entrySet().forEach(v -> {
            if (session.containsKey(v.getKey())) {
                if (v.getValue() instanceof Map) {
                    /*
                     * We need to copy the map of the other session to make sure they are independent. The merge also
                     * copies individual map entries in case the map was already stored in the current session.
                     * TODO support such merge for other collection types
                     */
                    Map sessionMap = (Map) session.get(v.getKey());
                    if (isNull(sessionMap)) {
                        sessionMap = new HashMap((Map) v.getValue());
                        session.put(v.getKey(), sessionMap);
                    } else {
                        sessionMap.putAll((Map) v.getValue());
                    }
                } else {
                    Log.warn("Overriding session variable {0} (old_value={1}, new_value={2}, sessionId={3})",
                            v.getKey(), session.get(v.getKey()), v.getValue(), this.contextId);
                    session.put(v.getKey(), v.getValue());
                }
            } else {
                session.put(v.getKey(), v.getValue());
            }
        });
        /*
         * Merging two sessions also implies to merge their RuntimeContexts. This ensures that a single call to
         * XatkitSession#merge merges all the information stored in the sessions.
         */
        this.runtimeContexts.merge(otherSesion.getRuntimeContexts());
    }

    /**
     * Returns a {@link String} representation of the session.
     *
     * @return a {@link String} representation of the session
     */
    @Override
    public String toString() {
        return MessageFormat.format("Session={0}", this.contextId);
    }
}

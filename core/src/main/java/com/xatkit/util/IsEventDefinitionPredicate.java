package com.xatkit.util;

import com.xatkit.execution.StateContext;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.EventInstance;
import lombok.Getter;

import java.text.MessageFormat;
import java.util.function.Predicate;

import static java.util.Objects.nonNull;

public class IsEventDefinitionPredicate implements Predicate<StateContext> {

    @Getter
    private EventDefinition eventDefinition;

    public IsEventDefinitionPredicate(EventDefinition eventDefinition) {
        this.eventDefinition = eventDefinition;
    }

    @Override
    public boolean test(StateContext stateContext) {
        EventInstance eventInstance = stateContext.getEventInstance();
        if(nonNull(eventInstance)) {
            if(nonNull(eventInstance.getDefinition())) {
                /*
                 * TODO check equals works fine for EventDefinitions.
                 */
                return eventInstance.getDefinition().equals(this.eventDefinition);
            } else {
                throw new IllegalStateException(MessageFormat.format("The current {0}'s definition is null",
                        EventInstance.class.getSimpleName()));
            }
        }
        return false;
    }
}

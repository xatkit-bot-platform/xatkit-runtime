package com.xatkit.dsl;

import com.xatkit.intent.Context;
import com.xatkit.intent.EventDefinition;
import lombok.val;
import org.junit.Test;

import static com.xatkit.dsl.DSL.event;
import static org.assertj.core.api.Assertions.assertThat;

public class EventTest {

    @Test
    public void eventWithoutContext() {
        val event = event("Event");
        EventDefinition base = event.getEventDefinition();
        assertThat(base.getName()).isEqualTo("Event");
        assertThat(base.getOutContexts()).isEmpty();
    }

    @Test
    public void eventWithContext() {
        val event = event("Event")
            .parameter("p1")
            .parameter("p2")
            .parameter("p3")
            .parameter("p4");

        EventDefinition base = event.getEventDefinition();
        assertThat(base.getName()).isEqualTo("Event");
        assertThat(base.getOutContexts()).hasSize(1);
        Context context = base.getOutContexts().get(0);
        assertThat(context.getName()).isEqualTo("XATKITCONTEXT");
        assertThat(context.getParameters()).hasSize(4);
        assertThat(context.getParameters()).anyMatch(p -> p.getName().equals("p1"));
        assertThat(context.getParameters()).anyMatch(p -> p.getName().equals("p2"));
        assertThat(context.getParameters()).anyMatch(p -> p.getName().equals("p3"));
        assertThat(context.getParameters()).anyMatch(p -> p.getName().equals("p4"));
    }
}

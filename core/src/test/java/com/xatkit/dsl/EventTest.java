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
                .context("Context1")
                    .lifespan(10)
                    .parameter("p1")
                    .parameter("p2")
                .context("Context2")
                    .parameter("p3")
                    .parameter("p4");

        EventDefinition base = event.getEventDefinition();
        assertThat(base.getName()).isEqualTo("Event");
        assertThat(base.getOutContexts()).hasSize(2);
        Context context1 = base.getOutContexts().get(0);
        assertThat(context1.getName()).isEqualTo("Context1");
        assertThat(context1.getLifeSpan()).isEqualTo(10);
        assertThat(context1.getParameters()).hasSize(2);
        assertThat(context1.getParameters()).anyMatch(p -> p.getName().equals("p1"));
        assertThat(context1.getParameters()).anyMatch(p -> p.getName().equals("p2"));
        Context context2 = base.getOutContexts().get(1);
        assertThat(context2.getName()).isEqualTo("Context2");
        assertThat(context2.getLifeSpan()).isEqualTo(5);
        assertThat(context2.getParameters()).hasSize(2);
        assertThat(context2.getParameters()).anyMatch(p -> p.getName().equals("p3"));
        assertThat(context2.getParameters()).anyMatch(p -> p.getName().equals("p4"));

    }
}

package com.xatkit.dsl;

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
        assertThat(base.getParameters()).isEmpty();
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
        assertThat(base.getParameters()).hasSize(4);
        assertThat(base.getParameter("p1")).isNotNull();
        assertThat(base.getParameter("p2")).isNotNull();
        assertThat(base.getParameter("p3")).isNotNull();
        assertThat(base.getParameter("p4")).isNotNull();
    }
}

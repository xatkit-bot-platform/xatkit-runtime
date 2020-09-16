package com.xatkit.dsl;

import com.xatkit.intent.Context;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.IntentDefinition;
import lombok.val;
import org.junit.Test;

import static com.xatkit.dsl.DSL.city;
import static com.xatkit.dsl.DSL.intent;
import static org.assertj.core.api.Assertions.assertThat;

public class IntentTest {

    @Test
    public void intentWithTrainingSentences() {
        val intent = intent("Greetings")
                .trainingSentence("Greetings")
                .trainingSentence("Hi")
                .trainingSentence("Hello");

        IntentDefinition base = intent.getIntentDefinition();
        assertThat(base.getName()).isEqualTo("Greetings");
        assertThat(base.getTrainingSentences()).hasSize(3);
        assertThat(base.getTrainingSentences()).contains("Greetings", "Hi", "Hello");
    }

    @Test
    public void intentWithParameter() {
        val intent = intent("LiveIn")
                .trainingSentence("I live in Barcelona")
                .parameter("cityName")
                .fromFragment("Barcelona")
                .entity(city());

        IntentDefinition base = intent.getIntentDefinition();
        assertThat(base.getTrainingSentences()).hasSize(1);
        assertThat(base.getTrainingSentences()).contains("I live in Barcelona");
        assertThat(base.getOutContexts()).hasSize(1);
        Context context = base.getOutContexts().get(0);
        assertThat(context.getName()).isEqualTo("XATKITCONTEXT");
        assertThat(context.getParameters()).hasSize(1);
        ContextParameter parameter1 = context.getParameters().get(0);
        assertThat(parameter1.getName()).isEqualTo("cityName");
        assertThat(parameter1.getTextFragment()).isEqualTo("Barcelona");
        assertThat(parameter1.getEntity().getReferredEntity().getName()).isEqualTo(city().getReferredEntity().getName());
    }
}

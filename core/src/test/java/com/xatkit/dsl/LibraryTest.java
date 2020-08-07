package com.xatkit.dsl;

import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.Library;
import com.xatkit.intent.MappingEntityDefinition;
import lombok.val;
import org.junit.Test;

import static com.xatkit.dsl.DSL.composite;
import static com.xatkit.dsl.DSL.intent;
import static com.xatkit.dsl.DSL.library;
import static com.xatkit.dsl.DSL.mapping;
import static org.assertj.core.api.Assertions.assertThat;

public class LibraryTest {

    @Test
    public void libraryWithIntents() {
        val library = library("Library")
                .intent(intent("Greetings")
                        .trainingSentence("Hello")
                        .trainingSentence("Hi")
                )
                .intent(intent("Bye")
                        .trainingSentence("Bye")
                );

        Library base = library.getLibrary();
        assertThat(base.getName()).isEqualTo("Library");
        assertThat(base.getEventDefinitions()).hasSize(2);
        assertThat(base.getEventDefinitions()).anyMatch(e -> e.getName().equals("Greetings"));
        assertThat(base.getEventDefinitions()).anyMatch(e -> e.getName().equals("Bye"));
    }

    @Test
    public void libraryWithEntities() {
        val library = library("Library")
                .entity(mapping("Mapping")
                    .entry()
                        .value("Value1").synonym("Synonym1")
                )
                .entity(composite("Composite")
                    .entry()
                        .text("text1").text("text2")
                );

        Library base = library.getLibrary();
        assertThat(base.getName()).isEqualTo("Library");
        assertThat(base.getCustomEntities()).hasSize(2);
        assertThat(base.getCustomEntities()).anyMatch(e -> (e instanceof MappingEntityDefinition) && e.getName().equals("Mapping"));
        assertThat(base.getCustomEntities()).anyMatch(e -> (e instanceof CompositeEntityDefinition) && e.getName().equals("Composite"));
    }
}

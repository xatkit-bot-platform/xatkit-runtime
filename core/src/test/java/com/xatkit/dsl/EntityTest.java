package com.xatkit.dsl;

import com.xatkit.dsl.entity.CustomEntityDefinitionProvider;
import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.EntityDefinitionReference;
import com.xatkit.intent.EntityTextFragment;
import com.xatkit.intent.LiteralTextFragment;
import com.xatkit.intent.MappingEntityDefinition;
import com.xatkit.intent.TextFragment;
import lombok.val;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static com.xatkit.dsl.DSL.*;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class EntityTest {

    @Test
    public void mappingTest() {
        val mapping = mapping("Category")
                .entry()
                .value("DSL")
                .synonym("Language")
                .synonym("Internal DSL")
                .entry()
                .value("UML")
                .synonym("Unified Modeling Language")
                .entry()
                .value("Tool");

        assertThat(mapping.getEntity()).isInstanceOf(MappingEntityDefinition.class);
        MappingEntityDefinition base = (MappingEntityDefinition) mapping.getEntity();
        assertThat(base.getName()).isEqualTo("Category");
        assertThat(base.getEntries()).hasSize(3);
        assertThat(base.getEntries()).anyMatch(e ->
                e.getReferenceValue().equals("DSL") &&
                        e.getSynonyms().contains("Language") &&
                        e.getSynonyms().contains("Internal DSL")
        );
        assertThat(base.getEntries()).anyMatch(e ->
                e.getReferenceValue().equals("UML") &&
                        e.getSynonyms().contains("Unified Modeling Language")
        );
        assertThat(base.getEntries()).anyMatch(e ->
                e.getReferenceValue().equals("Tool") &&
                        e.getSynonyms().isEmpty()
        );
    }

    @Ignore
    @Test
    public void emptyMappingTest() {
        val mapping = mapping("Category");
        assertThat(mapping).isNotInstanceOf(CustomEntityDefinitionProvider.class);
        /*
         * We have a way to make mapping instanceof CustomEntityDefinitionProvider returns false, is it worth it?
         */
    }

    @Test
    public void compositeTest() {
        /*
         * A composite entity to define a place. A place can be either a city + country, or a city + county + country.
         */
        val composite = composite("Place")
                .entry()
                .entity(city()).text(", ").entity(country())
                .entry()
                .entity(city()).text(", ").entity(countyUs()).text("; ").entity(country());

        assertThat(composite.getEntity()).isInstanceOf(CompositeEntityDefinition.class);
        CompositeEntityDefinition base = (CompositeEntityDefinition) composite.getEntity();
        assertThat(base.getName()).isEqualTo("Place");
        assertThat(base.getEntries()).hasSize(2);
        assertThat(base.getEntries()).anyMatch(e ->
                checkFragmentListContains(e.getFragments(), city()) &&
                        checkFragmentListContains(e.getFragments(), ", ") &&
                        checkFragmentListContains(e.getFragments(), country())
        );
        assertThat(base.getEntries()).anyMatch(e ->
                checkFragmentListContains(e.getFragments(), city()) &&
                        checkFragmentListContains(e.getFragments(), ", ") &&
                        checkFragmentListContains(e.getFragments(), countyUs()) &&
                        checkFragmentListContains(e.getFragments(), "; ") &&
                        checkFragmentListContains(e.getFragments(), country())
        );
    }

    @Ignore
    @Test
    public void emptyCompositeEntityTest() {
        val composite = composite("Place");
        assertThat(composite).isNotInstanceOf(CustomEntityDefinitionProvider.class);
        /*
         * We have a way to make mapping instanceof CustomEntityDefinitionProvider returns false, is it worth it?
         */
    }

    private boolean checkFragmentListContains(List<TextFragment> fragments, String literal) {
        return fragments.stream().anyMatch(t ->
                (t instanceof LiteralTextFragment) &&
                        ((LiteralTextFragment) t).getValue().equals(literal)
        );
    }

    private boolean checkFragmentListContains(List<TextFragment> fragments, EntityDefinitionReference reference) {
        return fragments.stream().anyMatch(t ->
                (t instanceof EntityTextFragment) &&
                        /*
                         * Use name equality instead of object equality because of EMF equals() default implementation.
                         */
                        ((EntityTextFragment) t).getEntityReference().getReferredEntity().getName().equals(reference.getReferredEntity().getName())
        );
    }
}

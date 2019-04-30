package edu.uoc.som.jarvis.test.util;

import edu.uoc.som.jarvis.intent.*;

import java.text.MessageFormat;

/**
 * A factory that creates <i>Intent</i>, <i>Platform</i>, and <i>Execution</i> model elements to use in tests.
 * <p>
 * This class gathers element creation methods that are reused in multiple test cases. See the methods' documentation
 * to have more information on the created elements.
 * <p>
 * <b>Note</b>: this class does not cache any instance, and calling the same method multiple times will create
 * different objects.
 */
public class ElementFactory {

    /**
     * Creates an {@link EntityDefinitionReference} for the provided {@code entityDefinition}.
     * <p>
     * This method supports both {@link CustomEntityDefinition} and {@link BaseEntityDefinition} instances.
     *
     * @param entityDefinition the {@link EntityDefinition} to create a reference to
     * @return the created {@link EntityDefinitionReference}
     * @see #createBaseEntityDefinitionReference(EntityType)
     */
    public static EntityDefinitionReference createEntityDefinitionReference(EntityDefinition entityDefinition) {
        EntityDefinitionReference reference;
        if (entityDefinition instanceof BaseEntityDefinition) {
            reference = IntentFactory.eINSTANCE.createBaseEntityDefinitionReference();
            ((BaseEntityDefinitionReference) reference).setBaseEntity((BaseEntityDefinition) entityDefinition);
        }
        if (entityDefinition instanceof CustomEntityDefinition) {
            reference = IntentFactory.eINSTANCE.createCustomEntityDefinitionReference();
            ((CustomEntityDefinitionReference) reference).setCustomEntity((CustomEntityDefinition) entityDefinition);
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Cannot create a reference for the provided {0} " +
                    "{1}", EntityDefinition.class.getSimpleName(), entityDefinition));
        }
        return reference;
    }

    /**
     * Creates an {@link EntityDefinitionReference} for the provided {@link entityType}.
     * <p>
     * This method creates a new {@link BaseEntityDefinition} and initializes it with the provided {@code entityType}.
     *
     * @param entityType the {@link EntityType} to create a reference to
     * @return the created {@link BaseEntityDefinition}
     * @see #createEntityDefinitionReference(EntityDefinition)
     */
    public static EntityDefinitionReference createBaseEntityDefinitionReference(EntityType entityType) {
        BaseEntityDefinition entityDefinition = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        entityDefinition.setEntityType(entityType);
        BaseEntityDefinitionReference reference = IntentFactory.eINSTANCE.createBaseEntityDefinitionReference();
        reference.setBaseEntity(entityDefinition);
        return reference;
    }

    /**
     * Creates an {@link IntentDefinition} with an empty {@code outContext} list.
     *
     * @return the created {@link IntentDefinition}
     */
    public static IntentDefinition testIntentDefinitionNoOutContext() {
        IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        intentDefinition.setName("TestIntentDefinitionNoOutContext");
        intentDefinition.getTrainingSentences().add("this is a test");
        return intentDefinition;
    }

    /**
     * Creates a {@link MappingEntityDefinition}.
     * <p>
     * The created {@link MappingEntityDefinition} is named {@code Class}, and is initialized with two entries:
     * {@code Person -> People, Fellow} and {@code Order -> Command}.
     *
     * @return the created {@link MappingEntityDefinition}
     */
    public static MappingEntityDefinition testMappingEntityDefinition() {
        MappingEntityDefinition entityDefinition = IntentFactory.eINSTANCE.createMappingEntityDefinition();
        entityDefinition.setName("Class");
        MappingEntityDefinitionEntry entry1 = IntentFactory.eINSTANCE.createMappingEntityDefinitionEntry();
        entry1.setReferenceValue("Person");
        entry1.getSynonyms().add("People");
        entry1.getSynonyms().add("Fellow");
        MappingEntityDefinitionEntry entry2 = IntentFactory.eINSTANCE.createMappingEntityDefinitionEntry();
        entry2.setReferenceValue("Order");
        entry2.getSynonyms().add("Command");
        entityDefinition.getEntries().add(entry1);
        entityDefinition.getEntries().add(entry2);
        return entityDefinition;
    }

    /**
     * Creates a {@link CompositeEntityDefinition}.
     * <p>
     * The created {@link CompositeEntityDefinition} is named {@code Composite}, and is initialized with the
     * following entry: {@code @Class with @age}, where {@code @Class} is an instance of the mapping returned by
     * {@link #testMappingEntityDefinition()}, and {@code @age} is a system {@link EntityType}.
     * <p>
     * Note that the referenced {@link MappingEntityDefinition} is not accessible through this method.
     *
     * @return the created {@link CompositeEntityDefinition}
     */
    public static CompositeEntityDefinition testCompositeEntityDefinition() {
        MappingEntityDefinition mappingEntityDefinition = testMappingEntityDefinition();

        CompositeEntityDefinition composite = IntentFactory.eINSTANCE.createCompositeEntityDefinition();
        composite.setName("Composite");

        // Creates the entry "@Class:Class with @sys.age:age"
        CompositeEntityDefinitionEntry compositeEntry1 = IntentFactory.eINSTANCE.createCompositeEntityDefinitionEntry();
        EntityTextFragment entityTextFragment1 = IntentFactory.eINSTANCE.createEntityTextFragment();
        EntityDefinitionReference entityTextFragment1Reference = IntentFactory.eINSTANCE
                .createCustomEntityDefinitionReference();
        ((CustomEntityDefinitionReference) entityTextFragment1Reference).setCustomEntity
                (mappingEntityDefinition);
        entityTextFragment1.setEntityReference(entityTextFragment1Reference);
        LiteralTextFragment literalTextFragment2 = IntentFactory.eINSTANCE.createLiteralTextFragment();
        literalTextFragment2.setValue(" with ");
        EntityTextFragment entityTextFragment2 = IntentFactory.eINSTANCE.createEntityTextFragment();
        EntityDefinitionReference entityTextFragment2Reference = IntentFactory.eINSTANCE
                .createBaseEntityDefinitionReference();
        BaseEntityDefinition baseEntityDefinition = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        baseEntityDefinition.setEntityType(EntityType.AGE);
        ((BaseEntityDefinitionReference) entityTextFragment2Reference).setBaseEntity(baseEntityDefinition);
        entityTextFragment2.setEntityReference(entityTextFragment2Reference);
        compositeEntry1.getFragments().add(entityTextFragment1);
        compositeEntry1.getFragments().add(literalTextFragment2);
        compositeEntry1.getFragments().add(entityTextFragment2);
        composite.getEntries().add(compositeEntry1);
        return composite;
    }
}

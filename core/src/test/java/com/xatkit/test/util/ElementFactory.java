package com.xatkit.test.util;


import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.BaseEntityDefinitionReference;
import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.CompositeEntityDefinitionEntry;
import com.xatkit.intent.CustomEntityDefinition;
import com.xatkit.intent.CustomEntityDefinitionReference;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.EntityDefinitionReference;
import com.xatkit.intent.EntityTextFragment;
import com.xatkit.intent.EntityType;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.EventInstance;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.LiteralTextFragment;
import com.xatkit.intent.MappingEntityDefinition;
import com.xatkit.intent.MappingEntityDefinitionEntry;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.platform.ActionDefinition;
import com.xatkit.util.EMFUtils;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.TypesFactory;
import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.XbaseFactory;

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

    public static XMemberFeatureCall createXMemberFeatureCall(ActionDefinition actionDefinition) {
        XMemberFeatureCall actionCall = XbaseFactory.eINSTANCE.createXMemberFeatureCall();
        JvmGenericType jvmGenericType = TypesFactory.eINSTANCE.createJvmGenericType();
        jvmGenericType.setSimpleName(EMFUtils.getName(actionDefinition.eContainer()));
        JvmOperation jvmOperation = TypesFactory.eINSTANCE.createJvmOperation();
        jvmOperation.setSimpleName(actionDefinition.getName());
        jvmGenericType.getMembers().add(jvmOperation);
        actionCall.setFeature(jvmOperation);
        XFeatureCall feature = XbaseFactory.eINSTANCE.createXFeatureCall();
        feature.setFeature(jvmGenericType);
        actionCall.setMemberCallTarget(feature);
        return actionCall;
    }

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
     * Creates an {@link EntityDefinitionReference} for the provided {@link EntityType}.
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
    public static IntentDefinition createIntentDefinitionNoOutContext() {
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
    public static MappingEntityDefinition createMappingEntityDefinition() {
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
     * {@link #createMappingEntityDefinition()}, and {@code @age} is a system {@link EntityType}.
     * <p>
     * Note that the referenced {@link MappingEntityDefinition} is not accessible through this method.
     *
     * @return the created {@link CompositeEntityDefinition}
     */
    public static CompositeEntityDefinition createCompositeEntityDefinition() {
        MappingEntityDefinition mappingEntityDefinition = createMappingEntityDefinition();

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

    /**
     * Creates a new {@link IntentDefinition} following the provided {@code parentIntentDefinition}.
     * <p>
     * This method modifies the provided {@code parentIntentDefinition} and sets the created {@link IntentDefinition}
     * in its {@code followedBy} list.
     *
     * @param parentIntentDefinition the {@link IntentDefinition} to create a follow-up intent for
     * @return the created {@link IntentDefinition}
     */
    public static IntentDefinition createFollowUpIntent(IntentDefinition parentIntentDefinition) {
        IntentDefinition followUpIntent = IntentFactory.eINSTANCE.createIntentDefinition();
        followUpIntent.setName("TestRegisteredFollowUp");
        followUpIntent.getTrainingSentences().add("test followUp");
        followUpIntent.setFollows(parentIntentDefinition);
        return followUpIntent;
    }

    /**
     * Creates an {@link EventDefinition} with the name {@code Event}.
     *
     * @return the created {@link EventDefinition}
     */
    public static EventDefinition createEventDefinition() {
        EventDefinition eventDefinition = IntentFactory.eINSTANCE.createEventDefinition();
        eventDefinition.setName("Event");
        return eventDefinition;
    }

    /**
     * Creates a {@link RecognizedIntent} with recognition confidence {@code 0.5f} and matched input {@code test}.
     * <p>
     * The created {@link RecognizedIntent#getDefinition()} value is the {@link IntentDefinition} returned by
     * {@link #createIntentDefinitionNoOutContext()}.
     *
     * @return the created {@link RecognizedIntent}
     */
    public static RecognizedIntent createRecognizedIntent() {
        IntentDefinition intentDefinition = createIntentDefinitionNoOutContext();
        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        recognizedIntent.setDefinition(intentDefinition);
        recognizedIntent.setMatchedInput("test");
        recognizedIntent.setRecognitionConfidence(0.5f);
        return recognizedIntent;
    }

    /**
     * Creates an {@link EventInstance}.
     * <p>
     * The created {@link EventInstance#getDefinition()} value is the {@link EventDefinition} returned by
     * {@link #createEventDefinition()}.
     *
     * @return the created {@link EventInstance}
     */
    public static EventInstance createEventInstance() {
        EventDefinition eventDefinition = createEventDefinition();
        EventInstance eventInstance = IntentFactory.eINSTANCE.createEventInstance();
        eventInstance.setDefinition(eventDefinition);
        return eventInstance;
    }
}

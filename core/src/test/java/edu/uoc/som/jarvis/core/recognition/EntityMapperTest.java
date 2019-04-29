package edu.uoc.som.jarvis.core.recognition;

import edu.uoc.som.jarvis.AbstractJarvisTest;
import edu.uoc.som.jarvis.core.JarvisException;
import edu.uoc.som.jarvis.intent.*;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityMapperTest extends AbstractJarvisTest {

    private EntityMapper mapper;

    private static String VALID_ENTITY_STRING = "city";

    private static EntityType VALID_ENTITY_TYPE;

    private static EntityDefinition VALID_ENTITY;

    private static CustomEntityDefinition VALID_CUSTOM_ENTITY_DEFINITION;

    private static String CONCRETE_VALUE = "concrete";

    private static String FALLBACK_VALUE = "fallback";

    @BeforeClass
    public static void setUpBeforeClass() {
        VALID_ENTITY_TYPE = EntityType.get(VALID_ENTITY_STRING);
        VALID_ENTITY = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        VALID_CUSTOM_ENTITY_DEFINITION = IntentFactory.eINSTANCE.createMappingEntityDefinition();
        VALID_CUSTOM_ENTITY_DEFINITION.setName("custom");
        ((BaseEntityDefinition) VALID_ENTITY).setEntityType(VALID_ENTITY_TYPE);
    }

    @Before
    public void setUp() {
        mapper = new EntityMapper();
    }

    @Test
    public void constructValid() {
        mapper = new EntityMapper();
        assertThat(mapper.entities).as("Mapping is empty").isEmpty();
    }

    @Test(expected = NullPointerException.class)
    public void addEntityMappingNullAbstractEntity() {
        mapper.addEntityMapping((EntityType)null, CONCRETE_VALUE);
    }

    @Test(expected = NullPointerException.class)
    public void addEntityMappingNullConcreteEntity() {
        mapper.addEntityMapping(VALID_ENTITY_TYPE, null);
    }

    @Test
    public void addEntityMappingValid() {
        mapper.addEntityMapping(VALID_ENTITY_TYPE, CONCRETE_VALUE);
        assertThat(mapper.entities).as("Mapping contains an entry for the abstract entity").containsKey(VALID_ENTITY_STRING);
        assertThat(mapper.entities.get(VALID_ENTITY_STRING)).as("Mapping contains the provided concrete value mapped to the " +
                "abstract entity").isEqualTo(CONCRETE_VALUE);
    }

    @Test
    public void addCustomEntityMappingValid() {
        mapper.addEntityMapping(VALID_CUSTOM_ENTITY_DEFINITION, CONCRETE_VALUE);
        assertThat(mapper.entities).as("Mapping contains an entry for the custom entity definition").containsKeys(VALID_CUSTOM_ENTITY_DEFINITION.getName());
        assertThat(mapper.entities.get(VALID_CUSTOM_ENTITY_DEFINITION.getName())).as("Mapping contains the provided " +
                "concrete value mapped to the custom entity definition").isEqualTo(CONCRETE_VALUE);
    }

    @Test(expected = NullPointerException.class)
    public void setFallbackEntityMappingNullConcreteEntity() {
        mapper.setFallbackEntityMapping(null);
    }

    @Test
    public void setFallbackEntityMappingValid() {
        mapper.setFallbackEntityMapping(CONCRETE_VALUE);
        assertThat(mapper.entities).as("Mapping contains a fallback entry").containsKey(EntityMapper
                .FALLBACK_ENTITY_KEY);
        assertThat(mapper.entities.get(EntityMapper.FALLBACK_ENTITY_KEY)).as("Mapping contains the provided concrete " +
                "value mapped to the fallback abstract entity").isEqualTo(CONCRETE_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setFallbackEntityMappingAlreadyRegisteredFallback() {
        mapper.setFallbackEntityMapping(CONCRETE_VALUE);
        mapper.setFallbackEntityMapping(CONCRETE_VALUE + "2");
    }

    @Test(expected = NullPointerException.class)
    public void getStringMappingForNullAbstractEntity() {
        mapper.getMappingFor((EntityType) null);
    }

    @Test
    public void getStringMappingForRegisteredAbstractEntity() {
        mapper.addEntityMapping(VALID_ENTITY_TYPE, CONCRETE_VALUE);
        String concrete = mapper.getMappingFor(VALID_ENTITY_TYPE);
        assertThat(concrete).as("Mapped value is not null").isNotNull();
        assertThat(concrete).as("Mapped value is valid").isEqualTo(CONCRETE_VALUE);
    }

    @Test
    public void getStringMappingForRegisteredAbstractEntityFallback() {
        /*
         * Check that the fallback is not returned if there is a direct mapping.
         */
        mapper.addEntityMapping(VALID_ENTITY_TYPE, CONCRETE_VALUE);
        mapper.setFallbackEntityMapping(FALLBACK_VALUE);
        String concrete = mapper.getMappingFor(VALID_ENTITY_TYPE);
        assertThat(concrete).as("Mapped value is not null").isNotNull();
        assertThat(concrete).as("Mapped value is valid and not fallback").isEqualTo(CONCRETE_VALUE);
    }

    @Test
    public void getStringMappingForNotRegisteredAbstractEntityNoFallback() {
        String concrete = mapper.getMappingFor(VALID_ENTITY_TYPE);
        assertThat(concrete).as("Mapped value is null").isNull();
    }

    @Test
    public void getStringMappingForNotRegisteredAbstractEntityFallback() {
        mapper.setFallbackEntityMapping(FALLBACK_VALUE);
        String concrete = mapper.getMappingFor(VALID_ENTITY_TYPE);
        assertThat(concrete).as("Mapped value is not null").isNotNull();
        assertThat(concrete).as("Mapped value is fallback").isEqualTo(FALLBACK_VALUE);
    }

    @Test(expected = NullPointerException.class)
    public void getEntityMappingForNullAbstractEntity() {
        mapper.getMappingFor((EntityDefinition) null);
    }

    /*
     * No test for null EntityType, null enums are set with their default value in Ecore.
     */

    @Test(expected = JarvisException.class)
    public void getEntityMappingForNotBaseEntityDefinition() {
        EntityDefinition entityDefinition = new EntityDefinition() {
            @Override
            public EClass eClass() {
                return null;
            }

            @Override
            public Resource eResource() {
                return null;
            }

            @Override
            public EObject eContainer() {
                return null;
            }

            @Override
            public EStructuralFeature eContainingFeature() {
                return null;
            }

            @Override
            public EReference eContainmentFeature() {
                return null;
            }

            @Override
            public EList<EObject> eContents() {
                return null;
            }

            @Override
            public TreeIterator<EObject> eAllContents() {
                return null;
            }

            @Override
            public boolean eIsProxy() {
                return false;
            }

            @Override
            public EList<EObject> eCrossReferences() {
                return null;
            }

            @Override
            public Object eGet(EStructuralFeature eStructuralFeature) {
                return null;
            }

            @Override
            public Object eGet(EStructuralFeature eStructuralFeature, boolean b) {
                return null;
            }

            @Override
            public void eSet(EStructuralFeature eStructuralFeature, Object o) {

            }

            @Override
            public boolean eIsSet(EStructuralFeature eStructuralFeature) {
                return false;
            }

            @Override
            public void eUnset(EStructuralFeature eStructuralFeature) {

            }

            @Override
            public Object eInvoke(EOperation eOperation, EList<?> eList) throws InvocationTargetException {
                return null;
            }

            @Override
            public EList<Adapter> eAdapters() {
                return null;
            }

            @Override
            public boolean eDeliver() {
                return false;
            }

            @Override
            public void eSetDeliver(boolean b) {

            }

            @Override
            public void eNotify(Notification notification) {

            }

            @Override
            public String getName() {
                return null;
            }
        };
        mapper.getMappingFor(entityDefinition);
    }

    @Test
    public void getEntityMappingForRegisteredAbstractEntity() {
        mapper.addEntityMapping(VALID_ENTITY_TYPE, CONCRETE_VALUE);
        String concrete = mapper.getMappingFor(VALID_ENTITY);
        assertThat(concrete).as("Mapped value is not null").isNotNull();
        assertThat(concrete).as("Mapped value is valid").isEqualTo(CONCRETE_VALUE);
    }

    /*
     * IntelliJ marks this test as duplicated, but it does not call the same signature of #getMappingFor
     */
    @Test
    public void getEntityMappingForRegisteredAbstractEntityFallback() {
        /*
         * Check that the fallback is not returned if there is a direct mapping
         */
        mapper.addEntityMapping(VALID_ENTITY_TYPE, CONCRETE_VALUE);
        mapper.setFallbackEntityMapping(FALLBACK_VALUE);
        String concrete = mapper.getMappingFor(VALID_ENTITY);
        assertThat(concrete).as("Mapped value is not null").isNotNull();
        assertThat(concrete).as("Mapped value is valid and not fallback").isEqualTo(CONCRETE_VALUE);
    }

    @Test
    public void getEntityMappingForNotRegisteredAbstractEntityNoFallback() {
        String concrete = mapper.getMappingFor(VALID_ENTITY);
        assertThat(concrete).as("Mapped value is null").isNull();
    }

    @Test
    public void getEntityMappingForNotRegisteredAbstractEntityFallback() {
        mapper.setFallbackEntityMapping(FALLBACK_VALUE);
        String concrete = mapper.getMappingFor(VALID_ENTITY);
        assertThat(concrete).as("Mapped value is not null").isNotNull();
        assertThat(concrete).as("Mapped value is fallback").isEqualTo(FALLBACK_VALUE);
    }

    @Test
    public void getEntityMappingForRegisteredCustomEntityDefinition() {
        mapper.addCustomEntityMapping(VALID_CUSTOM_ENTITY_DEFINITION, CONCRETE_VALUE);
        String concrete = mapper.getMappingFor(VALID_CUSTOM_ENTITY_DEFINITION);
        assertThat(concrete).as("Correct mapped value").isEqualTo(CONCRETE_VALUE);
    }

    @Test
    public void getEntityMappingForNotRegisteredCustomEntityDefinition() {
        String concrete = mapper.getMappingFor(VALID_CUSTOM_ENTITY_DEFINITION);
        assertThat(concrete).as("Mapped value is null").isNull();
    }

}

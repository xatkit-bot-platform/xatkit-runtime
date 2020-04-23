package com.xatkit.core.recognition.dialogflow.mapper;

import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.EntityType;
import com.xatkit.intent.IntentFactory;
import com.xatkit.test.util.TestBotExecutionModel;
import com.xatkit.test.util.TestModelLoader;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class DialogFlowEntityReferenceMapperTest {

    private static TestBotExecutionModel testBotExecutionModel;

    @BeforeClass
    public static void setUpBeforeClass() throws ConfigurationException {
        testBotExecutionModel = TestModelLoader.loadTestBot();
    }

    private DialogFlowEntityReferenceMapper mapper;

    @Test
    public void getMappingForSystemEntity() {
        mapper = new DialogFlowEntityReferenceMapper();
        BaseEntityDefinition entityDefinition = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        entityDefinition.setEntityType(EntityType.ANY);
        String reference = mapper.getMappingFor(entityDefinition);
        assertThat(reference).isEqualTo("@sys.any");
    }

    @Test
    public void getMappingForMappingEntity() {
        mapper = new DialogFlowEntityReferenceMapper();
        String reference = mapper.getMappingFor(testBotExecutionModel.getMappingEntity());
        assertThat(reference).isEqualTo("@" + testBotExecutionModel.getMappingEntity().getName());
    }

    @Test
    public void getMappingForCompositeEntity() {
        mapper = new DialogFlowEntityReferenceMapper();
        String reference = mapper.getMappingFor(testBotExecutionModel.getCompositeEntity());
        assertThat(reference).isEqualTo("@" + testBotExecutionModel.getCompositeEntity().getName());
    }

}

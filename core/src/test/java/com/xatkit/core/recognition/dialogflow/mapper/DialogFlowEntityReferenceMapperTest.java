package com.xatkit.core.recognition.dialogflow.mapper;

import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.EntityType;
import com.xatkit.intent.IntentFactory;
import com.xatkit.test.bot.IntentProviderTestBot;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class DialogFlowEntityReferenceMapperTest {

    private static IntentProviderTestBot intentProviderTestBot;

    @BeforeClass
    public static void setUpBeforeClass() {
        intentProviderTestBot = new IntentProviderTestBot();
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
        String reference = mapper.getMappingFor(intentProviderTestBot.getMappingEntity());
        assertThat(reference).isEqualTo("@" + intentProviderTestBot.getMappingEntity().getName());
    }

    @Test
    public void getMappingForCompositeEntity() {
        mapper = new DialogFlowEntityReferenceMapper();
        String reference = mapper.getMappingFor(intentProviderTestBot.getCompositeEntity());
        assertThat(reference).isEqualTo("@" + intentProviderTestBot.getCompositeEntity().getName());
    }

}

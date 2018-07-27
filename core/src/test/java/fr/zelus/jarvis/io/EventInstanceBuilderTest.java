package fr.zelus.jarvis.io;

import fr.zelus.jarvis.AbstractJarvisTest;
import fr.zelus.jarvis.core.EventDefinitionRegistry;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.intent.*;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EventInstanceBuilderTest extends AbstractJarvisTest {

    private EventInstanceBuilder builder;

    private EventDefinitionRegistry registry;

    @Before
    public void setUp() {
        registry = new EventDefinitionRegistry();
    }

    @After
    public void tearDown() {
        registry.clearEventDefinitions();
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();


    @Test(expected = NullPointerException.class)
    public void newBuilderNullRegistry() {
        builder = EventInstanceBuilder.newBuilder(null);
    }

    @Test
    public void newBuilderValidRegistry() {
        builder = EventInstanceBuilder.newBuilder(registry);
        assertThat(builder).as("Not null builder").isNotNull();
        softly.assertThat(builder.getEventDefinitionName()).as("Null EventDefinition name").isNull();
        softly.assertThat(builder.getOutContextValues()).as("Empty out context values").isEmpty();
    }

    @Test(expected = NullPointerException.class)
    public void setNullEventDefinitionName() {
        builder = EventInstanceBuilder.newBuilder(registry);
        builder.setEventDefinitionName(null);
    }

    @Test
    public void setEventDefinitionName() {
        builder = EventInstanceBuilder.newBuilder(registry);
        builder.setEventDefinitionName("EventName");
        /*
         * Setting the EventDefinition name does not throw an exception is the EventDefinition is not registered,
         * this is done when building the EventInstance.
         */
        assertThat(builder.getEventDefinitionName()).as("Valid EventDefinition name").isEqualTo("EventName");
    }

    @Test(expected = NullPointerException.class)
    public void setOutContextValueNullKey() {
        builder = EventInstanceBuilder.newBuilder(registry);
        builder.setOutContextValue(null, "value");
    }

    @Test(expected = NullPointerException.class)
    public void setOutContextValueNullValue() {
        builder = EventInstanceBuilder.newBuilder(registry);
        builder.setOutContextValue("key", null);
    }

    @Test
    public void setOutContextValue() {
        builder = EventInstanceBuilder.newBuilder(registry);
        builder.setOutContextValue("key", "value");
        softly.assertThat(builder.getOutContextValues()).as("Out context values contains the set key").containsKey
                ("key");
        softly.assertThat(builder.getOutContextValues().get("key")).as("Out context values contains the set value")
                .isEqualTo("value");
    }

    @Test(expected = JarvisException.class)
    public void buildNotRegisteredEventDefinition() {
        builder = EventInstanceBuilder.newBuilder(registry);
        builder.setEventDefinitionName("EventName").build();
    }

    @Test
    public void buildRegisteredEventDefinitionEmptyOutContext() {
        EventDefinition eventDefinition = createAndRegisterEmptyEventDefinition("EventName");
        builder = EventInstanceBuilder.newBuilder(registry);
        EventInstance eventInstance = builder.setEventDefinitionName("EventName").build();
        assertThat(eventInstance).as("Not null EventInstance").isNotNull();
        softly.assertThat(eventInstance.getDefinition()).as("Valid EventDefinition").isEqualTo(eventDefinition);
        softly.assertThat(eventInstance.getOutContextValues()).as("Empty out context values").isEmpty();
    }

    @Test(expected = JarvisException.class)
    public void buildRegisteredEventDefinitionNotRegisteredOutContext() {
        EventDefinition eventDefinition = createAndRegisterEventDefinitionWithOutContextParameter("EventName",
                "OutContext", "key");
        builder = EventInstanceBuilder.newBuilder(registry);
        builder.setEventDefinitionName("EventName").setOutContextValue("key2", "value").build();
    }

    @Test
    public void buildRegisteredEventDefinitionRegisteredOutContext() {
        EventDefinition eventDefinition = createAndRegisterEventDefinitionWithOutContextParameter("EventName",
                "OutContext", "key");
        builder = EventInstanceBuilder.newBuilder(registry);
        EventInstance eventInstance = builder.setEventDefinitionName("EventName").setOutContextValue("key", "value")
                .build();
        assertThat(eventInstance).as("Not null EventInstance").isNotNull();
        softly.assertThat(eventInstance.getDefinition()).as("Valid EventDefinition").isEqualTo(eventDefinition);
        assertThat(eventInstance.getOutContextValues()).as("Out context value list contains one element")
                .hasSize(1);
        ContextParameterValue contextParameterValue = eventInstance.getOutContextValues().get(0);
        assertThat(contextParameterValue.getContextParameter()).as("Not null ContextParameter").isNotNull();
        softly.assertThat(contextParameterValue.getContextParameter().getName()).as("Valid ContextParameter")
                .isEqualTo("key");
        softly.assertThat(contextParameterValue.getValue()).as("Valid ContextParameterValue").isEqualTo("value");
    }

    private EventDefinition createAndRegisterEmptyEventDefinition(String eventName) {
        EventDefinition eventDefinition = IntentFactory.eINSTANCE.createEventDefinition();
        eventDefinition.setName("EventName");
        registry.registerEventDefinition(eventDefinition);
        return eventDefinition;
    }

    private EventDefinition createAndRegisterEventDefinitionWithOutContextParameter(String eventName, String
            contextName, String parameterKey) {
        EventDefinition eventDefinition = createAndRegisterEmptyEventDefinition(eventName);
        Context outContext = IntentFactory.eINSTANCE.createContext();
        outContext.setName(contextName);
        ContextParameter contextParameter = IntentFactory.eINSTANCE.createContextParameter();
        contextParameter.setName(parameterKey);
        outContext.getParameters().add(contextParameter);
        eventDefinition.getOutContexts().add(outContext);
        return eventDefinition;
    }

}

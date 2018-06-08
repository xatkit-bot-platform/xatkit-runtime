package fr.zelus.jarvis.dialogflow;

import com.google.cloud.dialogflow.v2.SessionName;
import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.intent.IntentDefinition;
import fr.zelus.jarvis.intent.IntentFactory;
import fr.zelus.jarvis.intent.RecognizedIntent;
import fr.zelus.jarvis.module.Action;
import fr.zelus.jarvis.module.Module;
import fr.zelus.jarvis.module.ModuleFactory;
import fr.zelus.jarvis.orchestration.OrchestrationFactory;
import fr.zelus.jarvis.orchestration.OrchestrationLink;
import fr.zelus.jarvis.orchestration.OrchestrationModel;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DialogFlowApiTest {

    protected static String VALID_PROJECT_ID = "jarvis-fd96e";

    protected static String VALID_LANGUAGE_CODE = "en-US";

    protected static String SAMPLE_INPUT = "hello";

    protected DialogFlowApi api;

    // not tested here, only instantiated to enable IntentDefinition registration and Module retrieval
    protected static JarvisCore jarvisCore;

    @BeforeClass
    public static void setUpBeforeClass() {
        Module stubModule = ModuleFactory.eINSTANCE.createModule();
        stubModule.setName("StubJarvisModule");
        stubModule.setJarvisModulePath("fr.zelus.jarvis.stubs.StubJarvisModule");
        Action stubAction = ModuleFactory.eINSTANCE.createAction();
        stubAction.setName("StubJarvisAction");
        // No parameters, keep it simple
        stubModule.getActions().add(stubAction);
        IntentDefinition stubIntentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
        stubIntentDefinition.setName("Default Welcome Intent");
        // No parameters, keep it simple
        stubModule.getIntentDefinitions().add(stubIntentDefinition);
        OrchestrationModel orchestrationModel = OrchestrationFactory.eINSTANCE.createOrchestrationModel();
        OrchestrationLink link = OrchestrationFactory.eINSTANCE.createOrchestrationLink();
        link.setIntent(stubIntentDefinition);
        link.getActions().add(stubAction);
        orchestrationModel.getOrchestrationLinks().add(link);
        jarvisCore = new JarvisCore(VALID_PROJECT_ID, VALID_LANGUAGE_CODE, orchestrationModel);
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test(expected = NullPointerException.class)
    public void constructNullProjectId() {
        api = new DialogFlowApi(null);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullProjectIdValidLanguageCode() {
        api = new DialogFlowApi(null, "en-US");
    }

    @Test(expected = NullPointerException.class)
    public void constructNullLanguageCode() {
        api = new DialogFlowApi(VALID_PROJECT_ID, null);
    }

    @Test
    public void constructValid() {
        api = new DialogFlowApi(VALID_PROJECT_ID, VALID_LANGUAGE_CODE);
        softly.assertThat(VALID_PROJECT_ID).as("Valid project ID").isEqualTo(api.getProjectId());
        softly.assertThat(VALID_LANGUAGE_CODE).as("Valid language code").isEqualTo(api.getLanguageCode());
        softly.assertThat(api.isShutdown()).as("Not shutdown").isFalse();
    }

    @Test
    public void constructDefaultLanguageCode() {
        api = new DialogFlowApi(VALID_PROJECT_ID);
        softly.assertThat(VALID_PROJECT_ID).as("Valid project ID").isEqualTo(api.getProjectId());
        softly.assertThat(VALID_LANGUAGE_CODE).as("Valid language code").isEqualTo(api.getLanguageCode());
    }

    @Test
    public void createSessionValidApi() {
        api = new DialogFlowApi(VALID_PROJECT_ID);
        SessionName session = api.createSession();
        assertThat(session.getProject()).as("Valid session project").isEqualTo(VALID_PROJECT_ID);
    }

    @Test(expected = DialogFlowException.class)
    public void shutdownAlreadyShutdown() {
        api = new DialogFlowApi(VALID_PROJECT_ID);
        api.shutdown();
        api.shutdown();
    }

    @Test
    public void shutdown() {
        api = new DialogFlowApi(VALID_PROJECT_ID);
        SessionName session = api.createSession();
        api.shutdown();
        softly.assertThat(api.isShutdown()).as("DialogFlow API is shutdown").isTrue();
        assertThatExceptionOfType(DialogFlowException.class).isThrownBy(() -> api.getIntent("test", session))
                .withMessage("Cannot extract an Intent from the provided input, the DialogFlow API is shutdown");
        assertThatExceptionOfType(DialogFlowException.class).isThrownBy(() -> api.createSession()).withMessage
                ("Cannot create a new Session, the DialogFlow API is shutdown");
    }

    @Test
    public void getIntentValidSession() {
        api = new DialogFlowApi(VALID_PROJECT_ID);
        SessionName session = api.createSession();
        RecognizedIntent intent = api.getIntent(SAMPLE_INPUT, session);
        IntentDefinition intentDefinition = intent.getDefinition();
        assertThat(intent).as("Null Intent").isNotNull();
        assertThat(intentDefinition).as("Null Intent Definition").isNotNull();
        assertThat(intentDefinition.getName()).as("Valid Intent").isEqualTo("Default Welcome Intent");
    }

    @Test(expected = DialogFlowException.class)
    public void getIntentInvalidSession() {
        api = new DialogFlowApi("test");
        SessionName session = api.createSession();
        RecognizedIntent intent = api.getIntent(SAMPLE_INPUT, session);
    }

    @Test(expected = NullPointerException.class)
    public void getIntentNullSession() {
        api = new DialogFlowApi(VALID_PROJECT_ID);
        RecognizedIntent intent = api.getIntent(SAMPLE_INPUT, null);
    }

    @Test(expected = NullPointerException.class)
    public void getIntentNullText() {
        api = new DialogFlowApi(VALID_PROJECT_ID);
        SessionName session = api.createSession();
        RecognizedIntent intent = api.getIntent(null, session);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getIntentEmptyText() {
        api = new DialogFlowApi(VALID_PROJECT_ID);
        SessionName session = api.createSession();
        RecognizedIntent intent = api.getIntent("", session);
    }

    @Test(expected = DialogFlowException.class)
    public void getIntentUnkownText() {
        api = new DialogFlowApi(VALID_PROJECT_ID);
        SessionName session = api.createSession();
        RecognizedIntent intent = api.getIntent("azerty", session);
    }

}

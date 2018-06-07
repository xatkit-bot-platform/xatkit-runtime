package fr.zelus.jarvis.dialogflow;

import com.google.cloud.dialogflow.v2.Intent;
import com.google.cloud.dialogflow.v2.SessionName;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DialogFlowApiTest {

    protected static String VALID_PROJECT_ID = "jarvis-fd96e";

    protected static String VALID_LANGUAGE_CODE = "en-US";

    protected static String SAMPLE_INPUT = "hello";

    protected DialogFlowApi api;

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
        Intent intent = api.getIntent(SAMPLE_INPUT, session);
        assertThat(intent).as("Null Intent").isNotNull();
        assertThat(intent.getDisplayName()).as("Valid Intent").isEqualTo("Default Welcome Intent");
    }

    @Test(expected = DialogFlowException.class)
    public void getIntentInvalidSession() {
        api = new DialogFlowApi("test");
        SessionName session = api.createSession();
        Intent intent = api.getIntent(SAMPLE_INPUT, session);
    }

    @Test(expected = NullPointerException.class)
    public void getIntentNullSession() {
        api = new DialogFlowApi(VALID_PROJECT_ID);
        Intent intent = api.getIntent(SAMPLE_INPUT, null);
    }

    @Test(expected = NullPointerException.class)
    public void getIntentNullText() {
        api = new DialogFlowApi(VALID_PROJECT_ID);
        SessionName session = api.createSession();
        Intent intent = api.getIntent(null, session);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getIntentEmptyText() {
        api = new DialogFlowApi(VALID_PROJECT_ID);
        SessionName session = api.createSession();
        Intent intent = api.getIntent("", session);
    }

    @Test
    public void getIntentUnkownText() {
        api = new DialogFlowApi(VALID_PROJECT_ID);
        SessionName session = api.createSession();
        Intent intent = api.getIntent("azerty", session);
        assertThat(intent).as("Null Intent").isNotNull();
        assertThat(intent.getDisplayName()).as("Fallback Intent").isEqualTo("Default Fallback Intent");
    }

}

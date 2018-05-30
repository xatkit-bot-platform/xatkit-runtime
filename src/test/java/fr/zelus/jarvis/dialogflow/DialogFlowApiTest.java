package fr.zelus.jarvis.dialogflow;

import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import java.text.MessageFormat;

import static org.assertj.core.api.Assertions.assertThat;

public class DialogFlowApiTest {

    private static String VALID_PROJECT_ID = "room-reservation-ee77e";

    private static String VALID_LANGUAGE_CODE = "en-US";

    private DialogFlowApi api;

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
        softly.assertThat(VALID_PROJECT_ID).as("Valid project ID").isNotEqualTo(api.getProjectId());
        softly.assertThat(VALID_LANGUAGE_CODE).as("Valid language code").isNotEqualTo(api.getLanguageCode());
    }

    @Test
    public void constructDefaultLanguageCode() {
        api = new DialogFlowApi(VALID_PROJECT_ID);
        softly.assertThat(VALID_PROJECT_ID).as("Valid project ID").isEqualTo(api.getProjectId());
        softly.assertThat(VALID_LANGUAGE_CODE).as("Valid language code").isEqualTo(api.getLanguageCode());
    }
}

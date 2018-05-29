package fr.zelus.jarvis.dialogflow;

import org.junit.Test;

import java.text.MessageFormat;

public class DialogFlowApiTest {

    private static String VALID_PROJECT_ID = "room-reservation-ee77e";

    private static String VALID_LANGUAGE_CODE = "en-US";

    private DialogFlowApi api;

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
        assert VALID_PROJECT_ID.equals(api.getProjectId()): MessageFormat.format("Invalid project ID: expected {0}, " +
                "found {1}", VALID_PROJECT_ID, api.getProjectId());
        assert VALID_LANGUAGE_CODE.equals(api.getLanguageCode()) : MessageFormat.format("Invalid language code: " +
                "expected {0}, found {1}", VALID_LANGUAGE_CODE, api.getLanguageCode());
    }

    @Test
    public void constructDefaultLanguageCode() {
        api = new DialogFlowApi(VALID_PROJECT_ID);
        assert VALID_PROJECT_ID.equals(api.getProjectId()): MessageFormat.format("Invalid project ID: expected {0}, " +
                "found {1}", VALID_PROJECT_ID, api.getProjectId());
        assert VALID_LANGUAGE_CODE.equals(api.getLanguageCode()) : MessageFormat.format("Invalid language code: " +
                "expected {0} (default), found {1}", VALID_LANGUAGE_CODE, api.getLanguageCode());
    }
}

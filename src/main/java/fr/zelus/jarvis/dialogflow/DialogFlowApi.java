package fr.zelus.jarvis.dialogflow;

import com.google.cloud.dialogflow.v2.*;
import fr.inria.atlanmod.commons.log.Log;

import java.io.IOException;
import java.util.UUID;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

public class DialogFlowApi {

    private static String DEFAULT_LANGUAGE_CODE = "en-US";

    private String projectId;
    private String languageCode;
    private SessionsClient sessionsClient;

    public DialogFlowApi(String projectId) {
        this(projectId, DEFAULT_LANGUAGE_CODE);
    }

    public DialogFlowApi(String projectId, String languageCode) {
        checkNotNull(projectId, "Cannot construct a DialogFlow API instance from a null project ID");
        checkNotNull(languageCode, "Cannot construct a DialogFlow API instance from a null language code");
        Log.info("Creating a new DialogFlowAPI");
        try {
            Log.info("Starting DialogFlow Client");
            this.projectId = projectId;
            this.languageCode = languageCode;
            this.sessionsClient = SessionsClient.create();
        } catch (IOException e) {
            throw new DialogFlowException("Cannot construct the DialogFlow API", e);
        }
    }

    public String getProjectId() {
        return projectId;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public SessionName createSession() {
        UUID identifier = UUID.randomUUID();
        SessionName session = SessionName.of(projectId, identifier.toString());
        Log.info("New session created with path {0}", session.toString());
        return session;
    }

    public Intent getIntent(String text, SessionName session) {
        TextInput.Builder textInput = TextInput.newBuilder().setText(text).setLanguageCode(languageCode);
        QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();
        DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);
        QueryResult queryResult = response.getQueryResult();
        Log.info("====================\n" +
                "Query Text: {0} \n" +
                "Detected Intent: {1} (confidence: {2})\n" +
                "Fulfillment Text: {3}", queryResult.getQueryText(), queryResult.getIntent()
                .getDisplayName(), queryResult.getIntentDetectionConfidence(), queryResult.getFulfillmentText());
        return queryResult.getIntent();
    }

}

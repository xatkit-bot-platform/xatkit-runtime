package fr.zelus.jarvis.dialogflow;

import com.google.cloud.dialogflow.v2.*;
import fr.inria.atlanmod.commons.log.Log;

import java.io.IOException;
import java.util.UUID;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static fr.inria.atlanmod.commons.Preconditions.checkArgument;

/**
 * A wrapper of the DialogFlow API that provides utility methods to connect to a given DialogFlow project, start
 * sessions, and detect {@link Intent}s from textual inputs.
 * <p>
 * This class is used to easily setup a connection to a given DialogFlow project. Note that in addition to the
 * constructor parameters, the {@code GOOGLE_APPLICATION_CREDENTIALS} environment variable must be set and point to
 * the DialogFlow project's key. See
 * <a href="https://cloud.google.com/dialogflow-enterprise/docs/reference/libraries">DialogFlow documentation</a> for
 * further information.
 */
public class DialogFlowApi {

    /**
     * The default language processed by DialogFlow.
     */
    private static String DEFAULT_LANGUAGE_CODE = "en-US";

    /**
     * The unique identifier of the DialogFlow project.
     */
    private String projectId;

    /**
     * The language code of the DialogFlow project.
     */
    private String languageCode;

    /**
     * The client instance managing DialogFlow sessions.
     * <p>
     * This instance is used to initiate new sessions (see {@link #createSession()}) and send {@link Intent}
     * detection queries to the DialogFlow engine.
     */
    private SessionsClient sessionsClient;

    /**
     * Constructs a {@link DialogFlowApi} with the provided {@code projectId} and sets its language to
     * {@link #DEFAULT_LANGUAGE_CODE}.
     * <p>
     * See {@link #DialogFlowApi(String, String)} to construct a {@link DialogFlowApi} instance with a given {@code
     * languageCode}.
     *
     * @param projectId the unique identifier of the DialogFlow project
     * @throws NullPointerException if the provided {@code projectId} or {@code languageCode} is
     *                              {@code null}.
     * @throws DialogFlowException  if the client failed to start a new session
     * @see #DialogFlowApi(String, String)
     */
    public DialogFlowApi(String projectId) {
        this(projectId, DEFAULT_LANGUAGE_CODE);
    }

    /**
     * Constructs a {@link DialogFlowApi} with the provided {@code projectId} and {@code languageCode}.
     *
     * @param projectId    the unique identifier of the DialogFlow project
     * @param languageCode the code of the language processed by DialogFlow
     * @throws NullPointerException if the provided {@code projectId} or {@code languageCode} is
     *                              {@code null}.
     * @throws DialogFlowException  if the client failed to start a new session
     */
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

    /**
     * Returns the DialogFlow project unique identifier.
     *
     * @return the DialogFlow project unique identifier
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * Returns the code of the language processed by DialogFlow.
     *
     * @return the code of the language processed by DialogFlow
     */
    public String getLanguageCode() {
        return languageCode;
    }

    /**
     * Creates a new DialogFlow session.
     * <p>
     * A DialogFlow session contains contextual information and previous answers for a given client, and should not
     * be shared between clients.
     *
     * @return a {@link SessionName} identifying the created session
     */
    public SessionName createSession() {
        UUID identifier = UUID.randomUUID();
        SessionName session = SessionName.of(projectId, identifier.toString());
        Log.info("New session created with path {0}", session.toString());
        return session;
    }

    /**
     * Returns the {@link Intent} extracted from the provided {@code text}
     * <p>
     * This method uses the provided {@code session} to extract contextual {@link Intent}s, such as follow-up
     * or context-based {@link Intent}s.
     *
     * @param text    a {@link String} representing the textual input to process and extract the {@link Intent} from
     * @param session the client {@link SessionName}
     * @return an {@link Intent} extracted from the provided {@code text}
     * @throws NullPointerException     if the provided {@code text} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code text} is empty
     * @throws DialogFlowException      if an exception is thrown by the underlying DialogFlow engine
     */
    public Intent getIntent(String text, SessionName session) {
        checkNotNull(text, "Cannot retrieve the intent from null");
        checkNotNull(session, "Cannot retrieve the intent using null as a session");
        checkArgument(!text.isEmpty(), "Cannot retrieve the intent from empty string");
        TextInput.Builder textInput = TextInput.newBuilder().setText(text).setLanguageCode(languageCode);
        QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();
        DetectIntentResponse response;
        try {
            response = sessionsClient.detectIntent(session, queryInput);
        } catch (Exception e) {
            throw new DialogFlowException(e);
        }
        QueryResult queryResult = response.getQueryResult();
        Log.info("====================\n" +
                "Query Text: {0} \n" +
                "Detected Intent: {1} (confidence: {2})\n" +
                "Fulfillment Text: {3}", queryResult.getQueryText(), queryResult.getIntent()
                .getDisplayName(), queryResult.getIntentDetectionConfidence(), queryResult.getFulfillmentText());
        return queryResult.getIntent();
    }

}

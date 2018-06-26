package fr.zelus.jarvis.dialogflow;

import com.google.api.core.ApiFuture;
import com.google.api.gax.rpc.FailedPreconditionException;
import com.google.cloud.ProjectName;
import com.google.cloud.dialogflow.v2.*;
import com.google.longrunning.Operation;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.intent.IntentDefinition;
import fr.zelus.jarvis.intent.IntentFactory;
import fr.zelus.jarvis.intent.RecognizedIntent;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A wrapper of the DialogFlow API that provides utility methods to connect to a given DialogFlow project, start
 * sessions, manage registered intents, and detect intents instances from textual inputs.
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
     * Represents the DialogFlow project name.
     * <p>
     * This attribute is used to compute project-level operations, such as the training of the underlying
     * DialogFlow's agent.
     *
     * @see #trainMLEngine()
     */
    private ProjectName projectName;

    /**
     * Represents the DialogFlow agent name.
     * <p>
     * This attribute is used to compute intent-level operations, such as retrieving the list of registered
     * {@link Intent}s, or deleting specific {@link Intent}s.
     *
     * @see #registerIntentDefinition(IntentDefinition)
     * @see #deleteIntentDefinition(IntentDefinition)
     */
    private ProjectAgentName projectAgentName;

    /**
     * The client instance managing DialogFlow agent-related queries.
     * <p>
     * This client is used to compute project-level operations, such as the training of the underlying DialogFlow's
     * agent.
     *
     * @see #trainMLEngine()
     */
    private AgentsClient agentsClient;

    /**
     * The client instance managing DialogFlow intent-related queries.
     * <p>
     * This client is used to compute intent-level operations, such as retrieving the list of registered
     * {@link Intent}s, or deleting specific {@link Intent}s.
     *
     * @see #registerIntentDefinition(IntentDefinition)
     * @see #deleteIntentDefinition(IntentDefinition)
     */
    private IntentsClient intentsClient;

    /**
     * The client instance managing DialogFlow sessions.
     * <p>
     * This instance is used to initiate new sessions (see {@link #createSession()}) and send {@link Intent}
     * detection queries to the DialogFlow engine.
     */
    private SessionsClient sessionsClient;


    /**
     * The {@link IntentFactory} used to create {@link RecognizedIntent} instances from DialogFlow computed
     * {@link Intent}s.
     */
    private IntentFactory intentFactory;

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
            this.intentsClient = IntentsClient.create();
            this.projectAgentName = ProjectAgentName.of(projectId);
            this.agentsClient = AgentsClient.create();
            this.projectName = ProjectName.of(projectId);
            this.intentFactory = IntentFactory.eINSTANCE;
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
     * Returns the full descriptions of the {@link Intent}s that are registered in the DialogFlow project.
     * <p>
     * The full descriptions of the {@link Intent}s include the {@code training phrases}, that are typically used in
     * testing methods to check that a created {@link Intent} contains all the information provided to the API. To
     * get a partial description of the registered {@link Intent}s see {@link #getRegisteredIntents()}.
     * <p>
     * <b>Note:</b> this method is protected for testing purposes, and should not be called by client code.
     *
     * @return the full descriptions of the {@link Intent}s that are registered in the DialogFlow project
     * @throws DialogFlowException if the {@link DialogFlowApi} is shutdown
     */
    protected List<Intent> getRegisteredIntentsFullView() {
        if (isShutdown()) {
            throw new DialogFlowException("Cannot retrieve the registered Intents (full view), the DialogFlow API is " +
                    "shutdown");
        }
        List<Intent> registeredIntents = new ArrayList<>();
        ListIntentsRequest request = ListIntentsRequest.newBuilder().setIntentView(IntentView.INTENT_VIEW_FULL)
                .setParent(projectAgentName.toString()).build();
        for (Intent intent : intentsClient.listIntents(request).iterateAll()) {
            registeredIntents.add(intent);
        }
        return registeredIntents;
    }

    /**
     * Returns the partial description of the {@link Intent}s that are registered in the DialogFlow project.
     * <p>
     * The partial descriptions of the {@link Intent}s does not include the {@code training phrases}. To get a full
     * description of the registered {@link Intent}s see {@link #getRegisteredIntentsFullView()}
     * <p>
     * <b>Note:</b> this method is protected for testing purposes, and should not be called by client code.
     *
     * @return the partial descriptions of the {@link Intent}s that are registered in the DialogFlow project
     * @throws DialogFlowException if the {@link DialogFlowApi} is shutdown
     */
    protected List<Intent> getRegisteredIntents() {
        if (isShutdown()) {
            throw new DialogFlowException("Cannot retrieve the registered Intents (partial view), the DialogFlow API " +
                    "is shutdown");
        }
        List<Intent> registeredIntents = new ArrayList<>();
        for (Intent intent : intentsClient.listIntents(projectAgentName).iterateAll()) {
            registeredIntents.add(intent);
        }
        return registeredIntents;
    }

    /**
     * Registers the provided {@code intentDefinition} in the DialogFlow project.
     * <p>
     * This method reuses the information contained in the provided {@link IntentDefinition} to create a new
     * DialogFlow {@link Intent} and add it to the current project.
     * <p>
     * <b>Note:</b> this method does not train the underlying DialogFlow Machine Learning Engine, so multiple calls
     * to this method are not generating multiple training calls. Once all the {@link IntentDefinition}s have been
     * registered to the DialogFlow project use {@link #trainMLEngine()} to train the ML Engine.
     *
     * @param intentDefinition the {@link IntentDefinition} to register to the DialogFlow project
     * @throws DialogFlowException if the {@link DialogFlowApi} is shutdown, or if the {@link Intent} already exists in
     *                             the DialogFlow project
     * @see #trainMLEngine()
     */
    public void registerIntentDefinition(IntentDefinition intentDefinition) {
        if (isShutdown()) {
            throw new DialogFlowException(MessageFormat.format("Cannot register the Intent {0}, the DialogFlow API is" +
                    " " +
                    "shutdown", intentDefinition.getName()));
        }
        checkNotNull(intentDefinition, "Cannot register the IntentDefinition null");
        checkNotNull(intentDefinition.getName(), "Cannot register the IntentDefinition with null as its name");
        Log.info("Registering intent {0}", intentDefinition.getName());
        List<String> trainingSentences = intentDefinition.getTrainingSentences();
        List<Intent.TrainingPhrase> dialogFlowTrainingPhrases = new ArrayList<>();
        for (String trainingSentence : trainingSentences) {
            dialogFlowTrainingPhrases.add(Intent.TrainingPhrase.newBuilder().addParts(Intent.TrainingPhrase.Part
                    .newBuilder().setText(trainingSentence).build()).build());
        }
        Intent intent = Intent.newBuilder().setDisplayName(intentDefinition.getName()).addAllTrainingPhrases
                (dialogFlowTrainingPhrases).build();
        try {
            Intent response = intentsClient.createIntent(projectAgentName, intent);
            Log.info("Intent {0} successfully registered", response.getDisplayName());
        } catch (FailedPreconditionException e) {
            if (e.getMessage().contains("already exists")) {
                String errorMessage = MessageFormat.format("Cannot register the intent {0}, the intent already " +
                        "exists", intentDefinition.getName());
                Log.error(errorMessage);
                throw new DialogFlowException(errorMessage, e);
            }
        }
    }

    /**
     * Deletes the {@link Intent} matching the provided {@code intentDefinition} from the DialogFlow project.
     * <p>
     * <b>Note:</b> this method does not train the underlying DialogFlow Machine Learning Engine, so multiple calls
     * to this method are not generating multiple training calls. Once all the {@link IntentDefinition}s have been
     * deleted from the DialogFlow project use {@link #trainMLEngine()} to train the ML Engine.
     *
     * @param intentDefinition the {@link IntentDefinition} to delete from the DialogFlow project
     * @throws DialogFlowException if the {@link DialogFlowApi} is shutdown
     * @see #trainMLEngine()
     */
    public void deleteIntentDefinition(IntentDefinition intentDefinition) {
        if (isShutdown()) {
            throw new DialogFlowException(MessageFormat.format("Cannot delete the Intent {0}, the DialogFlow API is " +
                    "shutdown", intentDefinition.getName()));
        }
        checkNotNull(intentDefinition, "Cannot delete the IntentDefinition null");
        checkNotNull(intentDefinition.getName(), "Cannot delete the IntentDefinition with null as its name");
        List<Intent> registeredIntents = getRegisteredIntents();
        for (Intent intent : registeredIntents) {
            if (intent.getDisplayName().equals(intentDefinition.getName())) {
                Log.info("Deleting intent {0}", intentDefinition.getName());
                intentsClient.deleteIntent(intent.getName());
                Log.info("Intent {0} deleted", intentDefinition.getName());
                return;
            }
        }
        Log.warn("Cannot delete the Intent {0}, the intent does not exist", intentDefinition.getName());
    }

    /**
     * Sends a training query to the DialogFlow ML Engine and waits for its completion.
     * <p>
     * This method checks every second whether the underlying ML Engine has finished its training. Note that this
     * method is blocking as long as the ML Engine training is not terminated, and may not terminate if an issue
     * occurred on the DialogFlow side.
     *
     * @throws DialogFlowException if the {@link DialogFlowApi} is shutdown
     */
    public void trainMLEngine() {
        if (isShutdown()) {
            throw new DialogFlowException("Cannot train the ML Engine, the DialogFlow API is shutdown");
        }
        Log.info("Starting ML Engine Training (this make take a few minutes)");
        TrainAgentRequest request = TrainAgentRequest.newBuilder()
                .setParent(projectName.toString())
                .build();
        ApiFuture<Operation> future = agentsClient.trainAgentCallable().futureCall(request);
        try {
            Operation operation = future.get();
            while (!operation.getDone()) {
                Thread.sleep(1000);
                /*
                 * Retrieve the new version of the Operation from the API.
                 */
                operation = agentsClient.getOperationsClient().getOperation(operation.getName());
            }
            Log.info("ML Engine Training completed");
        } catch (InterruptedException | ExecutionException e) {
            String errorMessage = "An error occurred during the ML Engine Training";
            Log.error(errorMessage);
            throw new DialogFlowException(errorMessage, e);
        }
    }

    /**
     * Creates a new DialogFlow session.
     * <p>
     * A DialogFlow session contains contextual information and previous answers for a given client, and should not
     * be shared between clients.
     *
     * @return a {@link SessionName} identifying the created session
     * @throws DialogFlowException if the {@link DialogFlowApi} is shutdown
     */
    public SessionName createSession() {
        if (isShutdown()) {
            throw new DialogFlowException("Cannot create a new Session, the DialogFlow API is shutdown");
        }
        UUID identifier = UUID.randomUUID();
        SessionName session = SessionName.of(projectId, identifier.toString());
        Log.info("New session created with path {0}", session.toString());
        return session;
    }

    /**
     * Shuts down the DialogFlow clients and invalidates the session.
     * <p>
     * <b>Note:</b> calling this method invalidates the DialogFlow connection, and thus this class cannot be used to
     * access DialogFlow API anymore.
     */
    public void shutdown() {
        if (isShutdown()) {
            throw new DialogFlowException("Cannot perform shutdown, DialogFlow API is already shutdown");
        }
        this.sessionsClient.shutdownNow();
        this.intentsClient.shutdownNow();
        this.agentsClient.shutdownNow();
    }

    /**
     * Returns whether the DialogFlow client is shutdown.
     *
     * @return {@code true} if the DialogFlow client is shutdown, {@code false} otherwise
     */
    public boolean isShutdown() {
        return this.sessionsClient.isShutdown() || this.intentsClient.isShutdown() || this.agentsClient.isShutdown();
    }

    /**
     * Returns the {@link RecognizedIntent} extracted from the provided {@code text}
     * <p>
     * The returned {@link RecognizedIntent} is constructed from the raw {@link Intent} returned by the DialogFlow
     * API, using the mapping defined in {@link #convertDialogFlowIntentToRecognizedIntent(Intent)}.
     * {@link RecognizedIntent}s are used
     * to wrap the Intents returned by the Intent Recognition APIs and decouple the application from the concrete API
     * used.
     * <p>
     * This method uses the provided {@code session} to extract contextual {@link Intent}s, such as follow-up
     * or context-based {@link Intent}s.
     *
     * @param text    a {@link String} representing the textual input to process and extract the {@link Intent} from
     * @param session the client {@link SessionName}
     * @return a {@link RecognizedIntent} extracted from the provided input {@code text}
     * @throws NullPointerException     if the provided {@code text} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code text} is empty
     * @throws DialogFlowException      if the {@link DialogFlowApi} is shutdown or if an exception is thrown by the
     *                                  underlying DialogFlow engine
     */
    public RecognizedIntent getIntent(String text, SessionName session) {
        if (isShutdown()) {
            throw new DialogFlowException("Cannot extract an Intent from the provided input, the DialogFlow API is " +
                    "shutdown");
        }
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
        return convertDialogFlowIntentToRecognizedIntent(queryResult.getIntent());
    }

    private IntentDefinition convertDialogFlowIntentToIntentDefinition(Intent intent) {
        if (nonNull(intent)) {
            return JarvisCore.getInstance().getIntentDefinitionRegistry().getIntentDefinition(intent.getDisplayName());
            /*
             * The code below build an IntentDefinition from scratch, but does not check that the intent is
             * registered. We should move to something like that in the future, in order to let modelers design their
             * intents in the DialogFlow page if they want. Moving to this kind of architecture does not require an
             * IntentDefinition factory, or at least not the kind of one we are using now. (we need to synchronize it)
             */
//            IntentDefinition intentDefinition = intentFactory.createIntentDefinition();
//            intentDefinition.setName(intent.getDisplayName());
//            for(Intent.TrainingPhrase trainingPhrase : intent.getTrainingPhrasesList()) {
//                for(Intent.TrainingPhrase.Part part : trainingPhrase.getPartsList()) {
//                    intentDefinition.getTrainingSentences().add(part.getText());
//                }
//            }
//            return intentDefinition;
        } else {
            Log.warn("Cannot convert null to IntentDefinition");
            return null;
        }
    }

    private RecognizedIntent convertDialogFlowIntentToRecognizedIntent(Intent intent) {
        if (nonNull(intent)) {
            RecognizedIntent recognizedIntent = intentFactory.createRecognizedIntent();
            /*
             * Retrieve the IntentDefinition corresponding to this Intent.
             */
            IntentDefinition intentDefinition = convertDialogFlowIntentToIntentDefinition(intent);
            if (isNull(intentDefinition)) {
                String errorMessage = MessageFormat.format("Cannot retrieve the IntentDefinition associated to the " +
                        "provided DialogFlow Intent {0}", intent.getDisplayName());
                Log.error(errorMessage);
                throw new DialogFlowException(errorMessage);
            }
            recognizedIntent.setDefinition(intentDefinition);
            /*
             * Set the output context values.
             */
            if (intent.getOutputContextsCount() > 0) {
                if (intent.getOutputContextsCount() > 1) {
                    Log.warn("Multiple output contexts are not supported for now, proceeding with the first context " +
                            "found");
                }
                Context outContext = intent.getOutputContexts(0);
                Collection<Object> outContextValues = outContext.getParameters().getAllFields().values();
                for (Object value : outContextValues) {
                    if (value instanceof String) {
                        recognizedIntent.getOutContextValues().add((String) value);
                    } else {
                        throw new UnsupportedOperationException("Only String output context values are supported for " +
                                "now");
                    }
                }
                return recognizedIntent;
            } else {
                return recognizedIntent;
            }
        } else {
            Log.warn("Cannot convert null to a RecognizedIntent");
            return null;
        }
    }

    /**
     * Closes the DialogFlow session if it is not shutdown yet.
     */
    @Override
    protected void finalize() {
        if (!sessionsClient.isShutdown()) {
            Log.warn("DialogFlow session was not closed properly, calling automatic shutdown");
            this.sessionsClient.shutdownNow();
        }
        if (!intentsClient.isShutdown()) {
            Log.warn("DialogFlow Intent client was not closed properly, calling automatic shutdown");
            this.intentsClient.shutdownNow();
        }
        if (!agentsClient.isShutdown()) {
            Log.warn("DialogFlow Agent client was not closed properly, calling automatic shutdown");
            this.agentsClient.shutdownNow();
        }
    }
}

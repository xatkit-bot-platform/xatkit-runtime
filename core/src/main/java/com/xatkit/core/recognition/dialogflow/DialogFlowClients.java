package com.xatkit.core.recognition.dialogflow;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dialogflow.v2.AgentsClient;
import com.google.cloud.dialogflow.v2.AgentsSettings;
import com.google.cloud.dialogflow.v2.ContextsClient;
import com.google.cloud.dialogflow.v2.ContextsSettings;
import com.google.cloud.dialogflow.v2.EntityTypesClient;
import com.google.cloud.dialogflow.v2.EntityTypesSettings;
import com.google.cloud.dialogflow.v2.Intent;
import com.google.cloud.dialogflow.v2.IntentsClient;
import com.google.cloud.dialogflow.v2.IntentsSettings;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.xatkit.util.FileUtils;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.configuration2.Configuration;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Contains the clients used to access the DialogFlow API.
 * <p>
 * This class is initialized with a {@link DialogFlowConfiguration}, and setup the DialogFlow clients with the
 * credential file located at {@link DialogFlowConfiguration#getGoogleCredentialsPath()}.
 * <p>
 * If the provided {@link DialogFlowConfiguration} does not contain a path to a credentials file this class will try
 * to load it from environment variables.
 */
@Value
public class DialogFlowClients {

    /**
     * The client instance managing DialogFlow agent-related queries.
     * <p>
     * This client is used to compute project-level operations, such as the training of the underlying DialogFlow's
     * agent.
     */
    private AgentsClient agentsClient;

    /**
     * The client instance managing DialogFlow intent-related queries.
     * <p>
     * This client is used to compute intent-level operations, such as retrieving the list of registered
     * {@link Intent}s, or deleting specific {@link Intent}s.
     */
    private IntentsClient intentsClient;

    /**
     * The client instance managing DialogFlow entity-related queries.
     * <p>
     * This client is used to compute entity-level operations, such as retrieving the list of registered
     * {@link com.google.cloud.dialogflow.v2.EntityType.Entity} instances, or deleting specific
     * {@link com.google.cloud.dialogflow.v2.EntityType.Entity}.
     */
    private EntityTypesClient entityTypesClient;

    /**
     * The client instance managing DialogFlow sessions.
     * <p>
     * This instance is used to initiate new sessions and send {@link Intent} detection queries to the DialogFlow
     * engine.
     */
    private SessionsClient sessionsClient;

    /**
     * The client instance managing DialogFlow contexts.
     * <p>
     * This {@link ContextsClient} is used to merge local context information in the DialogFlow session. This enables
     * to match {@link com.xatkit.intent.IntentDefinition} with input contexts set from local computation (such as
     * received events, custom variables, etc).
     */
    private ContextsClient contextsClient;

    /**
     * Initializes the DialogFlow clients using the provided {@code configuration}.
     * <p>
     * The created clients are setup with the credential file located at
     * {@link DialogFlowConfiguration#getGoogleCredentialsPath()}.
     * <p>
     * If the provided {@code configuration} does not define a credentials file path the created clients are
     * initialized from the credentials file path stored in the {@code GOOGLE_APPLICATION_CREDENTIALS} environment
     * variable.
     *
     * @param configuration the {@link Configuration} containing the credentials file path
     * @throws DialogFlowException if the provided {@code configuration} or {@code GOOGLE_APPLICATION_CREDENTIALS}
     *                             environment variable does not contain a valid credentials file path
     */
    public DialogFlowClients(@NonNull DialogFlowConfiguration configuration) {
        CredentialsProvider credentialsProvider = getCredentialsProvider(configuration);
        AgentsSettings agentsSettings;
        IntentsSettings intentsSettings;
        EntityTypesSettings entityTypesSettings;
        SessionsSettings sessionsSettings;
        ContextsSettings contextsSettings;
        try {
            if (isNull(credentialsProvider)) {
                /*
                 * No credentials provided, using the GOOGLE_APPLICATION_CREDENTIALS environment variable.
                 */
                Log.warn("No credentials file provided, using GOOGLE_APPLICATION_CREDENTIALS environment variable");
                agentsSettings = AgentsSettings.newBuilder().build();
                intentsSettings = IntentsSettings.newBuilder().build();
                entityTypesSettings = EntityTypesSettings.newBuilder().build();
                sessionsSettings = SessionsSettings.newBuilder().build();
                contextsSettings = ContextsSettings.newBuilder().build();
            } else {
                agentsSettings = AgentsSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
                intentsSettings = IntentsSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
                entityTypesSettings = EntityTypesSettings.newBuilder().setCredentialsProvider(credentialsProvider)
                        .build();
                sessionsSettings = SessionsSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
                contextsSettings = ContextsSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
            }
            this.agentsClient = AgentsClient.create(agentsSettings);
            this.sessionsClient = SessionsClient.create(sessionsSettings);
            this.intentsClient = IntentsClient.create(intentsSettings);
            this.entityTypesClient = EntityTypesClient.create(entityTypesSettings);
            this.contextsClient = ContextsClient.create(contextsSettings);
        } catch (IOException e) {
            throw new DialogFlowException("An error occurred when initializing the DialogFlow clients, see attached " +
                    "exception", e);
        }
    }

    /**
     * Shutdowns the DialogFlow clients.
     */
    public void shutdown() {
        this.sessionsClient.shutdownNow();
        this.intentsClient.shutdownNow();
        this.entityTypesClient.shutdownNow();
        this.contextsClient.shutdownNow();
        this.agentsClient.shutdownNow();
    }

    /**
     * Returns whether the DialogFlow clients are shutdown.
     * <p>
     * This method returns {@code true} if at least {@code 1} client is shutdown.
     *
     * @return {@code true} if the DialogFlow clients are shutdown, {@code false} otherwise
     */
    public boolean isShutdown() {
        return this.sessionsClient.isShutdown()
                && this.intentsClient.isShutdown()
                && this.entityTypesClient.isShutdown()
                && this.contextsClient.isShutdown()
                && this.agentsClient.isShutdown();
    }

    /**
     * Shutdowns the DialogFlow clients if they haven't been shutdown yet.
     * <p>
     * This method logs a warning for each client closed this way, use {@link #shutdown()} to shutdown the clients
     * appropriately.
     */
    @Override
    protected void finalize() {
        if (!this.sessionsClient.isShutdown()) {
            Log.warn("DialogFlow session was not closed properly, calling automatic shutdown");
            this.sessionsClient.shutdownNow();
        }
        if (!this.intentsClient.isShutdown()) {
            Log.warn("DialogFlow Intent client was not closed properly, calling automatic shutdown");
            this.intentsClient.shutdownNow();
        }
        if (!this.entityTypesClient.isShutdown()) {
            Log.warn("DialogFlow EntityType client was not closed properly, calling automatic shutdown");
            this.entityTypesClient.shutdownNow();
        }
        if (!this.contextsClient.isShutdown()) {
            Log.warn("DialogFlow Context client was not closed properly, calling automatic shutdown");
            this.contextsClient.shutdownNow();
        }
        if (!this.agentsClient.isShutdown()) {
            Log.warn("DialogFlow Agent client was not closed properly, calling automatic shutdown");
            this.agentsClient.shutdownNow();
        }
    }

    /**
     * Creates the Google's {@link CredentialsProvider} from the provided {@code configuration}.
     * <p>
     * This method loads the credentials file located at {@link DialogFlowConfiguration#getGoogleCredentialsPath()}.
     * If the file does not exist the method attempts to load it from the classpath.
     * <p>
     * This method returns {@code null} if the provided {@code configuration} does not contain a path.
     *
     * @param configuration the {@link DialogFlowConfiguration} containing the credentials file path
     * @return the created {@link CredentialsProvider}, or {@code null} if the provided {@code configuration} does
     * not specify a credentials file path
     * @throws DialogFlowException if an error occurred when loading the credentials file
     */
    private @Nullable
    CredentialsProvider getCredentialsProvider(@NonNull DialogFlowConfiguration configuration) {
        String credentialsPath = configuration.getGoogleCredentialsPath();
        if (nonNull(credentialsPath)) {
            Log.info("Loading Google Credentials file {0}", credentialsPath);
            InputStream credentialsInputStream;
            try {
                File credentialsFile = FileUtils.getFile(credentialsPath, configuration.getBaseConfiguration());
                if (credentialsFile.exists()) {
                    credentialsInputStream = new FileInputStream(credentialsFile);
                } else {
                    Log.warn("Cannot load the credentials file at {0}, trying to load it from the classpath",
                            credentialsPath);
                    String classpathCredentialsPath = this.getClass().getClassLoader().getResource(credentialsPath)
                            .getFile();
                    credentialsInputStream = new FileInputStream(classpathCredentialsPath);
                }
                return FixedCredentialsProvider.create(GoogleCredentials.fromStream(credentialsInputStream));
            } catch (FileNotFoundException e) {
                throw new DialogFlowException(MessageFormat.format("Cannot find the credentials file at {0}",
                        credentialsPath), e);
            } catch (IOException e) {
                throw new DialogFlowException("Cannot retrieve the credentials provider, see attached exception", e);
            }
        }
        return null;
    }
}

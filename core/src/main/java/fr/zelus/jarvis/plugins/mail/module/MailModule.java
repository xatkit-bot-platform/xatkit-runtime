package fr.zelus.jarvis.plugins.mail.module;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.core.JarvisModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

/**
 * A {@link JarvisModule} concrete implementation providing mailing functionality that can be used in orchestration
 * models.
 * <p>
 * This module defines a single action {@link fr.zelus.jarvis.plugins.mail.module.action.SendMail} that sends a mail
 * through the {@link Gmail} service to a given address.
 * <p>
 * This class is part of jarvis' core modules, and can be used in an orchestration model by importing the <i>core
 * .MailModule</i> package.
 *
 * @see fr.zelus.jarvis.plugins.mail.module.action.SendMail
 */
public class MailModule extends JarvisModule {

    /**
     * The name of the application used to initialize the {@link Gmail} service.
     */
    private static final String APPLICATION_NAME = "Jarvis";

    /**
     * The {@link JsonFactory} instance used to parse messages from the {@link Gmail} service.
     */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /**
     * The folder used to store the {@link Gmail} application credentials.
     */
    private static final String CREDENTIALS_FOLDER = "credentials"; // Directory to store user credentials.

    /**
     * The OAuth scopes required by the application.
     */
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);

    /**
     * The path of the secret API key used to access the {@link Gmail} service.
     * <p>
     * This information should be provided by the module's {@link org.apache.commons.configuration2.Configuration},
     * or retrieved from the system environment variables (see #62).
     */
    private static final String CLIENT_SECRET_DIR = "/gmail_secret.json";

    /**
     * The {@link Gmail} service used to send mails.
     */
    private Gmail service;

    /**
     * Constructs a new {@link MailModule} and initializes the underlying {@link Gmail} service.
     */
    public MailModule() {
        super();
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (IOException | GeneralSecurityException e) {
            String errorMessage = MessageFormat.format("Cannot start {0}", this.getClass().getSimpleName());
            throw new JarvisException(errorMessage, e);
        }
    }

    /**
     * Loads the application secrets and build the associated credentials.
     *
     * @param HTTP_TRANSPORT the {@link NetHttpTransport} used to build the application authorization
     * @return the built {@link Credential}
     * @throws IOException if an error occurred when accessing the remote {@link Gmail} service
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = this.getClass().getResourceAsStream(CLIENT_SECRET_DIR);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(CREDENTIALS_FOLDER)))
                .setAccessType("offline")
                .build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    /**
     * Returns the {@link Gmail} service associated to this module.
     *
     * @return the {@link Gmail} service associated to this module
     */
    public Gmail getService() {
        return service;
    }


}

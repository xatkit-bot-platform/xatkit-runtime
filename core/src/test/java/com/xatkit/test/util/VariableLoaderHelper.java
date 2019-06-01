package com.xatkit.test.util;

import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.*;
import java.net.URL;
import java.text.MessageFormat;

import static java.util.Objects.isNull;

/**
 * An utility class to load test-related variables.
 * <p>
 * This class first tries to load the variables from the environment variables. If no environment variable is set
 * this helper attempts to find a local configuration file (not pushed on the project's repository) containing the
 * requested variable.
 */
public class VariableLoaderHelper {

    private static String LOCAL_FILE_PATH = "test-variables.properties";

    private static String XATKIT_DIALOGFLOW_PROJECT = "XATKIT_DIALOGFLOW_PROJECT";

    private static String XATKIT_DIALOGFLOW_LANGUAGE = "XATKIT_DIALOGFLOW_LANGUAGE";

    private static String XATKIT_DIALOGFLOW_CREDENTIALS_PATH = "XATKIT_DIALOGFLOW_CREDENTIALS_PATH";

    private static String XATKIT_SLACK_TOKEN_KEY = "XATKIT_SLACK_TOKEN";

    private static String XATKIT_DISCORD_TOKEN = "XATKIT_DISCORD_TOKEN";

    public static String getXatkitDialogFlowProject() {
        return getVariable(XATKIT_DIALOGFLOW_PROJECT);
    }

    public static String getXatkitDialogFlowLanguage() {
        return getVariable(XATKIT_DIALOGFLOW_LANGUAGE);
    }

    public static String getXatkitDialogflowCredentialsPath() {
        return getVariable(XATKIT_DIALOGFLOW_CREDENTIALS_PATH);
    }

    public static String getXatkitSlackToken() {
        return getVariable(XATKIT_SLACK_TOKEN_KEY);
    }

    public static String getXatkitDiscordToken() {
        return getVariable(XATKIT_DISCORD_TOKEN);
    }

    private static String getVariable(String key) {
        String token = System.getenv(key);
        if (isNull(token) || token.isEmpty()) {
            Log.debug("Cannot retrieve Xatkit variable {0} from the environment variables, using local file {1} ",
                    key, LOCAL_FILE_PATH);
            URL resource = VariableLoaderHelper.class.getClassLoader().getResource(LOCAL_FILE_PATH);
            if (isNull(resource)) {
                throw new RuntimeException(MessageFormat.format("Cannot retrieve Xatkit bot variables from local " +
                        "file: the file {0} does not exist", LOCAL_FILE_PATH));
            }
            String fileString = resource.getFile();
            FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(fileString);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            Reader reader = new InputStreamReader(fileInputStream);
            PropertiesConfiguration configuration = new PropertiesConfiguration();
            try {
                configuration.read(reader);
            } catch (ConfigurationException | IOException e) {
                throw new RuntimeException(e);
            }
            return configuration.getString(key);
        }
        return token;
    }
}
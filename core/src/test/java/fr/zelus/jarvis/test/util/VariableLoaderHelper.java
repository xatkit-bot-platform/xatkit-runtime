package fr.zelus.jarvis.test.util;

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

    private static String JARVIS_DIALOGFLOW_PROJECT = "JARVIS_DIALOGFLOW_PROJECT";

    private static String JARVIS_TEST_DIALOGFLOW_PROJECT = "JARVIS_TEST_DIALOGFLOW_PROJECT";

    private static String JARVIS_SLACK_TOKEN_KEY = "JARVIS_SLACK_TOKEN";

    private static String JARVIS_DISCORD_TOKEN = "JARVIS_DISCORD_TOKEN";

    public static String getJarvisDialogFlowProject() {
        return getVariable(JARVIS_DIALOGFLOW_PROJECT);
    }

    public static String getJarvisTestDialogFlowProject() {
        return getVariable(JARVIS_TEST_DIALOGFLOW_PROJECT);
    }

    public static String getJarvisSlackToken() {
        return getVariable(JARVIS_SLACK_TOKEN_KEY);
    }

    public static String getJarvisDiscordToken() {
        return getVariable(JARVIS_DISCORD_TOKEN);
    }

    private static String getVariable(String key) {
        String token = System.getenv(key);
        if(isNull(token) || token.isEmpty()) {
            Log.info("Cannot retrieve Jarvis bot API token from the environment variables, using local file {0} ",
                    LOCAL_FILE_PATH);
            URL resource = VariableLoaderHelper.class.getClassLoader().getResource(LOCAL_FILE_PATH);
            if(isNull(resource)) {
                throw new RuntimeException(MessageFormat.format("Cannot retrieve Jarvis bot variables from local " +
                        "file: the file {0} does not exist", LOCAL_FILE_PATH));
            }
            String fileString = resource.getFile();
            FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(fileString);
            } catch(FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            Reader reader = new InputStreamReader(fileInputStream);
            PropertiesConfiguration configuration = new PropertiesConfiguration();
            try {
                configuration.read(reader);
            } catch(ConfigurationException | IOException e) {
                throw new RuntimeException(e);
            }
            return configuration.getString(key);
        }
        return token;
    }
}
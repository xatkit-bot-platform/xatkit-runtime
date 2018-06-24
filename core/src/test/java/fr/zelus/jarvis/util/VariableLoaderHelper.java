package fr.zelus.jarvis.util;

import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.*;

import static java.util.Objects.isNull;

/**
 * An utility class to load test-related variables.
 * <p>
 * This class first tries to load the variables from the environment variables. If no environment variable is set
 * this helper attempts to find a local configuration file (not pushed on the project's repository) containing the
 * requested variable.
 */
public class VariableLoaderHelper {

    private static String JARVIS_SLACK_TOKEN_KEY = "JARVIS_SLACK_TOKEN";

    public static String getJarvisSlackToken() {
        return getVariable(JARVIS_SLACK_TOKEN_KEY);
    }

    private static String getVariable(String key) {
        String token = System.getenv(key);
        if(isNull(token) || token.isEmpty()) {
            Log.info("Cannot retrieve Jarvis bot API token from the environment variables, using local file instead");
            String fileString = VariableLoaderHelper.class.getClassLoader().getResource("test-variables.properties")
                    .getFile();
            if(isNull(fileString)) {
                throw new RuntimeException("Cannot retrieve Jarvis bot API token from the local file: Cannot load the" +
                        " local file");
            }
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

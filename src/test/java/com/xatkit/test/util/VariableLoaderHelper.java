package com.xatkit.test.util;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
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

    public static String getXatkitDialogFlowProject() {
        return getVariable(XATKIT_DIALOGFLOW_PROJECT);
    }

    public static String getXatkitDialogFlowLanguage() {
        return getVariable(XATKIT_DIALOGFLOW_LANGUAGE);
    }

    public static String getXatkitDialogflowCredentialsPath() {
        return getVariable(XATKIT_DIALOGFLOW_CREDENTIALS_PATH);
    }

    public static String getVariable(String key) {
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
}
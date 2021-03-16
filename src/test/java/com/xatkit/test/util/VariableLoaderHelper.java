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
 * This class loads the variables from the {@code test-variables.properties} configuration file located in {@code
 * /src/test/resources}. This file is not pushed in the Github repository. If you want to test Xatkit you'll need to
 * create your own version of the file with the following properties:
 * <pre>
 * {@code
 * xatkit.dialogflow.projectId = <Your DialogFlow project ID>
 * xatkit.dialogflow.credentials.path = <Path to your DialogFlow credentials file>
 * xatkit.dialogflow.language = <The language of your DialogFlow agent>
 * xatkit.perspectiveapi.apiKey = <Your Perspective API Key>
 * xatkit.nlpjs.agentId = <The name of the NLP.js agent>
 * xatkit.nlpjs.server = <The URL of the NLP.js server>
 * xatkit.nlpjs.basicauth.username = <The username for the basic authentication of the NLP.js server>
 * xatkit.nlpjs.basicauth.password = <The password for the basic authentication of the NLP.js server>
 * }
 * </pre>
 * You'll also need to add your own DialogFlow credentials file in {@code /src/test/resources} named according to
 * your {@code xatkit.dialogflow.credentials.path} property.
 * <p>
 * <b>Note</b>: if you are part of Xatkit you can get in touch with us to get credentials to run your tests.
 */
public class VariableLoaderHelper {

    private static String LOCAL_FILE_PATH = "test-variables.properties";

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
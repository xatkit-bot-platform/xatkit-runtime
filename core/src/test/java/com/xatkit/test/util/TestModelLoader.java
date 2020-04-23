package com.xatkit.test.util;

import com.xatkit.core.RuntimePlatformRegistry;
import com.xatkit.core.XatkitCore;
import com.xatkit.core.recognition.dialogflow.mapper.DialogFlowEntityMapperTest;
import com.xatkit.util.ModelLoader;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;
import java.net.URL;

public class TestModelLoader {

    public static TestBotExecutionModel loadTestBot() throws ConfigurationException {
        ModelLoader modelLoader = new ModelLoader(new RuntimePlatformRegistry());
        URL url = DialogFlowEntityMapperTest.class.getClassLoader().getResource("TestBot/TestBot.properties");
        File propertiesFile = new File(url.getFile());

        Configurations configs = new Configurations();
        Configuration configuration = configs.properties(propertiesFile);
        /*
         * Need to call getAbsoluteFile() in case the provided path only contains the file name, otherwise
         * getParentFile() returns null (see #202)
         */
        configuration.addProperty(XatkitCore.CONFIGURATION_FOLDER_PATH_KEY,
                propertiesFile.getAbsoluteFile().getParentFile().getAbsolutePath());

        return new TestBotExecutionModel(modelLoader.loadExecutionModel(configuration));
    }

    private TestModelLoader() {

    }
}

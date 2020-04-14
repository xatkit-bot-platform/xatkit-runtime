package com.xatkit.core.util;

import com.xatkit.core.RuntimePlatformRegistry;
import com.xatkit.core.XatkitCore;
import com.xatkit.core.XatkitException;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.util.ModelLoader;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class ModelLoaderTest {

    private ModelLoader modelLoader;

    @Test(expected = NullPointerException.class)
    public void constructNullRegistry() {
        modelLoader = new ModelLoader(null);
    }

    @Test
    public void constructValidRegistry() {
        modelLoader = new ModelLoader(new RuntimePlatformRegistry());
        assertThat(modelLoader).isNotNull();
    }

    @Test
    public void loadExistingModel() throws ConfigurationException {
        modelLoader = new ModelLoader(new RuntimePlatformRegistry());
        URL url = this.getClass().getClassLoader().getResource("TestBot/TestBot.properties");
        File propertiesFile = new File(url.getFile());

        Configurations configs = new Configurations();
        Configuration configuration = configs.properties(propertiesFile);
        /*
         * Need to call getAbsoluteFile() in case the provided path only contains the file name, otherwise
         * getParentFile() returns null (see #202)
         */
        configuration.addProperty(XatkitCore.CONFIGURATION_FOLDER_PATH_KEY,
                propertiesFile.getAbsoluteFile().getParentFile().getAbsolutePath());

        ExecutionModel executionModel = modelLoader.loadExecutionModel(configuration);
        assertThat(executionModel).isNotNull();
        /*
         * Just a basic check to see if the states have been loaded.
         */
        assertThat(executionModel.getStates().size()).isGreaterThan(0);
    }

    @Test(expected = XatkitException.class)
    public void loadNotExistingModel() {
        modelLoader = new ModelLoader(new RuntimePlatformRegistry());
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(ModelLoader.EXECUTION_MODEL_KEY, "/invalid-path.execution");
        modelLoader.loadExecutionModel(configuration);
    }
}

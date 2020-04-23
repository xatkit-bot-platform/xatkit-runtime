package com.xatkit.core.util;

import com.xatkit.core.RuntimePlatformRegistry;
import com.xatkit.core.XatkitCore;
import com.xatkit.core.XatkitException;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.util.ModelLoader;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.eclipse.emf.common.util.URI;
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
        Configuration testBotModelConfiguration = getTestBotModelConfiguration();
        ExecutionModel executionModel = modelLoader.loadExecutionModel(testBotModelConfiguration);
        assertThat(executionModel).isNotNull();
        /*
         * Just a basic check to see if the states have been loaded.
         */
        assertThat(executionModel.getStates().size()).isGreaterThan(0);
    }

    @Test(expected = XatkitException.class)
    public void loadInvalidExecutionModelPropertyType() {
        modelLoader = new ModelLoader(new RuntimePlatformRegistry());
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(ModelLoader.EXECUTION_MODEL_KEY, new Integer(2));
        modelLoader.loadExecutionModel(configuration);
    }

    @Test(expected = XatkitException.class)
    public void loadInvalidModelURI() {
        modelLoader = new ModelLoader(new RuntimePlatformRegistry());
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(ModelLoader.EXECUTION_MODEL_KEY, URI.createURI("tmp/test.execution"));
        modelLoader.loadExecutionModel(configuration);
    }

    @Test
    public void loadModelInMemory() {
        modelLoader = new ModelLoader(new RuntimePlatformRegistry());
        ExecutionModel model = ExecutionFactory.eINSTANCE.createExecutionModel();
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(ModelLoader.EXECUTION_MODEL_KEY, model);
        ExecutionModel result = modelLoader.loadExecutionModel(configuration);
        assertThat(result).isEqualTo(model);
    }

    @Test(expected = XatkitException.class)
    public void loadNotExistingModel() {
        modelLoader = new ModelLoader(new RuntimePlatformRegistry());
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(ModelLoader.EXECUTION_MODEL_KEY, "/invalid-path.execution");
        modelLoader.loadExecutionModel(configuration);
    }

    @Test(expected = XatkitException.class)
    public void loadInvalidCustomPlatformBinding() throws ConfigurationException {
        modelLoader = new ModelLoader(new RuntimePlatformRegistry());
        Configuration configuration = getTestBotModelConfiguration();
        configuration.addProperty(ModelLoader.CUSTOM_PLATFORMS_KEY_PREFIX + "Example", "test");
        modelLoader.loadExecutionModel(configuration);
    }

    @Test(expected = XatkitException.class)
    public void loadInvalidCustomLibraryBinding() throws ConfigurationException {
        modelLoader = new ModelLoader(new RuntimePlatformRegistry());
        Configuration configuration = getTestBotModelConfiguration();
        configuration.addProperty(ModelLoader.CUSTOM_LIBRARIES_KEY_PREFIX + "Example", "test");
        modelLoader.loadExecutionModel(configuration);
    }

    private Configuration getTestBotModelConfiguration() throws ConfigurationException {
        URL url = this.getClass().getClassLoader().getResource("TestBot/TestBot.properties");
        File propertiesFile = new File(url.getFile());

        Configurations configs = new Configurations();
        Configuration configuration = configs.properties(propertiesFile);
        configuration.addProperty(XatkitCore.CONFIGURATION_FOLDER_PATH_KEY,
                propertiesFile.getAbsoluteFile().getParentFile().getAbsolutePath());
        /*
         * Need to call getAbsoluteFile() in case the provided path only contains the file name, otherwise
         * getParentFile() returns null (see #202)
         */
        return configuration;
    }
}

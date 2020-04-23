package com.xatkit;

import com.xatkit.core.XatkitCore;
import com.xatkit.util.XatkitEnvironmentConfiguration;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;

import static java.util.Objects.isNull;

/**
 * The main {@link Xatkit} {@link Class} used to start a Xatkit bot instance.
 * <p>
 * This class is executed when invoking {@code java -jar xatkit.jar}. The {@link #main(String[])} method accepts an
 * optional {@link String} argument containing the path of the {@code .properties} file to use to start Xatkit. If
 * Xatkit is started without a {@code .properties} file it will try to load its properties from the
 * {@link System#getenv()} map.
 *
 * @see #main(String[])
 */
public class Xatkit {

    /**
     * The URL of the tutorial article describing Xatkit configuration.
     */
    private static String CONFIGURATION_TUTORIAL_URL = "https://github.com/xatkit-bot-platform/xatkit-runtime/wiki" +
            "/Deploying-chatbots";

    /**
     * The {@link Configuration} used to initialize the {@link XatkitCore} instance.
     */
    private static Configuration configuration;

    /**
     * The {@link XatkitCore} instance used to run the bot.
     */
    private static XatkitCore xatkitCore;

    /**
     * Starts the underlying {@link XatkitCore} engine.
     * <p>
     * The provided {@code args} can contain a single value representing the path of the {@code .properties} file to
     * use to start Xatkit. If Xatkit is started without a {@code .properties} file it will try to load its
     * properties from the {@link System#getenv()} map. In this case the property keys must be upper cased, and
     * {@code .} must be replaced by {@code _}. See
     * <a href="https://github.com/xatkit-bot-platform/xatkit/wiki/Xatkit-Options">the documentation</a>
     * for more information.
     *
     * @param args the program's arguments
     */
    public static void main(String[] args) {
        /*
         * Reset the configuration and XatkitCore instance to null, this is required to run the test cases, where
         * these attributes can be updated multiple times in the same run.
         */
        configuration = null;
        xatkitCore = null;
        if (isNull(args) || args.length != 1) {
            Log.warn("Xatkit started without a configuration file, loading Xatkit properties from environment " +
                    "variables. If you want to use a custom configuration file check our online tutorial " +
                    "here: {0}", CONFIGURATION_TUTORIAL_URL);
            configuration = new XatkitEnvironmentConfiguration();
        } else {
            String configurationFilePath = args[0];
            Log.info("Starting Xatkit with the configuration file {0}", configurationFilePath);
            File propertiesFile = new File(configurationFilePath);
            try {
                Configurations configs = new Configurations();
                configuration = configs.properties(propertiesFile);
                /*
                 * Need to call getAbsoluteFile() in case the provided path only contains the file name, otherwise
                 * getParentFile() returns null (see #202)
                 */
                configuration.addProperty(XatkitCore.CONFIGURATION_FOLDER_PATH_KEY,
                        propertiesFile.getAbsoluteFile().getParentFile().getAbsolutePath());
            } catch (ConfigurationException e) {
                Log.error("Cannot load the configuration file at the given location {0}, please ensure the provided " +
                        "file is a valid properties file. You can check our online tutorial to learn how to provide a" +
                        " custom configuration file here: {1}", propertiesFile.getPath(), CONFIGURATION_TUTORIAL_URL);
                return;
            }
        }
        try {
            xatkitCore = new XatkitCore(configuration);
        } catch (Throwable t) {
            Log.error(t, "Cannot start Xatkit, see the attached exception");
        }
    }

    /**
     * Returns the {@link Configuration} used to initialize the {@link XatkitCore} instance.
     * <p>
     * This method is used for testing purposes, and allows to check the loaded {@link Configuration} even if the
     * {@link XatkitCore} initialization fails.
     *
     * @return the {@link Configuration} used to initialize the {@link XatkitCore} instance.
     */
    public static Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Returns the {@link XatkitCore} instance used to run the bot.
     *
     * @return the {@link XatkitCore} instance used to run the bot
     */
    public static XatkitCore getXatkitCore() {
        return xatkitCore;
    }
}

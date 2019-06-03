package com.xatkit;

import com.xatkit.core.XatkitCore;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;

import static java.util.Objects.isNull;

/**
 * The main {@link Xatkit} {@link Class} used to start a Xatkit bot instance.
 * <p>
 * This class is executed when invoking {@code java -jar xatkit.jar}. The {@link #main(String[])} method accepts a
 * single {@link String} argument containing the path of the {@link org.apache.commons.configuration2.Configuration}
 * file to use to setup the underlying {@link XatkitCore} engine.
 */
public class Xatkit {

    /**
     * The {@link org.apache.commons.configuration2.Configuration} key to store the configuration folder path.
     */
    public static String CONFIGURATION_FOLDER_PATH = "xatkit.core.configuration.path";

    private static String CHECK_TUTORIAL_SENTENCE = "You can check our online tutorial to learn how to setup a bot " +
            "using Xatkit here: https://github.com/xatkit-bot-platform/xatkit/wiki/Deploying-chatbots";

    /**
     * The {@link XatkitCore} instance used to run the bot.
     */
    private static XatkitCore xatkitCore;

    /**
     * Starts the underlying {@link XatkitCore} engine with the
     * {@link org.apache.commons.configuration2.Configuration} retrieved from the provided {@code args}.
     * <p>
     * The provided {@code args} must contain a single value representing the path of the
     * {@link org.apache.commons.configuration2.Configuration} file to use to setup the {@link XatkitCore} engine.
     *
     * @param args the program's arguments
     * @throws NullPointerException     if the provided {@code args} is {@code null}
     * @throws IllegalArgumentException if the provided {@code args} size is different than {@code 1}
     */
    public static void main(String[] args) {
        if(isNull(args) || args.length != 1) {
            Log.error("Cannot start Xatkit, please provide as parameter the path of the Xatkit configuration file to " +
                    "use ({0})", CHECK_TUTORIAL_SENTENCE);
            return;
        }
        String configurationFilePath = args[0];
        Log.info("Starting xatkit with the configuration file {0}", configurationFilePath);
        File propertiesFile = new File(configurationFilePath);
        try {
            Configurations configs = new Configurations();
            PropertiesConfiguration configuration = configs.properties(propertiesFile);
            /*
             * Need to call getAbsoluteFile() in case the provided path only contains the file name, otherwise
             * getParentFile() returns null (see #202)
             */
            configuration.addProperty(CONFIGURATION_FOLDER_PATH,
                    propertiesFile.getAbsoluteFile().getParentFile().getAbsolutePath());
            xatkitCore = new XatkitCore(configuration);
        } catch (ConfigurationException e) {
            Log.error("Cannot load the configuration file at the given location {0}, please ensure the provided file " +
                    "is a valid properties file ({1})", propertiesFile.getPath(), CHECK_TUTORIAL_SENTENCE);
        }
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

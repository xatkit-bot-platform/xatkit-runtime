package edu.uoc.som.jarvis;

import fr.inria.atlanmod.commons.log.Log;
import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.JarvisException;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * The main {@link Jarvis} {@link Class} used to start a Jarvis bot instance.
 * <p>
 * This class is executed when invoking {@code java -jar jarvis.jar}. The {@link #main(String[])} method accepts a
 * single {@link String} argument containing the path of the {@link org.apache.commons.configuration2.Configuration}
 * file to use to setup the underlying {@link JarvisCore} engine.
 */
public class Jarvis {

    /**
     * The {@link org.apache.commons.configuration2.Configuration} key to store the configuration folder path.
     */
    public static String CONFIGURATION_FOLDER_PATH = "jarvis.core.configuration.path";

    /**
     * The {@link JarvisCore} instance used to run the bot.
     */
    private static JarvisCore jarvisCore;

    /**
     * Starts the underlying {@link JarvisCore} engine with the
     * {@link org.apache.commons.configuration2.Configuration} retrieved from the provided {@code args}.
     * <p>
     * The provided {@code args} must contain a single value representing the path of the
     * {@link org.apache.commons.configuration2.Configuration} file to use to setup the {@link JarvisCore} engine.
     *
     * @param args the program's arguments
     * @throws NullPointerException     if the provided {@code args} is {@code null}
     * @throws IllegalArgumentException if the provided {@code args} size is different than {@code 1}
     */
    public static void main(String[] args) {
        checkNotNull(args, "Cannot start Jarvis, please provide a parameter containing the path of the Jarvis " +
                "configuration file");
        checkArgument(args.length == 1, "Cannot start Jarvis, please provide a single parameter containing the " +
                "path of the Jarvis configuration file");
        String configurationFilePath = args[0];
        Log.info("Starting jarvis with the configuration file {0}", configurationFilePath);
        File propertiesFile = new File(configurationFilePath);
        try {
            Configurations configs = new Configurations();
            PropertiesConfiguration configuration = configs.properties(propertiesFile);
            configuration.addProperty(CONFIGURATION_FOLDER_PATH, propertiesFile.getParentFile().getAbsolutePath());
            jarvisCore = new JarvisCore(configuration);
        } catch (ConfigurationException e) {
            throw new JarvisException("Cannot load the configuration file", e);
        }
    }

    /**
     * Returns the {@link JarvisCore} instance used to run the bot.
     *
     * @return the {@link JarvisCore} instance used to run the bot
     */
    public static JarvisCore getJarvisCore() {
        return jarvisCore;
    }
}

package fr.zelus.jarvis;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.JarvisException;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;

public class Jarvis {

    public static void main(String[] args) {
        if(args.length == 0) {
            throw new JarvisException("Cannot start jarvis, please provide a configuration file");
        }
        String configurationFilePath = args[0];
        Log.info("Starting jarvis with the configuration file {0}", configurationFilePath);
        File propertiesFile = new File(configurationFilePath);
        try
        {
            Configurations configs = new Configurations();
            PropertiesConfiguration configuration = configs.properties(propertiesFile);
            JarvisCore jarvisCore = new JarvisCore(configuration);
        }
        catch(ConfigurationException e)
        {
            throw new JarvisException("Cannot load the configuration file", e);
        }
    }
}

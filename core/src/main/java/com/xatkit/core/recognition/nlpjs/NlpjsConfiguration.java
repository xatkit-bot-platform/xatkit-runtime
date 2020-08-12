package com.xatkit.core.recognition.nlpjs;

import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.configuration2.Configuration;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;


@Value
public class NlpjsConfiguration {

    public static String NLPJS_SERVER_KEY = "xatkit.nlpjs.server";

    public static String AGENT_ID_KEY = "xatkit.nlpjs.agentId";

    public static String LANGUAGE_CODE_KEY = "xatkit.nlpjs.language";

    public static String DEFAULT_LANGUAGE_CODE = "en";

    public static String CLEAN_AGENT_ON_STARTUP_KEY = "xatkit.nlpjs.clean_on_startup";

    

    Configuration baseConfiguration;

    String languageCode;

    boolean cleanAgentOnStartup;

    String agentId;

    String nlpjsServer;


    public NlpjsConfiguration(@NonNull Configuration baseConfiguration){
        this.baseConfiguration = baseConfiguration;
        checkArgument(baseConfiguration.containsKey(NLPJS_SERVER_KEY), "The provided %s does not contain a value for " +
                "the mandatory property %s", Configuration.class.getSimpleName(), NLPJS_SERVER_KEY);
        checkArgument(baseConfiguration.containsKey(AGENT_ID_KEY), "The provided %s does not contain a value for " +
                "the mandatory property %s", Configuration.class.getSimpleName(), AGENT_ID_KEY);
        this.nlpjsServer = baseConfiguration.getString(NLPJS_SERVER_KEY);
        this.agentId = baseConfiguration.getString(AGENT_ID_KEY);

        if (baseConfiguration.containsKey(LANGUAGE_CODE_KEY)) {
            languageCode = baseConfiguration.getString(LANGUAGE_CODE_KEY);
        } else {
            Log.warn("No language code provided, using the default one ({0})", DEFAULT_LANGUAGE_CODE);
            languageCode = DEFAULT_LANGUAGE_CODE;
        }

        this.cleanAgentOnStartup = baseConfiguration.getBoolean(CLEAN_AGENT_ON_STARTUP_KEY, false);

    }
}

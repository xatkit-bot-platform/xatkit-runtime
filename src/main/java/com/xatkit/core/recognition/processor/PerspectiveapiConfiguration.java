package com.xatkit.core.recognition.processor;

import lombok.Value;
import org.apache.commons.configuration2.Configuration;

@Value
public class PerspectiveapiConfiguration {

    public static String API_KEY = "xatkit.perspectiveapi.apiKey";

    public static String LANGUAGE = "xatkit.perspectiveapi.language";

    public static String DO_NOT_STORE = "xatkit.perspectiveapi.doNotStore";

    public static String CLIENT_TOKEN = "xatkit.perspectiveapi.clientToken";

    public static String SESSION_ID = "xatkit.perspectiveapi.sessionId";

    private String apiKey;

    private String language;

    private boolean doNotStore;

    private String clientToken;

    private String sessionId;

    public PerspectiveapiConfiguration(Configuration baseConfiguration) {
        this.apiKey = baseConfiguration.getString(API_KEY);
        this.language = baseConfiguration.getString(LANGUAGE, "en");
        this.doNotStore = baseConfiguration.getBoolean(DO_NOT_STORE, false);
        this.clientToken = baseConfiguration.getString(CLIENT_TOKEN);
        this.sessionId = baseConfiguration.getString(SESSION_ID);
    }
}

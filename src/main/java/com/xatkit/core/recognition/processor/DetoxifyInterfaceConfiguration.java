package com.xatkit.core.recognition.processor;

import lombok.Value;
import org.apache.commons.configuration2.Configuration;

@Value
public class DetoxifyInterfaceConfiguration {

    public static String DETOXIFY_SERVER_URL = "xatkit.detoxify.server.url";

    private String detoxifyServerUrl;

    public DetoxifyInterfaceConfiguration(Configuration baseConfiguration) {
        String serverUrl = baseConfiguration.getString(DETOXIFY_SERVER_URL);
        if(serverUrl.endsWith("/")) {
            this.detoxifyServerUrl = serverUrl.substring(0, serverUrl.length() - 1);
        } else {
            this.detoxifyServerUrl = serverUrl;
        }
    }
}

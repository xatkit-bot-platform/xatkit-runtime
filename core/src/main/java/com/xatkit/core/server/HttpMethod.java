package com.xatkit.core.server;

/**
 * Utility enum holding supported Http methods.
 */
public enum HttpMethod {

    GET("GET"),
    POST("POST"),
    DELETE("DELETE"),
    HEAD("HEAD"),
    OPTIONS("OPTIONS"),
    PATCH("PATCH"),
    PUT("PUT"),
    TRACE("TRACE");

    /**
     * The literal of the enum value.
     */
    public final String label;

    /**
     * Constructs a new enum value with the provided {@code label}.
     *
     * @param label the label of the enum value
     */
    HttpMethod(String label) {
        this.label = label;
    }

}

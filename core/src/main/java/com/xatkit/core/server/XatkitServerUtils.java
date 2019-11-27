package com.xatkit.core.server;

import org.apache.commons.configuration2.Configuration;

/**
 * Utility values for the {@link XatkitServer}.
 * <p>
 * This interface contains the {@link Configuration} keys and default values used by the {@link XatkitServer} and its
 * internal components.
 */
public interface XatkitServerUtils {

    /**
     * The {@link Configuration} key to store the server port to use.
     */
    String SERVER_PORT_KEY = "xatkit.server.port";

    /**
     * The default port to use.
     * <p>
     * The server port can be customized in the constructor's {@link Configuration} using the {@link #SERVER_PORT_KEY}
     * key.
     */
    int DEFAULT_SERVER_PORT = 5000;

    /**
     * The {@link Configuration} key used to specify the Xatkit server location (public URL).
     * <p>
     * This key is used to customize HTML templates and Javascript to connect them to the running
     * {@link XatkitServer} instance.
     *
     * @see #DEFAULT_SERVER_LOCATION
     */
    String SERVER_PUBLIC_URL_KEY = "xatkit.server.public_url";

    /**
     * The default Xatkit server location.
     * <p>
     * This value is used if the {@link Configuration} does not contain a server public url, and allows to test bots in
     * a local development environment by connecting to {@code http://localhost:5000}.
     *
     * @see #SERVER_PUBLIC_URL_KEY
     */
    String DEFAULT_SERVER_LOCATION = "http://localhost";

    /**
     * The URL fragment used to access Xatkit public content.
     */
    String PUBLIC_CONTENT_URL_FRAGMENT = "/content/";

    /**
     * The directory name used to store Xatkit public content.
     */
    String PUBLIC_DIRECTORY_NAME = "public";

    /**
     * The {@link Configuration} key used to specify the location of the keystore to create the SSL context from.
     * <p>
     * This property can contain an absolute path to the keystore, or a relative path that will be resolved from the
     * location of the configuration file.
     */
    String SERVER_KEYSTORE_LOCATION_KEY = "xatkit.server.ssl.keystore";

    /**
     * The {@link Configuration} key used to specify the {@code store password} of the SSL keystore.
     */
    String SERVER_KEYSTORE_STORE_PASSWORD_KEY = "xatkit.server.ssl.keystore.store.password";

    /**
     * The {@link Configuration} key used to specify the {@code key password} of the SSL keystore.
     * <p>
     * The value of this property is usually equal to {@link #SERVER_KEYSTORE_STORE_PASSWORD_KEY}.
     */
    String SERVER_KEYSTORE_KEY_PASSWORD_KEY = "xatkit.server.ssl.keystore.key.password";
}

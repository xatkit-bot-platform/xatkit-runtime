package fr.zelus.jarvis.plugins.wordpress;

/**
 * An utility interface that holds WordPress-related helpers.
 * <p>
 * This class defines the jarvis configuration keys to store the WordPress <i>base_url</i>, <i>username</i>, and
 * <i>password</i>, as well as a set of {@link fr.zelus.jarvis.core.session.JarvisContext} keys that can be used to
 * retrieve WordPress-related information.
 *
 * @see fr.zelus.jarvis.core.session.JarvisContext
 */
public interface JarvisWordPressUtils {

    /**
     * The {@link org.apache.commons.configuration2.Configuration} key to store the WordPress <i>base_url</i>.
     */
    String WORDPRESS_BASE_URL_KEY = "jarvis.wordpress.base_url";

    /**
     * The {@link org.apache.commons.configuration2.Configuration} key to store the WordPress <i>username</i>.
     */
    String WORDPRESS_USERNAME_KEY = "jarvis.wordpress.username";

    /**
     * The {@link org.apache.commons.configuration2.Configuration} key to store the WordPress <i>password</i>.
     */
    String WORDPRESS_PASSWORD_KEY = "jarvis.wordpress.password";

    /**
     * The {@link fr.zelus.jarvis.core.session.JarvisContext} key used to store wordpress-related information.
     */
    String WORDPRESS_CONTEXT_KEY = "wordpress";

    /**
     * The {@link fr.zelus.jarvis.core.session.JarvisContext} variable key used to store post title information.
     */
    String WORDPRESS_POST_TITLE_CONTEXT_KEY = "title";

    /**
     * The {@link fr.zelus.jarvis.core.session.JarvisContext} variable key used to store post updated date information.
     */
    String WORDPRESS_POST_UPDATED_CONTEXT_KEY = "updated";

    /**
     * The {@link fr.zelus.jarvis.core.session.JarvisContext} variable key used to store post link information.
     */
    String WORDPRESS_POST_LINK_CONTEXT_KEY = "link";
}

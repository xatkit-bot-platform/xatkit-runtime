package fr.zelus.jarvis.plugins.wordpress.module.io;

import com.afrozaar.wordpress.wpapi.v2.Wordpress;
import com.afrozaar.wordpress.wpapi.v2.config.ClientConfig;
import com.afrozaar.wordpress.wpapi.v2.config.ClientFactory;
import com.afrozaar.wordpress.wpapi.v2.model.Post;
import com.afrozaar.wordpress.wpapi.v2.request.Request;
import com.afrozaar.wordpress.wpapi.v2.request.SearchRequest;
import com.afrozaar.wordpress.wpapi.v2.response.PagedResponse;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.*;
import fr.zelus.jarvis.io.EventProvider;
import fr.zelus.jarvis.plugins.wordpress.JarvisWordPressUtils;
import org.apache.commons.configuration2.Configuration;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class WordPressEventProvider extends EventProvider {

    private String baseUrl;

    private String username;

    private String password;

    private Wordpress client;

    private final DateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private final DateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Date lastUpdateDate;

    public WordPressEventProvider(JarvisCore jarvisCore, Configuration configuration) {
        super(jarvisCore, configuration);
        checkNotNull(configuration, "Cannot construct a %s from a null configuration", this.getClass().getSimpleName());
        baseUrl = configuration.getString(JarvisWordPressUtils.WORDPRESS_BASE_URL_KEY);
        checkArgument(nonNull(baseUrl) && !baseUrl.isEmpty(), "Cannot construct a %s from the provided base URL: " +
                        "%s, please ensure that the jarvis configuration contains a valid base URL associated to the " +
                        "key %s",
                this.getClass().getSimpleName(), baseUrl, JarvisWordPressUtils.WORDPRESS_BASE_URL_KEY);
        username = configuration.getString(JarvisWordPressUtils.WORDPRESS_USERNAME_KEY);
        if (isNull(username)) {
            Log.warn("No username provided to configure the %s, REST API access may be restricted (use the %s " +
                            "property" +
                            " in the jarvis configuration to set a username", this.getClass().getSimpleName(),
                    JarvisWordPressUtils.WORDPRESS_USERNAME_KEY);
        }
        password = configuration.getString(JarvisWordPressUtils.WORDPRESS_PASSWORD_KEY);
        if (isNull(password)) {
            Log.warn("No password provided to configure the %s, REST API access may be restricted (use the %s " +
                            "property in the jarvis configuration to set a password", this.getClass().getSimpleName(),
                    JarvisWordPressUtils.WORDPRESS_PASSWORD_KEY);
        }

        client = ClientFactory.fromConfig(ClientConfig.of(baseUrl, username, password, false,
                false));
        Log.info("WordPress client started");

        /*
         * Use the current date as the first reference date, we don't need to retrieve the last update date since we
         * only log new updates from now.
         */
        this.lastUpdateDate = new Date();
    }

    @Override
    public void run() {
        while (true) {
            List<Post> lastUpdatedPosts = getLastUpdatedPosts();
            for (Post p : lastUpdatedPosts) {
                Date postDate = getPostUpdateDate(p);
                if (postDate.after(lastUpdateDate)) {
                    Log.info("Found a new updated post: \"{0}\" ({1})", p.getTitle(), outputDateFormat.format
                            (postDate));
                    lastUpdateDate = getPostUpdateDate(p);
                    JarvisSession session = jarvisCore.getOrCreateJarvisSession(JarvisWordPressUtils
                            .WORDPRESS_CONTEXT_KEY);
                    EventDefinition eventDefinition = jarvisCore.getEventDefinitionRegistry().getEventDefinition
                            ("UpdatedPost");
                    EventInstance eventInstance = IntentFactory.eINSTANCE.createEventInstance();
                    eventInstance.setDefinition(eventDefinition);

                    ContextParameterValue titleValue = IntentFactory.eINSTANCE.createContextParameterValue();
                    titleValue.setContextParameter(getContextParameter(eventDefinition, JarvisWordPressUtils
                            .WORDPRESS_POST_TITLE_CONTEXT_KEY));
                    titleValue.setValue(p.getTitle().getRendered());
                    eventInstance.getOutContextValues().add(titleValue);

                    ContextParameterValue updatedValue = IntentFactory.eINSTANCE.createContextParameterValue();
                    updatedValue.setContextParameter(getContextParameter(eventDefinition, JarvisWordPressUtils
                            .WORDPRESS_POST_UPDATED_CONTEXT_KEY));
                    updatedValue.setValue(outputDateFormat.format(postDate));
                    eventInstance.getOutContextValues().add(updatedValue);

                    ContextParameterValue linkValue = IntentFactory.eINSTANCE.createContextParameterValue();
                    linkValue.setContextParameter(getContextParameter(eventDefinition, JarvisWordPressUtils
                            .WORDPRESS_POST_LINK_CONTEXT_KEY));
                    linkValue.setValue(p.getLink());
                    eventInstance.getOutContextValues().add(linkValue);

                    jarvisCore.handleEvent(eventInstance, session);
                    break; // TODO support multiple updates
                }
            }
            synchronized (this) {
                try {
                    /*
                     * Look for new updated posts every minute.
                     */
                    wait(1000 * 60);
                } catch (InterruptedException e) {
                    /*
                     * The current thread can be interrupted when the containing JarvisModule is shutdown.
                     */
                    break;
                }
            }
        }
    }

    private ContextParameter getContextParameter(EventDefinition eventDefinition, String parameterName) {
        for (Context c : eventDefinition.getOutContexts()) {
            for (ContextParameter cp : c.getParameters()) {
                if (cp.getName().equals(parameterName)) {
                    return cp;
                }
            }
        }
        throw new RuntimeException("Cannot find the parameter " + parameterName);
    }

    /**
     * Returns the update {@link Date} of the provided {@code post}.
     *
     * @param post the {@link Post} to retrieve the update {@link Date} of
     * @return the update {@link Date} of the provided {@code post} or {@code null} if an error occurred when parsing
     * the {@link Post}'s {@link Date}.
     */
    private Date getPostUpdateDate(Post post) {
        Date postDate;
        try {
            postDate = inputDateFormat.parse(post.getModified());
        } catch (ParseException e) {
            return null;
        }
        return postDate;
    }

    /**
     * Returns the last updated {@link Post}s.
     * <p>
     * This method relies on the underlying {@link Wordpress} client to connect and query the WordPress REST API.
     * Note that this method does not require an authentication.
     *
     * @return the last updated {@link Post}s
     */
    private List<Post> getLastUpdatedPosts() {
        final PagedResponse<Post> response = client.search(SearchRequest.Builder.aSearchRequest(Post.class)
                .withUri(Request.POSTS)
                .withOrderBy("modified")
                .build());
        return response.getList();
    }
}

package com.xatkit.core.server;

import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.io.WebhookEventProvider;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Locale;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

/**
 * Handles the input requests and notifies the {@link WebhookEventProvider}s.
 */
class AdminHttpHandler implements HttpRequestHandler {

    /**
     * The pattern used to match and replace the server location template in the HTML file.
     */
    // Need to double single quotes, see https://docs.oracle.com/javase/7/docs/api/java/text/MessageFormat.html
    private static final String SERVER_LOCATION_PATTERN = "window.xatkit_server = ''{0}''";

    /**
     * The placeholder used for the server location in the HTML file.
     */
    private static final String SERVER_LOCATION_PLACEHOLDER =
            XatkitServerUtils.DEFAULT_SERVER_LOCATION + ":" + Integer.toString(5001);
    // TODO This handler should be move to the react platform, where the default react port is defined.

    /**
     * The Xatkit server location (public URL and port) to use from the returned HTML and Javascript files.
     *
     * @see XatkitServerUtils#SERVER_PUBLIC_URL_KEY
     * @see XatkitServerUtils#DEFAULT_SERVER_LOCATION
     */
    private String fullServerLocation;

    /**
     * Constructs an {@link AdminHttpHandler} with the provided {@code configuration}.
     * <p>
     * The {@link Configuration} can contain an optional key {@code xatkit.server.public_url} that specifies the Xatkit
     * server's public URL to use in the returned HTML and Javascript files.
     *
     * @param configuration the {@link Configuration} to use
     */
    public AdminHttpHandler(Configuration configuration) {
        super();
        checkNotNull(configuration, "Cannot construct a %s with the provided %s %s", this.getClass().getSimpleName(),
                Configuration.class.getSimpleName(), configuration);
        // TODO This handler should probably go in the react platform.
        int port = configuration.getInt("xatkit.react.port", 5001);
        String serverLocation = configuration.getString(XatkitServerUtils.SERVER_PUBLIC_URL_KEY,
                XatkitServerUtils.DEFAULT_SERVER_LOCATION);
        this.fullServerLocation = serverLocation + ":" + Integer.toString(port);
    }

    /**
     * Handles the received {@code request} and fill the provided {@code response}.
     * <p>
     * This method is triggered when a request is received on the {@code /admin*} endpoint, and serves the
     * administration-related htmp, css, and javascript files.
     * <p>
     * If the {@code xatkit.server.public_url} key is defined in the {@link Configuration} this method will replace the
     * template server location by the provided ones.
     *
     * @param request  the received {@link HttpRequest}
     * @param response the {@link HttpResponse} to send to the caller
     * @param context  the {@link HttpContext} associated to the received {@link HttpRequest}
     * @see XatkitServer#notifyWebhookEventProviders(String, Object, Header[])
     */
    public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) {

        String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
        String target = request.getRequestLine().getUri();

        Log.info("Received a {0} query on {1}", method, target);

        /*
         * Ignore the parameters, they are not used for now.
         */

        if (method.equals("GET")) {
            if (target.equals("/admin")) {
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("admin/admin.html");
                if (isNull(is)) {
                    Log.error("Cannot return the admin/admin.html page not found");
                    response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                    return;
                }
                BasicHttpEntity entity = new BasicHttpEntity();
                InputStream entityContent = replaceHtmlTemplates(is);
                entity.setContent(entityContent);
                entity.setContentType(ContentType.TEXT_HTML.getMimeType());
                entity.setContentEncoding(HTTP.UTF_8);
                response.setEntity(entity);
                response.setStatusCode(HttpStatus.SC_OK);
                return;
            }

            if (target.startsWith("/admin/js/") || target.startsWith("/admin/css/")) {
                String targetPath = target.substring(1);
                InputStream is = this.getClass().getClassLoader().getResourceAsStream(targetPath);
                if (isNull(is)) {
                    Log.error("Cannot return the resource at {0}", targetPath);
                    response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                    return;
                }
                BasicHttpEntity entity = new BasicHttpEntity();
                entity.setContent(is);
                if (targetPath.endsWith(".css")) {
                    entity.setContentType("text/css");
                } else if (targetPath.endsWith(".js")) {
                    entity.setContentType("application/javascript");
                }
                entity.setContentEncoding(HTTP.UTF_8);
                response.setEntity(entity);
                response.setStatusCode(HttpStatus.SC_OK);
                return;
            }
        }
    }

    /**
     * Replaces the templates of the provided {@code from} {@link InputStream}.
     * <p>
     * This method creates a new {@link InputStream} containing {@code from}'s contents with its template replaced.
     *
     * @param from the {@link InputStream} to replace the templates from
     * @return an {@link InputStream} containing {@code from}'s contents with its templates replaced
     * @see XatkitServerUtils#SERVER_PUBLIC_URL_KEY
     */
    private InputStream replaceHtmlTemplates(InputStream from) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(from));
        StringBuilder builder = new StringBuilder();
        try {
            while (reader.ready()) {
                builder.append(reader.readLine());
            }
        } catch (IOException e) {
            throw new XatkitException(MessageFormat.format("An error occurred when replacing templates in {0}, see " +
                    "attached exception", this.getClass().getSimpleName()), e);
        }
        String content = builder.toString();
        content = content.replace(MessageFormat.format(SERVER_LOCATION_PATTERN, SERVER_LOCATION_PLACEHOLDER),
                MessageFormat.format(SERVER_LOCATION_PATTERN, fullServerLocation));
        try {
            return new ByteArrayInputStream(content.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new XatkitException(MessageFormat.format("Cannot create an {0} from the provided {1}: {2}, see " +
                            "attached exception", InputStream.class.getSimpleName(), String.class.getSimpleName(),
                    content), e);
        }
    }
}

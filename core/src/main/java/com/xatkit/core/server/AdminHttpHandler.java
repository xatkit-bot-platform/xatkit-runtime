package com.xatkit.core.server;

import com.xatkit.core.XatkitException;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

/**
 * A handler bound to {@code /admin} that allows to quickly test web-based bots.
 */
class AdminHttpHandler implements HttpRequestHandler {

    /**
     * The pattern used to print the server location in the rendered {@code admin.html} file.
     */
    private static final String SERVER_LOCATION_PATTERN = "#xatkit\\.server";

    /**
     * The pattern used to print the username in the rendered {@code admin.html} file.
     * <p>
     * This pattern is replaced with a random name from {@link #TEST_CLIENT_NAMES} when the page is displayed to
     * simulate multiple users.
     */
    private static final String USERNAME_PATTERN = "#xatkit\\.username";

    /**
     * A {@link List} of test client names used to render the {@code /admin} page and simulate multiple users.
     *
     * @see #TEMPLATE_FILLED_COUNT
     */
    private static List<String> TEST_CLIENT_NAMES = Arrays.asList("Bob", "Alice", "Gwendal", "Jordi");

    /**
     * A counter used to render the {@code /admin} page with random client names.
     *
     * @see #TEST_CLIENT_NAMES
     */
    private static int TEMPLATE_FILLED_COUNT = 0;

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
    private String reactServerURL;

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
        int reactPort = configuration.getInt("xatkit.react.port", 5001);
        String reactBasePath = configuration.getString("xatkit.react.base_path", "/socket.io");
        String serverLocation = configuration.getString(XatkitServerUtils.SERVER_PUBLIC_URL_KEY,
                XatkitServerUtils.DEFAULT_SERVER_LOCATION);
        this.reactServerURL = serverLocation + ":" + Integer.toString(reactPort) + reactBasePath;
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
     */
    public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) {

        String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
        String path = null;
        try {
            path = HttpUtils.getPath(request);
        } catch(URISyntaxException e) {
            throw new XatkitException(MessageFormat.format("Cannot parse the provided URI {0}, see attached exception",
                    request.getRequestLine().getUri()), e);
        }
        Log.info("Received a {0} query on {1}", method, path);

        /*
         * Ignore the parameters, they are not used for now.
         */

        if (Objects.equals(method, "GET")) {
            /*
             * Use Objects.equals(): the method/target may be null
             */
            if (Objects.equals(path, "/admin")) {
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
                entity.setContentEncoding(StandardCharsets.UTF_8.name());
                response.setEntity(entity);
                response.setStatusCode(HttpStatus.SC_OK);
                return;
            }

            if (path.startsWith("/admin/js/") || path.startsWith("/admin/css/")) {
                String targetPath = path.substring(1);
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
                entity.setContentEncoding(StandardCharsets.UTF_8.name());
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
     * @see #SERVER_LOCATION_PATTERN
     * @see #USERNAME_PATTERN
     */
    private InputStream replaceHtmlTemplates(InputStream from) {
        TEMPLATE_FILLED_COUNT++;
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
        content = content.replaceAll(SERVER_LOCATION_PATTERN, reactServerURL);
        String clientName = TEST_CLIENT_NAMES.get(TEMPLATE_FILLED_COUNT % TEST_CLIENT_NAMES.size());
        content = content.replaceAll(USERNAME_PATTERN, clientName);
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}

package com.xatkit.core.server;

import com.xatkit.core.platform.io.WebhookEventProvider;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.InputStream;
import java.util.Locale;

import static java.util.Objects.isNull;

/**
 * Handles the input requests and notifies the {@link WebhookEventProvider}s.
 */
class AdminHttpHandler implements HttpRequestHandler {

    public AdminHttpHandler() {
        super();
    }

    /**
     * Handles the received {@code request} and fill the provided {@code response}.
     * <p>
     * This method is triggered when a request is received on the {@code /admin*} endpoint, and serves the
     * administration-related htmp, css, and javascript files.
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
                entity.setContent(is);
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
}

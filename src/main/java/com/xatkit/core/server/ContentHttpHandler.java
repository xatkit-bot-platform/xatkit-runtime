package com.xatkit.core.server;

import fr.inria.atlanmod.commons.log.Log;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;

import static java.util.Objects.nonNull;

/**
 * Handles the HTTP requests performed on the {@code /content/} path.
 * <p>
 * This handler is designed to server public files. It only accepts GET requests, and do not take into account query
 * parameters.
 * <p>
 * Accessible files are stored in the {@code <public/>} directory, and can be accessed using the following URL
 * template: {@code <baseURL/content/path>}.
 */
public class ContentHttpHandler implements HttpRequestHandler {

    /**
     * The {@link XatkitServer} managing this handler.
     */
    private XatkitServer xatkitServer;

    /**
     * Constructs a new {@link ContentHttpHandler} managed by the provided {@code xatkitServer}.
     *
     * @param xatkitServer the {@link XatkitServer} managing this handler.
     */
    public ContentHttpHandler(XatkitServer xatkitServer) {
        super();
        this.xatkitServer = xatkitServer;
    }

    /**
     * Handles the received {@code request} and fill the provided {@code response} with the retrieved {@link File} to
     * serve.
     * <p>
     * This method relies on {@link XatkitServer#getPublicFile(String)} to retrieve the public file associated to the
     * request target. If this {@link File} doesn't exist or the accessed location is illegal the response is set to
     * {@link HttpStatus#SC_NOT_FOUND}.
     *
     * @param request  the received {@link HttpRequest}
     * @param response the {@link HttpResponse} to send to the caller
     * @param context  the {@link HttpContext} associated to the received {@link HttpRequest}
     */
    @Override
    public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) {

        String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
        String target = request.getRequestLine().getUri();

        Log.debug("{0} - Received a {1} query on {2}", this.getClass().getSimpleName(), method, target);

        /*
         * Ignore the parameters, they are not used for now.
         */
        if (method.equals("GET")) {
            if (target.startsWith(XatkitServerUtils.PUBLIC_CONTENT_URL_FRAGMENT)) {
                String filePath = target.replaceFirst(XatkitServerUtils.PUBLIC_CONTENT_URL_FRAGMENT, "");
                Log.debug("File Path: {0}", filePath);
                File file = xatkitServer.getPublicFile(filePath);
                if (nonNull(file) && file.exists() && !file.isDirectory()) {
                    FileInputStream fis;
                    try {
                        fis = new FileInputStream(file);
                    } catch (IOException e) {
                        Log.error("{0} Cannot retrieve the file {1}", this.getClass().getSimpleName(),
                                file.getAbsolutePath());
                        response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                        return;
                    }
                    BasicHttpEntity entity = new BasicHttpEntity();
                    entity.setContent(fis);
                    response.setEntity(entity);
                    response.setStatusCode(HttpStatus.SC_OK);
                    return;
                }
            }
        }
        Log.error("Cannot server content {0}, unsupported method {1}", target, method);
        response.setStatusCode(HttpStatus.SC_NOT_FOUND);
    }
}

package fr.zelus.jarvis.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisException;
import org.apache.http.*;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.nonNull;

public class JarvisServer {

    private static String JSON_APPLICATION_CONTENT_TYPE = "application/json";

    private int portNumber;

    private HttpServer server;

    public JarvisServer() {
        Log.info("Starting JarvisServer");
        this.portNumber = 5000;
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();

        server = ServerBootstrap.bootstrap()
                .setListenerPort(portNumber)
                .setServerInfo("Test/1.1")
                .setSocketConfig(socketConfig)
                .registerHandler("*", new HttpHandler())
                .create();
        try {
            server.start();
        } catch(IOException e) {
            throw new JarvisException("Cannot start the JarvisServer", e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Log.info("Shutting down JarvisServer (shutdown hook)");
            server.shutdown(5, TimeUnit.SECONDS);
        }));
        Log.info("JarvisServer started, listening on localhost:{0}", portNumber);
    }

    public void shutdown() {
        Log.info("Shutting down JarvisServer");
        server.shutdown(5, TimeUnit.SECONDS);
    }


    static class HttpHandler implements HttpRequestHandler {

        private JsonParser parser;

        private Gson gson;

        public HttpHandler() {
            super();
            parser = new JsonParser();
            gson = new GsonBuilder().setPrettyPrinting().create();
        }

        public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) throws
                IOException {

            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
            String target = request.getRequestLine().getUri();

            Log.info("Received a {0} query on {1}", method, target);

            List<NameValuePair> parameters = null;
            try {
                parameters = URLEncodedUtils.parse(new URI(target), HTTP.UTF_8);
            } catch (URISyntaxException e) {
                String errorMessage = MessageFormat.format("Cannot parse the requested URI {0}", target);
                throw new JarvisException(errorMessage);
            }

            for (NameValuePair parameter : parameters) {
                Log.info("Query parameter: {0} = {1}", parameter.getName(), parameter.getValue());
            }

            if (request instanceof HttpEntityEnclosingRequest) {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                String contentEncoding = null;
                Header encodingHeader = entity.getContentEncoding();
                if (nonNull(encodingHeader) && encodingHeader.getElements().length > 0) {
                    contentEncoding = encodingHeader.getElements()[0].getName();
                    Log.info("Query content encoding: {0}", contentEncoding);
                } else {
                    Log.warn("Unknown query content encoding");
                }
                String contentType = null;
                Header contentTypeHeader = entity.getContentType();
                if (nonNull(contentTypeHeader) && contentTypeHeader.getElements().length > 0) {
                    contentType = contentTypeHeader.getElements()[0].getName();
                    Log.info("Query content type: {0}", contentType);
                } else {
                    Log.warn("Unknown query content type");
                }
                Long contentLength = entity.getContentLength();
                Log.info("Query content length: {0}", contentLength);

                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
                StringBuilder contentBuilder = new StringBuilder();
                while (reader.ready()) {
                    contentBuilder.append(reader.readLine());
                }
                String content = contentBuilder.toString();
                if (content.isEmpty()) {
                    Log.warn("Empty query content");
                } else {
                    if (JSON_APPLICATION_CONTENT_TYPE.equals(contentType)) {
                        Log.info("Parsing {0} content", JSON_APPLICATION_CONTENT_TYPE);
                        JsonElement jsonElement = parser.parse(content);
                        Log.info("Query content: \n {0}", gson.toJson(jsonElement));
                    } else {
                        Log.info("No parser for the provided content type {0}, returning the raw content: \n {1}",
                                contentType, content);
                    }
                }
            }
            response.setStatusCode(HttpStatus.SC_OK);

        }

    }

}

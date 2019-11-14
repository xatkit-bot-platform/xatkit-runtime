package com.xatkit.stubs;

import com.xatkit.core.server.HttpMethod;
import com.xatkit.core.server.XatkitServer;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.http.Header;
import org.apache.http.NameValuePair;

import javax.annotation.Nullable;
import java.util.List;

public class StubXatkitServer extends XatkitServer {

    private HttpMethod lastIsRestEndpointMethod;

    private String lastIsRestEndpointURI;

    private HttpMethod lastNotifyRestHandlerMethod;

    private String lastNotifyRestHandlerURI;

    private List<Header> lastNotifyRestHandlerHeaders;

    private List<NameValuePair> lastNotifyRestHandlerParams;

    private Object lastNotifyRestHandlerContent;

    private String lastNotifyRestHandlerContentType;

    public StubXatkitServer() {
        super(new BaseConfiguration());
    }

    @Override
    public boolean isRestEndpoint(HttpMethod httpMethod, String uri) {
        this.lastIsRestEndpointMethod = httpMethod;
        this.lastIsRestEndpointURI = uri;
        return super.isRestEndpoint(httpMethod, uri);
    }

    @Override
    public Object notifyRestHandler(HttpMethod httpMethod, String uri, List<Header> headers, List<NameValuePair> params,
                                    @Nullable Object content, String contentType) {
        this.lastNotifyRestHandlerMethod = httpMethod;
        this.lastNotifyRestHandlerURI = uri;
        this.lastNotifyRestHandlerHeaders = headers;
        this.lastNotifyRestHandlerParams = params;
        this.lastNotifyRestHandlerContent = content;
        this.lastNotifyRestHandlerContentType = contentType;
        return super.notifyRestHandler(httpMethod, uri, headers, params, content, contentType);
    }

    public HttpMethod getLastIsRestEndpointMethod() {
        return this.lastIsRestEndpointMethod;
    }

    public String getLastIsRestEndpointURI() {
        return this.lastIsRestEndpointURI;
    }

    public HttpMethod getLastNotifyRestHandlerMethod() {
        return this.lastNotifyRestHandlerMethod;
    }

    public String getLastNotifyRestHandlerURI() {
        return this.lastNotifyRestHandlerURI;
    }

    public List<Header> getLastNotifyRestHandlerHeaders() {
        return this.lastNotifyRestHandlerHeaders;
    }

    public List<NameValuePair> getLastNotifyRestHandlerParams() {
        return this.lastNotifyRestHandlerParams;
    }

    public Object getLastNotifyRestHandlerContent() {
        return this.lastNotifyRestHandlerContent;
    }

    public String getLastNotifyRestHandlerContentType() {
        return this.lastNotifyRestHandlerContentType;
    }

    public void clean() {
        this.lastIsRestEndpointMethod = null;
        this.lastIsRestEndpointURI = null;
        this.lastNotifyRestHandlerMethod = null;
        this.lastNotifyRestHandlerURI = null;
        this.lastNotifyRestHandlerHeaders = null;
        this.lastNotifyRestHandlerParams = null;
        this.lastNotifyRestHandlerContent = null;
        this.lastNotifyRestHandlerContentType = null;
    }
}

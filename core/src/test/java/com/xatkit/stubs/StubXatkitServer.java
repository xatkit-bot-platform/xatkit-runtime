package com.xatkit.stubs;

import com.xatkit.core.server.RestHandler;
import com.xatkit.core.server.XatkitServer;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.http.Header;
import org.apache.http.NameValuePair;

import javax.annotation.Nullable;
import java.util.List;

public class StubXatkitServer extends XatkitServer {

    private String lastIsRestEndpointURI;

    private String lastNotifyRestHandlerURI;

    private List<Header> lastNotifyRestHandlerHeaders;

    private List<NameValuePair> lastNotifyRestHandlerParams;

    private Object lastNotifyRestHandlerContent;

    private String lastNotifyRestHandlerContentType;

    public StubXatkitServer() {
        super(new BaseConfiguration());
    }

    @Override
    public boolean isRestEndpoint(String uri) {
        this.lastIsRestEndpointURI = uri;
        return super.isRestEndpoint(uri);
    }

    @Override
    public Object notifyRestHandler(String uri, List<Header> headers, List<NameValuePair> params, @Nullable Object content, String contentType) {
        this.lastNotifyRestHandlerURI = uri;
        this.lastNotifyRestHandlerHeaders = headers;
        this.lastNotifyRestHandlerParams = params;
        this.lastNotifyRestHandlerContent = content;
        this.lastNotifyRestHandlerContentType = contentType;
        return super.notifyRestHandler(uri, headers, params, content, contentType);
    }

    public String getLastIsRestEndpointURI() {
        return this.lastIsRestEndpointURI;
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
        this.lastIsRestEndpointURI = null;
        this.lastNotifyRestHandlerURI = null;
        this.lastNotifyRestHandlerHeaders = null;
        this.lastNotifyRestHandlerParams = null;
        this.lastNotifyRestHandlerContent = null;
        this.lastNotifyRestHandlerContentType = null;
    }
}

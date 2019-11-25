package com.xatkit.core.server;

import com.xatkit.AbstractXatkitTest;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicNameValuePair;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpUtilsTest extends AbstractXatkitTest {

    private static String VALID_HTTP_REQUEST_PATH = "/target";

    private static HttpRequest VALID_HTTP_REQUEST;

    private static HttpRequest INVALID_HTTP_REQUEST;

    private static List<NameValuePair> TEST_PARAMETERS = Arrays.asList(new BasicNameValuePair("param1", "abc"),
            new BasicNameValuePair("param2", "def"));

    @BeforeClass
    public static void setUpBeforeClass() {
        VALID_HTTP_REQUEST = new BasicHttpRequest("GET", VALID_HTTP_REQUEST_PATH);
        INVALID_HTTP_REQUEST = new BasicHttpRequest("GET", "i\nvalid");
    }

    @Test(expected = NullPointerException.class)
    public void getURIBuilderFromNullRequest() throws URISyntaxException {
        HttpUtils.getURIBuilderFrom(null);
    }

    @Test(expected = URISyntaxException.class)
    public void getURIBuilderFromInvalidURI() throws URISyntaxException {
        HttpUtils.getURIBuilderFrom(INVALID_HTTP_REQUEST);
    }

    @Test
    public void getURIBuilderFromValidURI() throws URISyntaxException {
        URIBuilder uriBuilder = HttpUtils.getURIBuilderFrom(VALID_HTTP_REQUEST);
        assertThat(uriBuilder.getPath()).as("Builder contains the correct path").isEqualTo(VALID_HTTP_REQUEST_PATH);
    }

    @Test(expected = NullPointerException.class)
    public void getPathNullRequest() throws URISyntaxException {
        HttpUtils.getPath(null);
    }

    @Test(expected = URISyntaxException.class)
    public void getPathInvalidURI() throws URISyntaxException {
        HttpUtils.getPath(INVALID_HTTP_REQUEST);
    }

    @Test
    public void getPathValidURI() throws URISyntaxException {
        String path = HttpUtils.getPath(VALID_HTTP_REQUEST);
        assertThat(path).as("Correct path").isEqualTo(VALID_HTTP_REQUEST_PATH);
    }

    @Test(expected = NullPointerException.class)
    public void getParametersNullRequest() throws URISyntaxException {
        HttpUtils.getParameters(null);
    }

    @Test(expected = URISyntaxException.class)
    public void getParametersInvalidURI() throws URISyntaxException {
        HttpUtils.getParameters(INVALID_HTTP_REQUEST);
    }

    @Test
    public void getParametersValidURINoParameters() throws URISyntaxException {
        List<NameValuePair> parameters = HttpUtils.getParameters(VALID_HTTP_REQUEST);
        assertThat(parameters).as("Parameter list is empty").isEmpty();
    }

    @Test
    public void getParametersValidURIWithParameters() throws URISyntaxException {
        List<NameValuePair> parameters = HttpUtils.getParameters(new BasicHttpRequest("GET",
                VALID_HTTP_REQUEST_PATH + "?param1=abc&param2=def"));
        assertThat(parameters).as("Parameters list contains two parameters").hasSize(2);
        assertThat(parameters.stream()).as("Parameter list contains param1").anyMatch(p -> p.getName().equals("param1"
        ) && p.getValue().equals("abc"));
        assertThat(parameters.stream()).as("Parameter list contains param2").anyMatch(p -> p.getName().equals("param2"
        ) && p.getValue().equals("def"));
    }

    @Test(expected = NullPointerException.class)
    public void getParameterValueNullName() {
        HttpUtils.getParameterValue(null, TEST_PARAMETERS);
    }

    @Test(expected = NullPointerException.class)
    public void getParameterValueNullParameters() {
        HttpUtils.getParameterValue("param1", null);
    }

    @Test
    public void getParameterValueExistingParameter() {
        String value = HttpUtils.getParameterValue("param1", TEST_PARAMETERS);
        assertThat(value).as("Valid parameter value").isEqualTo("abc");
    }

    @Test
    public void getParameterValueNotExistingParameter() {
        String value = HttpUtils.getParameterValue("invalid", TEST_PARAMETERS);
        assertThat(value).as("Parameter value is null").isNull();
    }

}

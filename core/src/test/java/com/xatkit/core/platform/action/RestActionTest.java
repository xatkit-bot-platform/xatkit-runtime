package com.xatkit.core.platform.action;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.XatkitCore;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.stubs.StubXatkitCore;
import com.xatkit.stubs.StubRuntimePlatform;
import org.apache.commons.configuration2.BaseConfiguration;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/*
 * This class is abstract: RestActions are only tested through their concrete subclasses.
 */
public abstract class RestActionTest extends AbstractXatkitTest {

    protected static XatkitCore XATKIT_CORE;

    protected static StubRuntimePlatform RUNTIME_PLATFORM;

    /*
     * We use https://jsonplaceholder.typicode.com/ to test the REST actions: it provides sample JSON data accessible
     * through a simple REST API.
     */
    protected static String VALID_GET_ENDPOINT = "https://jsonplaceholder.typicode.com/todos/1";

    protected static String VALID_POST_ENDPOINT = "https://jsonplaceholder.typicode.com/posts";

    protected static JsonElement VALID_POST_BODY;

    static {
        VALID_POST_BODY = new JsonParser().parse("{\n" +
                "      title: 'foo',\n" +
                "      body: 'bar',\n" +
                "      userId: 1\n" +
                "    }");
    }

    protected XatkitSession session;

    @BeforeClass
    public static void setUpBeforeClass() {
        XATKIT_CORE = new StubXatkitCore();
        RUNTIME_PLATFORM = new StubRuntimePlatform(XATKIT_CORE, new BaseConfiguration());
    }

    @AfterClass
    public static void tearDownAfterClass() {
        RUNTIME_PLATFORM.shutdown();
        XATKIT_CORE.shutdown();
    }

    @Before
    public void setUp() {
        this.session = new XatkitSession("sessionID");
    }
}

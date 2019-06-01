package edu.uoc.som.jarvis.core.platform.action;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import edu.uoc.som.jarvis.AbstractJarvisTest;
import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.stubs.StubJarvisCore;
import edu.uoc.som.jarvis.stubs.StubRuntimePlatform;
import org.apache.commons.configuration2.BaseConfiguration;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/*
 * This class is abstract: RestActions are only tested through their concrete subclasses.
 */
public abstract class RestActionTest extends AbstractJarvisTest {

    protected static JarvisCore JARVIS_CORE;

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

    protected JarvisSession session;

    @BeforeClass
    public static void setUpBeforeClass() {
        JARVIS_CORE = new StubJarvisCore();
        RUNTIME_PLATFORM = new StubRuntimePlatform(JARVIS_CORE, new BaseConfiguration());
    }

    @AfterClass
    public static void tearDownAfterClass() {
        RUNTIME_PLATFORM.shutdown();
        JARVIS_CORE.shutdown();
    }

    @Before
    public void setUp() {
        this.session = new JarvisSession("sessionID");
    }
}

package com.xatkit.core.recognition;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.server.XatkitServer;
import com.xatkit.core.server.XatkitServerUtils;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.library.core.CoreLibrary;
import lombok.SneakyThrows;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHttpRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.assertEquals;


public class RecognitionMonitorPostgreSQLTest extends AbstractXatkitTest {

    private int botId = 1;

    private String url = "jdbc:postgresql://localhost:5432/test_monitoring";

    private String user = "monitoring";

    private String password = "testmonitoring";

    private Configuration baseConfiguration;

    private StateContext context;

    private RecognitionMonitorPostgreSQL monitor;

    private XatkitServer server;

    private RecognizedIntent greetings;

    private RecognizedIntent greetings2;

    private Connection conn;

    @Before
    public void setUp() {

        this.context = ExecutionFactory.eINSTANCE.createStateContext();
        this.context.setContextId("43701e87-7e35-44c0-9121-12a424732f91");

        /*
         * Create a minimal configuration
         */
        botId=1;
        this.baseConfiguration = new BaseConfiguration();
        this.baseConfiguration.addProperty(RecognitionMonitorPostgreSQL.BOT_ID_KEY, botId );
        this.baseConfiguration.addProperty(RecognitionMonitorPostgreSQL.URL, url );
        this.baseConfiguration.addProperty(RecognitionMonitorPostgreSQL.USER, user);
        this.baseConfiguration.addProperty(RecognitionMonitorPostgreSQL.PASSWORD, password);
        this.baseConfiguration.setProperty(XatkitServerUtils.SERVER_PORT_KEY, 1234);

        this.server = new XatkitServer(new BaseConfiguration());

        try {
            conn = DriverManager.getConnection(url, user, password);
            conn.setAutoCommit(false);
            //Cleaning the tables for the test
            PreparedStatement st = conn.prepareStatement("DELETE FROM monitoring_entry");
            st.executeUpdate();
            PreparedStatement st2 = conn.prepareStatement("DELETE FROM monitoring_session");
            st.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error when connecting to the PostgreSQL monitoring, see the attached "
                    + "exception", e);
        }

    }

    @Test
    public void successfulConnection() {
        try {
            monitor = new RecognitionMonitorPostgreSQL(server,baseConfiguration);
        } catch (Exception e) {
            throw new RuntimeException("Error when testing the connection to PostgreSQL monitoring, "
                    + "see the attached "
                    + "exception", e);
        }

    }

    @Test(expected = RuntimeException.class)
    public void constructEmptyConfiguration() {
        Configuration emptyConfiguration = new BaseConfiguration();
        try {
            monitor = new RecognitionMonitorPostgreSQL(server, emptyConfiguration);
        } catch (Exception e) {
            throw new RuntimeException("Error when testing the connection to PostgreSQL monitoring, "
                    + "see the attached "
                    + "exception", e);
        }
    }

    @Test
    public void checkSingleIntentLog() {
        greetings = IntentFactory.eINSTANCE.createRecognizedIntent();
        greetings.setDefinition(CoreLibrary.Greetings);
        greetings.setRecognitionConfidence(.5f);
        greetings.setMatchedInput("Hello");

        try {
            monitor = new RecognitionMonitorPostgreSQL(server,baseConfiguration);
            monitor.logRecognizedIntent(context, greetings);
            PreparedStatement st = conn.prepareStatement("SELECT * FROM monitoring_session WHERE session_uuid"
                    + " = ?");
            st.setString(1, context.getContextId());
            ResultSet rs = st.executeQuery();
            rs.next();
            int sessionId = rs.getInt("id");
            String UUID = rs.getString("session_uuid");
            int sessionBotId = rs.getInt("bot_id");
            assertEquals(context.getContextId(),UUID);
            assertEquals(this.botId,sessionBotId);
            st.close();

            PreparedStatement st2 = conn.prepareStatement("SELECT intent, utterance, confidence FROM monitoring_entry "
                    + "WHERE session_id = ?");
            st2.setInt(1, sessionId);
            ResultSet rs2 = st2.executeQuery();
            rs2.next();
            assertEquals(greetings.getMatchedInput(), rs2.getString("utterance"));
            assertEquals(greetings.getRecognitionConfidence(), rs2.getFloat("confidence"),0.2);
            assertEquals(greetings.getDefinition().getName(), rs2.getString("intent"));
            st2.close();

        } catch (Exception e)
        {
            throw new RuntimeException("Error when testing the log of a new intent with PostgreSQL monitoring, "
                    + "see the attached exception", e);
        }

    }

    @Test
    public void checkMultipleIntentLog() {
        greetings = IntentFactory.eINSTANCE.createRecognizedIntent();
        greetings.setDefinition(CoreLibrary.Greetings);
        greetings.setRecognitionConfidence(.5f);
        greetings.setMatchedInput("Hello");

        greetings2 = IntentFactory.eINSTANCE.createRecognizedIntent();
        greetings2.setDefinition(CoreLibrary.Greetings);
        greetings2.setRecognitionConfidence(.7f);
        greetings2.setMatchedInput("Hi");


        try {
            monitor = new RecognitionMonitorPostgreSQL(server,baseConfiguration);
            monitor.logRecognizedIntent(context, greetings);
            monitor.logRecognizedIntent(context, greetings2);
            PreparedStatement st = conn.prepareStatement("SELECT id FROM monitoring_session WHERE session_uuid"
                    + " = ?");
            st.setString(1, context.getContextId());
            ResultSet rs = st.executeQuery();
            rs.next();
            int sessionId = rs.getInt("id");

            PreparedStatement st2 = conn.prepareStatement("SELECT count(*) FROM monitoring_entry WHERE "
                    + "session_id"
                    + " = ?");
            st2.setInt(1, sessionId);
            ResultSet rs2 = st2.executeQuery();
            rs2.next();
            int count = rs2.getInt(1);
            assertEquals(2,count);

        } catch (Exception e)
        {
            throw new RuntimeException("Error when testing the log of a two intents with PostgreSQL monitoring, "
                    + "see the attached exception", e);
        }

    }

    @Test
    public void checkRegisteredEndPoints()
    {
        try {
            monitor = new RecognitionMonitorPostgreSQL(server,baseConfiguration);
            HttpRequest VALID_HTTP_REQUEST;
            VALID_HTTP_REQUEST = new BasicHttpRequest("GET", "/analytics/monitoring/matched");
            VALID_HTTP_REQUEST = new BasicHttpRequest("GET", "/analytics/monitoring/unmatched");
            VALID_HTTP_REQUEST = new BasicHttpRequest("GET", "/analytics/monitoring/");
            VALID_HTTP_REQUEST = new BasicHttpRequest("GET", "/analytics/monitoring/sessions/stats");
            VALID_HTTP_REQUEST = new BasicHttpRequest("GET", "/analytics/monitoring/session");

        } catch (Exception e) {
            throw new RuntimeException("Error when testing the registered endpoints for PostgreSQL monitoring, "
                    + "see the attached "
                    + "exception", e);
        }
    }




    @SneakyThrows
    @After
    public void tearDown() {

        if (monitor != null) {
            if (monitor.getConn() != null && !monitor.getConn().isClosed()) {
               monitor.getConn().close();
                //Cleaning the tables after the test
            }
        }
        PreparedStatement st = conn.prepareStatement("DELETE FROM monitoring_entry");
        st.executeUpdate();
        PreparedStatement st2 = conn.prepareStatement("DELETE FROM monitoring_session");
        st.executeUpdate();
        conn.commit();
        conn.close();

    }



}
package com.xatkit.core.recognition;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.server.XatkitServer;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.library.core.CoreLibrary;
import lombok.SneakyThrows;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.assertEquals;


public class RecognitionMonitorPostgresTest extends AbstractXatkitTest {

    private int botId = 1;

    private String url = "jdbc:postgresql://localhost:5432/test-monitoring";

    private String user = "monitoring";

    private String password = "testmonitoring";

    private Configuration baseConfiguration;

    private StateContext context;

    private RecognitionMonitorPostgreSQL monitor;

    private XatkitServer server;

    private RecognizedIntent greetings;

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

        this.server = new XatkitServer(new BaseConfiguration());

        try {
            conn = DriverManager.getConnection(url, user, password);
            //Cleaning the tables for the test
            PreparedStatement st = conn.prepareStatement("DELETE FROM monitoring_entry");
            st.executeUpdate();
            PreparedStatement st2 = conn.prepareStatement("DELETE FROM monitoring_session");
            st.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error when shutting down the PostgreSQL monitoring, see the attached "
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

    @Test(expected = IllegalArgumentException.class)
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

        try {
            monitor.logRecognizedIntent(context, greetings);
            PreparedStatement st = conn.prepareStatement("SELECT session_id FROM monitoring_session WHERE session_uuid"
                    + " = ?");
            st.setString(1, context.getContextId());
            ResultSet rs = st.executeQuery();
            rs.next();
            int sessionId = rs.getInt("session_id");
            String UUID = rs.getString("session_uuid");
            int sessionBotId = rs.getInt("bot_id");
            assertEquals(context.getContextId(),UUID);
            assertEquals(this.botId,sessionBotId);

            PreparedStatement st2 = conn.prepareStatement("SELECT intent, utterance, confidence FROM monitoring_entry "
                    + "WHERE session_id = ?");
            st2.setInt(1, sessionId);
            ResultSet rs2 = st.executeQuery();
            rs2.next();
            assertEquals(greetings.getMatchedInput(), rs.getString("utterance"));
            assertEquals(greetings.getRecognitionConfidence(), rs.getFloat("confidence"));
            assertEquals(greetings.getDefinition().getName(), rs.getString("intent"));

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

        try {
            monitor.logRecognizedIntent(context, greetings);
            monitor.logRecognizedIntent(context, greetings);
            PreparedStatement st = conn.prepareStatement("SELECT session_id FROM monitoring_session WHERE session_uuid"
                    + " = ?");
            st.setString(1, context.getContextId());
            ResultSet rs = st.executeQuery();
            rs.next();
            int sessionId = rs.getInt("session_id");


            PreparedStatement st2 = conn.prepareStatement("SELECT count(*) as entries FROM monitoring_entry WHERE "
                    + "session_id"
                    + " = ?");
            st2.setInt(1, sessionId);
            ResultSet rs2 = st.executeQuery();
            rs2.next();
            int count = rs2.getInt("entries");
            assertEquals(count,2);

        } catch (Exception e)
        {
            throw new RuntimeException("Error when testing the log of a two intents with PostgreSQL monitoring, "
                    + "see the attached exception", e);
        }

    }


    @SneakyThrows
    @After
    public void tearDown() {

        if (monitor != null) {
            if (monitor.getConn() != null && !monitor.getConn().isClosed()) {
                monitor.getConn().rollback();
                monitor.getConn().close();
            }
        }

    }



}

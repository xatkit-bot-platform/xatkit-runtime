package com.xatkit.core.recognition;

import com.mashape.unirest.http.Unirest;
import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.server.HttpMethod;
import com.xatkit.core.server.RestHandler;
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
import org.assertj.core.data.Offset;
import org.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests are ignored because we don't have a PostgreSQL database available for our CI/CD pipelines.
 */
@Ignore
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
        botId = 1;
        this.baseConfiguration = new BaseConfiguration();
        this.baseConfiguration.addProperty(RecognitionMonitorPostgreSQL.BOT_ID_KEY, botId);
        this.baseConfiguration.addProperty(RecognitionMonitorPostgreSQL.URL, url);
        this.baseConfiguration.addProperty(RecognitionMonitorPostgreSQL.USER, user);
        this.baseConfiguration.addProperty(RecognitionMonitorPostgreSQL.PASSWORD, password);
        this.baseConfiguration.setProperty(XatkitServerUtils.SERVER_PORT_KEY, 1234);

        this.server = new XatkitServer(new BaseConfiguration());

        try {
            conn = DriverManager.getConnection(url, user, password);
            conn.setAutoCommit(false);
            deleteTestData();
        } catch (Exception e) {
            throw new RuntimeException("Error when connecting to the PostgreSQL monitoring, see the attached "
                    + "exception", e);
        }

    }

    @Test
    public void successfulConnection() {
        try {
            monitor = new RecognitionMonitorPostgreSQL(server, baseConfiguration);
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
            deleteTestData();
            monitor = new RecognitionMonitorPostgreSQL(server, baseConfiguration);
            monitor.logRecognizedIntent(context, greetings);
            PreparedStatement st = conn.prepareStatement("SELECT * FROM monitoring_session WHERE session_uuid"
                    + " = ?");
            st.setString(1, context.getContextId());
            ResultSet rs = st.executeQuery();
            rs.next();
            int sessionId = rs.getInt("id");
            String UUID = rs.getString("session_uuid");
            int sessionBotId = rs.getInt("bot_id");
            assertThat(context.getContextId()).isEqualTo(UUID);
            assertThat(this.botId).isEqualTo(sessionBotId);
            st.close();

            PreparedStatement st2 = conn.prepareStatement("SELECT intent, utterance, confidence FROM monitoring_entry "
                    + "WHERE session_id = ?");
            st2.setInt(1, sessionId);
            ResultSet rs2 = st2.executeQuery();
            rs2.next();
            assertThat(greetings.getMatchedInput()).isEqualTo(rs2.getString("utterance"));
            assertThat(greetings.getRecognitionConfidence()).isEqualTo(rs2.getFloat("confidence"), Offset.offset(0.2f));
            assertThat(greetings.getDefinition().getName()).isEqualTo(rs2.getString("intent"));
            st2.close();

        } catch (Exception e) {
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
            deleteTestData();
            monitor = new RecognitionMonitorPostgreSQL(server, baseConfiguration);
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
            assertThat(count).isEqualTo(2);

        } catch (Exception e) {
            throw new RuntimeException("Error when testing the log of a two intents with PostgreSQL monitoring, "
                    + "see the attached exception", e);
        }

    }

    @Test
    public void checkRegistrationEndPoints() {
        try {
            monitor = new RecognitionMonitorPostgreSQL(server, baseConfiguration);
        } catch (Exception e) {
            throw new RuntimeException("Error when testing the registered endpoints for PostgreSQL monitoring, "
                    + "see the attached "
                    + "exception", e);
        }
        RestHandler VALID_HTTP_REQUEST;
        VALID_HTTP_REQUEST = server.getRegisteredRestHandler(HttpMethod.GET,
                "/analytics/monitoring/matched");
        VALID_HTTP_REQUEST = server.getRegisteredRestHandler(HttpMethod.GET, "/analytics/monitoring/unmatched");
        VALID_HTTP_REQUEST = server.getRegisteredRestHandler(HttpMethod.GET, "/analytics/monitoring/");
        VALID_HTTP_REQUEST = server.getRegisteredRestHandler(HttpMethod.GET, "/analytics/monitoring/sessions/stats");
        VALID_HTTP_REQUEST = server.getRegisteredRestHandler(HttpMethod.GET, "/analytics/monitoring/session");

    }

    @Test
    public void checkMatched() {
        deleteTestData();
        JSONArray result;

        try {
            greetings = IntentFactory.eINSTANCE.createRecognizedIntent();
            greetings.setDefinition(CoreLibrary.Greetings);
            greetings.setRecognitionConfidence(.5f);
            greetings.setMatchedInput("Hello");
            monitor = new RecognitionMonitorPostgreSQL(server, baseConfiguration);
            monitor.logRecognizedIntent(context, greetings);

            /*
             * We need to start the server in order to test the REST API.
             */
            server.start();
            result = Unirest.get("http://localhost:5000/analytics/monitoring/matched")
                    .asJson()
                    .getBody()
                    .getArray();

            server.stop();
        } catch (Exception e) {
            throw new RuntimeException("Error when testing the matched endpoint for PostgreSQL monitoring, "
                    + "see the attached "
                    + "exception", e);
        }
        assertThat(result).hasSize(1);
        assertThat(result.getJSONObject(0).getString("utterance")).isEqualTo("Hello");
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
        deleteTestData();
        conn.close();

    }

    private void deleteTestData() {
        try {
            PreparedStatement st = conn.prepareStatement("DELETE FROM monitoring_entry");
            st.executeUpdate();
            PreparedStatement st2 = conn.prepareStatement("DELETE FROM monitoring_session");
            st2.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Error when deleting the test data for PostgreSQL monitoring, "
                    + "see the attached "
                    + "exception", e);
        }

    }

}

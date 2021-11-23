package com.xatkit.core.recognition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.xatkit.core.server.HttpMethod;
import com.xatkit.core.server.HttpUtils;
import com.xatkit.core.server.RestHandlerException;
import com.xatkit.core.server.RestHandlerFactory;
import com.xatkit.core.server.XatkitServer;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.isNull;

/**
 * Provides monitoring capabilities for {@link IntentRecognitionProvider}s.
 * <p>
 * This class stores analytics information related to intent recognition, and registers a set of REST endpoints
 * allowing to query them from external applications.
 * Also, this class manages the connection to an external database (PostgresQL) and stores the information.
 * Each stored point in the database includes the following information:
 * <p>
 * <li><b>bot_id</b>: The ID of the bot that took care of answering the utterance. </li>
 * <li><b>is_Matched</b>: If said utterance was matched or not. </li>
 * <li><b>session_id</b>: The ID of the session. </li>
 * <li><b>origin</b>: The origin of the utterance, assuming it's defined (and the bot runs in different sites). </li>
 * <li><b>platform</b>: Platform in which the session did ocurr. </li>
 * <li><b>username</b>: self-explanatory. </li>
 * <li><b>confidence</b>: How confident is the bot about the answer given to the user. </li>
 * <li><b>utterance</b>: The received message by the input, which is being interpreted by the bot. </li>
 * <li><b>intent</b>: Recognized intent by the RecognitionProvider. </li>
 * <li><b>parameters</b>: Matched parameters </li>
 * <li><b>timestamp</b>: Time entry when the utterance occurred. </li>
 * </p>
 * <p>
 * The following endpoints can be used to access the stored information:
 * <ul>
 * <li><b>/analytics/monitoring</b>: returns a JSON array containing all the persisted monitoring information (note
 * that this method doesn't support pagination yet, so the returned JSON may be big for long-running applications).</li>
 * <li><b>/analytics/monitoring/session?sessionId=id</b>: returns a JSON object containing the monitoring information
 * for the provided {@code sessionId}</li>
 * <li><b>/analytics/monitoring/unmatched</b>: returns a JSON array containing all the monitoring entries
 * corresponding to unmatched inputs (i.e. inputs that haven't been successfully translated into intents)</li>
 * <li><b>/analytics/monitoring/matched</b>: returns a JSON array containing all the monitoring entries
 * corresponding to matched inputs (i.e. inputs that have been successfully translated into intents)</li>
 * <li><b>/analytics/monitoring/sessions/stats</b>: returns a JSON object containing computed statistics over
 * stored sessions (e.g. average time/session, average number of matched inputs/sessions, etc)</li>
 * <li><b>/analytics/monitoring/origin</b>: returns a JSON object with a resume of the different origins where the data
 * is received from.</li>
 * </ul>
 */
public class RecognitionMonitorPostgreSQL implements RecognitionMonitor {

    /**
     * The {@link Configuration} key to specify the connection URL for the database
     *
     * This property is mandatory.
     */
    public static final String URL = "xatkit.postgresql.url";


    /**
     * The {@link Configuration} key to specify the user to connect to the database
     *
     * This property is mandatory.
     */
    public static final String USER = "xatkit.postgresql.user";

    /**
     * The {@link Configuration} key to specify the user to connect to the database
     *
     * This property is mandatory.
     */
    public static final String PASSWORD = "xatkit.postgresql.password";

    /**
     * The bot ID key.
     * <p>
     * This property is optional and defaults to {@code "xatkitBot} if not specified.
     */
    public static final String BOT_ID_KEY = "xatkit.postgresql.bot_id";

    /**
     * The actual ID of the bot.
     */
    private final Integer botId;

    /**
     * The database connection object to make the petitions.
     */
    private final Connection conn;

    /**
     * Initializes the PostgreSQL Recognition Monitor.
     *
     * As a precondition, it checks the existence of all mandatory parameters
     * and tests a connection can be established
     *
     * @param xatkitServer  the {@link XatkitServer} instance used to register the REST endpoints
     * @param configuration the Xatkit {@link Configuration}
     */
    public RecognitionMonitorPostgreSQL(XatkitServer xatkitServer, Configuration configuration) throws SQLException {
        Log.info("Starting new intent recognition monitoring with PostgresqL");

        checkArgument(configuration.containsKey(URL), "You need to provide the URL of the PostgreSQL database, please "
                + "provide a valid token in the configuration (key: %s)", URL);

        checkArgument(configuration.containsKey(USER), "User details for the PostgreSQL connection are missing, "
                + "please provide a valid token in the configuration (key: %s)", USER);

        checkArgument(configuration.containsKey(PASSWORD), "Password details for the PostgreSQL conection are "
                + "missing, please provide a valid token in the configuration (key: %s)", PASSWORD);

        botId = new Integer(configuration.getString(BOT_ID_KEY));
        String url = configuration.getString(URL);
        String user = configuration.getString(USER);
        String password = configuration.getString(PASSWORD);

        Log.info("DB URL: {0}", url);

        conn = DriverManager.getConnection(url, user, password);
        conn.setAutoCommit(true);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (!this.conn.isClosed()) {
                    conn.commit();
                    conn.close();
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error when shutting down the PostgreSQL monitoring, see the attached "
                        + "exception", e);
            }
        }));

        registerServerEndpoints(xatkitServer);
    }

    /**
     * Registers the REST endpoints used to retrieve monitoring information.
     *
     * @param xatkitServer the {@link XatkitServer} instance used to register the REST endpoints
     */
    private void registerServerEndpoints(XatkitServer xatkitServer) {
        this.registerGetMonitoringData(xatkitServer);
        this.registerGetMonitoringDataForSession(xatkitServer);
        this.registerGetUnmatchedUtterances(xatkitServer);
        this.registerGetMatchedUtterances(xatkitServer);
        this.registerGetSessionsStats(xatkitServer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logRecognizedIntent(StateContext context, RecognizedIntent intent) {
        try {
            int sessionId = registerSession(context.getContextId(), botId);
            //Timestamp value for the entry is automatically assigned by postgres when inserting
            PreparedStatement st = conn.prepareStatement("INSERT INTO monitoring_entry (session_id, utterance,"
                    + "intent, origin, confidence) VALUES (?,?,?,?,?)");
            st.setInt(1, sessionId);
            st.setString(2, intent.getMatchedInput());
            st.setString(3, intent.getDefinition().getName());
            st.setString(4, context.getOrigin());
            st.setFloat(5, intent.getRecognitionConfidence());
            st.executeUpdate();
            st.close();
        } catch (Exception e) {
            throw new RuntimeException("Error when registering PostgreSQL monitoring data, see the "
                    + "attached exception", e);
        }
    }


    /**
     * Inserts a new session row if the session is not already registered in the database.
     *
     * @param UUID is used as unique identifier for the sessions
     */
    private int registerSession(String UUID, int botId) throws SQLException {
        int sessionId = getSessionFromUUID(UUID);
        if (sessionId <= 0) {
            insertNewSession(UUID, botId);
            sessionId = getSessionFromUUID(UUID);
        }
        return sessionId;
    }

    /**
     * Returns the internal database session identifier.
     *
     * @param UUID is used as unique identifier for the sessions
     */
    private int getSessionFromUUID(String UUID) throws SQLException {
        int sessionId = -1;
        PreparedStatement st = conn.prepareStatement("SELECT id FROM monitoring_session WHERE session_uuid = "
                + "?");
        st.setString(1, UUID);
        ResultSet rs = st.executeQuery();
        if (rs.next()) {
            sessionId = rs.getInt("id");
        }
        st.close();
        return sessionId;
    }

    /**
     * Inserts a new monitoring session
     *
     */
    private void insertNewSession(String UUID, int botId) throws SQLException {
        PreparedStatement st = conn.prepareStatement("INSERT INTO monitoring_session (session_uuid, bot_id) VALUES (?,?"
                + ")");
        st.setString(1, UUID);
        st.setInt(2, botId);
        st.executeUpdate();
        st.close();
    }


    /**
     * Registers the {@code GET: /analytics/monitoring/matched} endpoint.
     * <p>
     * This endpoint returns a JSON array containing all  the matched intents (i.e. inputs that have been
     * successfully translated into intents) for the bot
     * <p>
     * The listing below shows an example of the returned JSON payload:
     * <pre>
     * {@code
     * [
     *     {
     *         "sessionId": "72f8fa90-8d3e-4804-b00d-5612a95fb644",
     *         "timestamp": 1573750605388,
     *         "utterance": "How are you?",
     *         "intent": "HowAreYou",
     *         "confidence": 1.0
     *     }
     * ]
     * }
     * </pre>
     *
     * @param xatkitServer the {@link XatkitServer} instance used to register the REST endpoint
     */
    private void registerGetMatchedUtterances(XatkitServer xatkitServer) {
        xatkitServer.registerRestEndpoint(HttpMethod.GET, "/analytics/monitoring/matched",
                RestHandlerFactory.createJsonRestHandler((headers, params, content) -> {
                    try {
                        JsonArray res = getUtterances(true);
                        return res;
                    } catch (Exception e) {
                        throw new RuntimeException("Error when retrieving utterances with the PostgreSQL "
                                + "monitoring, see the attached "
                                + "exception", e);
                    }

                })
        );
    }

    /**
     * Registers the {@code GET: /analytics/monitoring/unmatched} endpoint.
     * <p>
     * This endpoint returns a JSON array containing all the unmatched intents (i.e. inputs that have been
     * unsuccessfully translated into intents).
     * <p>
     *
     * @param xatkitServer the {@link XatkitServer} instance used to register the REST endpoint
     */
    private void registerGetUnmatchedUtterances(XatkitServer xatkitServer) {
        xatkitServer.registerRestEndpoint(HttpMethod.GET, "/analytics/monitoring/unmatched",
                RestHandlerFactory.createJsonRestHandler((headers, params, content) -> {
                    try {
                        JsonArray res = getUtterances(false);
                        return res;
                    }  catch (Exception e) {
                        throw new RuntimeException("Error when retrieving utterances with the PostgreSQL monitoring, "
                                + "see the attached exception", e);
                    }

                })
        );
    }


    private JsonArray getUtterances(boolean matched) throws SQLException {
        JsonArray result = new JsonArray();
        String sql = "SELECT s.UUID, m.utterance, m.intent, m.instant, m.confidence from monitoring_session s, monitoring_entry "
                + "m where s.id=m.session_id and s.bot_id = ? ";
        if (matched) {
            sql = sql + " AND intent <> 'Default_Fallback_Intent'";
        } else {
            sql = sql + " AND intent = 'Default_Fallback_Intent'";
        }

        PreparedStatement st = conn.prepareStatement(sql);
        st.setInt(1, botId);
        ResultSet rs = st.executeQuery();
        while (rs.next()) {
            JsonObject matchedUtteranceObject = new JsonObject();
            matchedUtteranceObject.addProperty("sessionId", rs.getString("UUID"));
            matchedUtteranceObject.addProperty("timestamp", rs.getObject("instant", LocalDateTime.class).toInstant(OffsetDateTime.now().getOffset()).toEpochMilli());
            matchedUtteranceObject.addProperty("utterance", rs.getString("utterance"));
            matchedUtteranceObject.addProperty("intent", rs.getString("intent"));
            matchedUtteranceObject.addProperty("confidence", rs.getString("confidence"));
            result.add(matchedUtteranceObject);
        }
        st.close();
        return result;
    }

    /**
     * Registers the {@code GET: /analytics/monitoring} endpoint.
     * <p>
     * This endpoint returns a JSON array containing all the persisted monitoring information for the bot (note that
     * this method doesn't support pagination yet, so the returned JSON may be big for long-running applications).
     * <p>
     * The listing below shows an example of the returned JSON payload:
     * <pre>
     * {@code
     * [
     *  [{
     *      "sessionId": "7df67aeb-4e20-4ee4-86dd-8f0df52e3720",
     *      "entries": [{
     *          "timestamp": 1582543925719,
     *          "utterance": "whats up",
     *          "intent": "Default_Fallback_Intent",
     *          "confidence": 1.0
     *      }, {
     *          "timestamp": 1582543930723,
     *          "utterance": "help me pls",
     *          "intent": "Default_Fallback_Intent",
     *          "confidence": 1.0
     *      }],
     *      "matchedUtteranceCount": 0,
     *      "unmatchedUtteranceCount": 2
     *  }, {
     *      "sessionId": "22d48fa1-bb93-42fc-bf7e-7a61903fb0e4",
     *      "entries": [{
     *          "timestamp": 1582543939678,
     *          "utterance": "hi",
     *          "intent": "Welcome",
     *          "confidence": 1.0
     *      }, {
     *          "timestamp": 1582543942658,
     *          "utterance": "how are you?",
     *          "intent": "HowAreYou",
     *          "confidence": 1.0
     *      }, {
     *          "timestamp": 1582543948698,
     *          "utterance": "me too! thanks!",
     *          "intent": "Default_Fallback_Intent",
     *          "confidence": 1.0
     *      }],
     *      "matchedUtteranceCount": 2,
     *      "unmatchedUtteranceCount": 1,
     *      "avgSessionConfidence": 1.0
     *  }], {
     *      "nSessions": 2,
     *      "avgRecognitionConfidence": 1.0,
     *      "totalUnmatchedUtterances": 3,
     *      "totalMatchedUtterances": 2
     * }]
     * </pre>
     *
     * @param xatkitServer the {@link XatkitServer} instance used to register the REST endpoint
     */
    private void registerGetMonitoringData(XatkitServer xatkitServer) {
        xatkitServer.registerRestEndpoint(HttpMethod.GET, "/analytics/monitoring",
                RestHandlerFactory.createJsonRestHandler((headers, param, content) -> {
                    JsonArray sessionsArray = new JsonArray();
                    double accRecognitionConfidence = 0.0;
                    int matchedCount = 0;
                    int unmatchedCount = 0;
                    int nSessions = 0;
                    try
                    {
                        PreparedStatement st = conn.prepareStatement("SELECT * FROM monitoring_session WHERE bot_id = ?");
                        st.setInt(1, botId);
                        ResultSet rs = st.executeQuery();

                        while (rs.next()) {
                            JsonObject sessionObject = buildSessionObject(rs.getInt("id"));
                            int sessionMatchedCount = sessionObject.get("matchedUtteranceCount").getAsInt();
                            matchedCount += sessionMatchedCount;
                            unmatchedCount += sessionObject.get("unmatchedUtteranceCount").getAsInt();
                            if (sessionObject.has("avgSessionConfidence")) {
                                double avgSessionConfidence = sessionObject.get("avgSessionConfidence").getAsDouble();
                                accRecognitionConfidence += avgSessionConfidence * (double) sessionMatchedCount;
                            }
                            nSessions++;
                            sessionsArray.add(sessionObject);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Error when retrieving utterances with the PostgreSQL monitoring, "
                                + "see the attached exception", e);
                    }

                    JsonObject globalInfo = new JsonObject();
                    globalInfo.addProperty("nSessions", nSessions);
                    double aux = accRecognitionConfidence / (double) matchedCount;
                    if (!Double.isNaN(aux) && Double.isFinite(aux)) {
                        globalInfo.addProperty("avgRecognitionConfidence", aux);
                    }
                    globalInfo.addProperty("totalUnmatchedUtterances", unmatchedCount);
                    globalInfo.addProperty("totalMatchedUtterances", matchedCount);
                    JsonArray resultArray = new JsonArray();
                    resultArray.add(sessionsArray);
                    resultArray.add(globalInfo);
                    return resultArray;
                }));
    }


    /**
     * Registers the {@code GET: /analytics/monitoring/sessions/stats} endpoint.
     * <p>
     * This endpoint returns a JSON object containing computed statistics over stored sessions (e.g. average
     * time/session, average number of matched inputs/sessions, etc) for the bot;
     *
     * <p>
     * The listing below shows an example of the returned JSON payload:
     * <pre>
     * {@code
     * {
     *     "averageMatchedUtteranceCount": 1.0,
     *     "averageUnmatchedUtteranceCount": 2.0,
     *     "averageSessionTime": 43.246
     * }
     * }
     * </pre>
     *
     * @param xatkitServer
     */
    private void registerGetSessionsStats(XatkitServer xatkitServer) {
        xatkitServer.registerRestEndpoint(HttpMethod.GET, "/analytics/monitoring/sessions/stats",
                RestHandlerFactory.createJsonRestHandler(((headers, params, content) -> {
                    JsonObject result = new JsonObject();
                    int sessionCount = 0;
                    int totalMatched = 0;
                    int totalUnmatched = 0;
                    long sessionTime = 0;

                    try {

                        PreparedStatement st = conn.prepareStatement("SELECT COUNT(*) FROM monitoring_session where "
                            + "bot_id = ? ");
                        st.setInt(1, botId);
                        ResultSet rsSessionCount = st.executeQuery();
                        rsSessionCount.next();
                        sessionCount = rsSessionCount.getInt(1);
                        st.close();

                        PreparedStatement st2 = conn.prepareStatement("SELECT id FROM monitoring_session WHERE bot_id = ? ");
                        st2.setInt(1, botId);
                        ResultSet rs = st2.executeQuery();

                        int count;
                        int sessionId;
                        int matched;
                        int unmatched;
                        long minTime;
                        long maxTime;

                        //Given the information the endpoints returns right now some of these queries could have been
                        // done at the global level but doing them at the session level will facilitate further extensions
                        while (rs.next()) {
                            sessionId = rs.getInt("id");

                            PreparedStatement stCount = conn.prepareStatement("SELECT COUNT(*) FROM monitoring_entry  "
                                    + "WHERE session_id = ? ");
                            stCount.setInt(1, sessionId);
                            ResultSet rsCount = stCount.executeQuery();
                            count = rsCount.getInt(1);
                            stCount.close();

                            PreparedStatement stCountMatched = conn.prepareStatement("SELECT COUNT(*) FROM "
                                    + "monitoring_entry WHERE session_id = ?  AND intent <> 'Default_Fallback_Intent' ");
                            stCountMatched.setInt(1, sessionId);
                            ResultSet rsCountMatched = stCountMatched.executeQuery();
                            matched = rsCountMatched.getInt(1);
                            stCountMatched.close();

                            PreparedStatement stCountUnMatched = conn.prepareStatement("SELECT COUNT(*) FROM "
                                    + "monitoring_entry WHERE session_id = ?  AND intent = 'Default_Fallback_Intent' ");
                            stCountUnMatched.setInt(1, sessionId);
                            ResultSet rsCountUnMatched = stCountUnMatched.executeQuery();
                            unmatched = rsCountUnMatched.getInt(1);
                            stCountUnMatched.close();

                            PreparedStatement stTime = conn.prepareStatement("SELECT MIN(instant) minTime, MAX(instant) "
                                    + "maxTime  FROM monitoring_entry WHERE session_id = ? ");
                            stTime.setInt(1, sessionId);
                            ResultSet rsTime = stTime.executeQuery();
                            minTime = rsTime.getObject("mintime", LocalDateTime.class).toEpochSecond(OffsetDateTime.now().getOffset());
                            maxTime = rsTime.getObject("mintime", LocalDateTime.class).toEpochSecond(OffsetDateTime.now().getOffset());
                            stTime.close();

                            totalMatched = totalMatched + matched;
                            totalUnmatched = totalUnmatched + unmatched;
                            sessionTime = sessionTime + (maxTime - minTime);
                        }

                        st2.close();
                    } catch (Exception e) {
                        throw new RuntimeException("Error when retrieving session stats with the PostgreSQL "
                                + "monitoring, "
                                + "see the attached exception", e);
                    }

                    double avgMatchedUtterance = totalMatched / (double) sessionCount;
                    double avgUnmatchedUtterance = totalUnmatched / (double) sessionCount;
                    double avgSessionTime = sessionTime / (double) sessionCount;
                    result.addProperty("averageMatchedUtteranceCount", avgMatchedUtterance);
                    result.addProperty("averageUnmatchedUtteranceCount", avgUnmatchedUtterance);
                    result.addProperty("averageSessionTime", avgSessionTime);
                    return result;
                })));
    }


    /**
     * Registers the {@code GET: /analytics/monitoring/session} endpoint.
     * <p>
     * This endpoint expects a {@code sessionId} parameter corresponding to an existing {@code session}. If the
     * parameter is not provided or if the provided {@code sessionId} does not correspond to an existing {@code
     * session} the returned JSON payload will contain a single {@code error} field with an error message.
     * <p>
     * The listing below shows an example of the returned JSON payload:
     * <pre>
     * {@code
     * {
     *     "sessionId": "72f8fa90-8d3e-4804-b00d-5612a95fb644",
     *     "entries": [
     *         {
     *             "timestamp": 1573750605388,
     *             "utterance": "How are you?",
     *             "intent": "HowAreYou",
     *             "confidence": 1.0
     *         },
     *         {
     *             "timestamp": 1573750623741,
     *             "utterance": "Here is something you won't understand!",
     *             "intent": "Default_Fallback_Intent",
     *             "confidence": 1.0
     *         },
     *         {
     *             "timestamp": 1573750630281,
     *             "utterance": "I knew it",
     *             "intent": "Default_Fallback_Intent",
     *             "confidence": 1.0
     *         }
     *     ],
     *     "matchedUtteranceCount": 1,
     *     "unmatchedUtteranceCount": 2
     * }
     * }
     * </pre>
     * <p>
     * <b>Note</b>: this endpoint returns a {@code 404} status and the following content if the provided session does
     * not exist:
     * <pre>
     * {@code
     *  {
     *      "error" : "Session <sessionId> not found"
     *  }
     * }
     * </pre>
     *
     * @param xatkitServer the {@link XatkitServer} instance used to register the REST endpoint
     */
    private void registerGetMonitoringDataForSession(XatkitServer xatkitServer) {
        xatkitServer.registerRestEndpoint(HttpMethod.GET, "/analytics/monitoring/session",
                RestHandlerFactory.createJsonRestHandler(((headers, params, content) -> {
                    String sessionId = HttpUtils.getParameterValue("sessionId", params);
                    if (isNull(sessionId)) {
                        throw new RestHandlerException(404, "Missing parameter sessionId");
                    }
                    try {
                        int id = getSessionFromUUID(sessionId);
                        if (id <= 0) {
                            throw new RestHandlerException(404, "Session " + sessionId + " not found");
                        } else {
                            //we use the internal session id to facilitate the retrieval of the data
                            return buildSessionObject(id);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Error when getting monitoring data from PostgreSLQ, see the "
                                + "attached exception ", e);
                    }

                })));
    }


    /**
     * Creates a {@link JsonObject} representing the provided session record.
     *
     * @param sessionId   the identifier of the session to translate to a {@link JsonObject}
     * @return the created {@link JsonObject}
     */
    private JsonObject buildSessionObject(int sessionId) throws SQLException {
        JsonObject sessionObject = new JsonObject();
        sessionObject.addProperty("sessionId", sessionId);
        JsonArray sessionRecords = new JsonArray();
        sessionObject.add("entries", sessionRecords);
        int unmatchedCount = 0;
        int matchedCount = 0;
        double accConfidence = 0.0;

        PreparedStatement st = conn.prepareStatement("SELECT * from monitoring_entry where session_id = ?");
        st.setInt(1, botId);
        ResultSet rs = st.executeQuery();
        while (rs.next()) {
            JsonObject entryObject = new JsonObject();
            sessionRecords.add(entryObject);
            entryObject.addProperty("timestamp", rs.getObject("instant", LocalDateTime.class).toInstant(OffsetDateTime.now().getOffset()).toEpochMilli());
            entryObject.addProperty("utterance", rs.getString("utterance"));
            entryObject.addProperty("intent", rs.getString("intent"));
            entryObject.addProperty("confidence", rs.getString("confidence"));
            String intentName = rs.getString("intent");
            if (intentName.equals("Default_Fallback_Intent")) {
                unmatchedCount++;
            } else {
                accConfidence += rs.getDouble("confidence");
                matchedCount++;
            }

        }
        st.close();

        sessionObject.add("matchedUtteranceCount", new JsonPrimitive(matchedCount));
        sessionObject.add("unmatchedUtteranceCount", new JsonPrimitive(unmatchedCount));
        if (matchedCount > 0) {
            sessionObject.add("avgSessionConfidence",
                    new JsonPrimitive(accConfidence / (double) matchedCount));
        }
        return sessionObject;
    }




    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        try {
            this.conn.commit();
            this.conn.close();
        } catch (SQLException e) {
            throw new RuntimeException("Error when shutting down the PostgreSQL monitoring, see the attached "
                    + "exception", e);
        }
    }


    public Connection getConn() {
        return conn;
    }
}

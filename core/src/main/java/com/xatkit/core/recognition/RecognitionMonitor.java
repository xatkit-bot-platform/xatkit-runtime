package com.xatkit.core.recognition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.xatkit.core.server.HttpMethod;
import com.xatkit.core.server.HttpUtils;
import com.xatkit.core.server.RestHandlerException;
import com.xatkit.core.server.RestHandlerFactory;
import com.xatkit.core.server.XatkitServer;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.util.FileUtils;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Objects.isNull;

/**
 * Provides monitoring capabilities for {@link IntentRecognitionProvider}s.
 * <p>
 * This class stores analytics information related to intent recognition, and registers a set of REST endpoints
 * allowing to query them from external applications.
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
 * </ul>
 */
public class RecognitionMonitor {

    /**
     * The {@link Configuration} key to specify a custom data directory to store the analytics.
     * <p>
     * This property is optional, and is set with the value {@code ./data} if it is not specified.
     */
    static final String DATA_DIRECTORY_KEY = "xatkit.data.directory";

    /**
     * The default directory used to store data when no {@link #DATA_DIRECTORY_KEY} is provided in the
     * {@link Configuration}.
     */
    static final String DEFAULT_DATA_DIRECTORY = "data";

    /**
     * The directory used to store analytics-related data within the specified {@code data} directory.
     * <p>
     * This value cannot be changed in the Xatkit {@link Configuration}.
     */
    static final String ANALYTICS_DIRECTORY = "analytics";

    /**
     * The file used to store analytics-related data.
     * <p>
     * This value cannot be changed in the Xatkit {@link Configuration}.
     */
    static final String ANALYTICS_DB_FILE = "analytics.db";

    /**
     * The persistent {@link Map} containing recognition monitoring information.
     * <p>
     * This {@link Map} uses {@code sessionId} as its primary index, and each {@code sessionId} is associated to
     * another {@link Map} containing {@code timestamp -> IntentRecord} bindings.
     * <p>
     * Note that {@code timestamp -> IntentRecord} internal {@link Map}s are ordered, allowing to easily sort
     * {@link IntentRecord}s over time.
     *
     * @see IntentRecord
     */
    private Map<String, Map<Long, IntentRecord>> records;

    /**
     * The database used to persist and load the monitoring information.
     */
    private DB db;

    /**
     * Constructs a {@link RecognitionMonitor} with the provided {@code xatkitServer} and {@code configuration}.
     * <p>
     * This constructor loads the stored information from the <i>analytics</i> database and create the in-memory
     * data structures used to monitor intent recognition providers.
     * <p>
     * This constructor also registers the REST endpoints allowing to query the stored information from external
     * applications.
     * <p>
     * This method also registers a shutdown hook which ensures that the database is closed properly when the JVM is
     * stopped.
     * <p>
     * If the provided {@link Configuration} specifies a value for the {@code xatkit.data.directory} key it will be
     * used as the base location to create the analytics database (in {@code <xatkit.data.directory>/analytics
     * /analytics.db}.
     *
     * @param xatkitServer  the {@link XatkitServer} instance used to register the REST endpoints
     * @param configuration the Xatkit {@link Configuration}
     */
    public RecognitionMonitor(XatkitServer xatkitServer, Configuration configuration) {
        Log.info("Starting intent recognition monitoring");
        String dataDirectoryPath = configuration.getString(DATA_DIRECTORY_KEY, DEFAULT_DATA_DIRECTORY);
        File analyticsDbDirectory = FileUtils.getFile(dataDirectoryPath + File.separator + ANALYTICS_DIRECTORY,
                configuration);
        analyticsDbDirectory.mkdirs();
        db = DBMaker.fileDB(new File(analyticsDbDirectory.getAbsolutePath() + File.separator + ANALYTICS_DB_FILE)).make();

        this.records = (Map<String, Map<Long, IntentRecord>>) db.hashMap("intent_records").createOrOpen();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!this.db.isClosed()) {
                db.commit();
                db.close();
            }
        }));
        this.registerServerEndpoints(xatkitServer);
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
     * Registers the {@code GET: /analytics/monitoring} endpoint.
     * <p>
     * This endpoint returns a JSON array containing all the persisted monitoring information (note that this method
     * doesn't support pagination yet, so the returned JSON may be big for long-running applications).
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
                    for (Map.Entry<String, Map<Long, IntentRecord>> entry : records.entrySet()) {
                        JsonObject sessionObject = buildSessionObject(entry.getKey(), entry.getValue());
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
                    Map<Long, IntentRecord> sessionRecords = records.get(sessionId);
                    if (isNull(sessionRecords)) {
                        throw new RestHandlerException(404, "Session " + sessionId + " not found");
                    } else {
                        return buildSessionObject(sessionId, sessionRecords);
                    }
                })));
    }

    /**
     * Registers the {@code GET: /analytics/monitoring/unmatched} endpoint.
     * <p>
     * This endpoint returns a JSON array containing all the unmatched inputs (i.e. inputs that haven't been
     * successfully translated into intents).
     * <p>
     * The listing below shows an example of the returned JSON payload:
     * <pre>
     * {@code
     * [
     *     {
     *         "sessionId": "72f8fa90-8d3e-4804-b00d-5612a95fb644",
     *         "timestamp": 1573750623741,
     *         "utterance": "Here is something you won't understand!"
     *     },
     *     {
     *         "sessionId": "72f8fa90-8d3e-4804-b00d-5612a95fb644",
     *         "timestamp": 1573750630281,
     *         "utterance": "I knew it"
     *     }
     * ]
     * }
     * </pre>
     *
     * @param xatkitServer the {@link XatkitServer} instance used to register the REST endpoint
     */
    private void registerGetUnmatchedUtterances(XatkitServer xatkitServer) {
        xatkitServer.registerRestEndpoint(HttpMethod.GET, "/analytics/monitoring/unmatched",
                RestHandlerFactory.createJsonRestHandler(((headers, params, content) -> {
                    JsonArray result = new JsonArray();
                    for (Map.Entry<String, Map<Long, IntentRecord>> recordEntry : records.entrySet()) {
                        String sessionId = recordEntry.getKey();
                        for (Map.Entry<Long, IntentRecord> sessionRecordEntry : recordEntry.getValue().entrySet()) {
                            Long timestamp = sessionRecordEntry.getKey();
                            IntentRecord intentRecord = sessionRecordEntry.getValue();
                            if (intentRecord.getIntentName().equals("Default_Fallback_Intent")) {
                                JsonObject unmatchedUtteranceObject = new JsonObject();
                                unmatchedUtteranceObject.addProperty("sessionId", sessionId);
                                unmatchedUtteranceObject.addProperty("timestamp", timestamp);
                                unmatchedUtteranceObject.addProperty("utterance", intentRecord.getUtterance());
                                result.add(unmatchedUtteranceObject);
                            }
                        }
                    }
                    return result;
                })));
    }

    /**
     * Registers the {@code GET: /analytics/monitoring/matched} endpoint.
     * <p>
     * This endpoint returns a JSON array containing all  the matched intents (i.e. inputs that have been
     * successfully translated into intents).
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
                    JsonArray result = new JsonArray();
                    for (Map.Entry<String, Map<Long, IntentRecord>> recordEntry : records.entrySet()) {
                        String sessionId = recordEntry.getKey();
                        for (Map.Entry<Long, IntentRecord> sessionRecordEntry : recordEntry.getValue().entrySet()) {
                            Long timestamp = sessionRecordEntry.getKey();
                            IntentRecord intentRecord = sessionRecordEntry.getValue();
                            if (!intentRecord.getIntentName().equals("Default_Fallback_Intent")) {
                                JsonObject matchedUtteranceObject = new JsonObject();
                                matchedUtteranceObject.addProperty("sessionId", sessionId);
                                matchedUtteranceObject.addProperty("timestamp", timestamp);
                                matchedUtteranceObject.addProperty("utterance", intentRecord.getUtterance());
                                matchedUtteranceObject.addProperty("intent", intentRecord.getIntentName());
                                matchedUtteranceObject.addProperty("confidence",
                                        intentRecord.getRecognitionConfidence());
                                result.add(matchedUtteranceObject);
                            }
                        }
                    }
                    return result;
                }));
    }

    /**
     * Registers the {@code GET: /analytics/monitoring/sessions/stats} endpoint.
     * <p>
     * This endpoint returns a JSON object containing computed statistics over stored sessions (e.g. average
     * time/session, average number of matched inputs/sessions, etc).
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
                    int totalMatchedUtteranceCount = 0;
                    int totalUnmatchedUtteranceCount = 0;
                    long totalSessionTime = 0;
                    for (Map.Entry<String, Map<Long, IntentRecord>> recordEntry : records.entrySet()) {
                        sessionCount++;
                        long sessionStartTimestamp = 0;
                        long sessionStopTimestamp = 0;
                        for (Map.Entry<Long, IntentRecord> sessionRecordEntry : recordEntry.getValue().entrySet()) {
                            long timestamp = sessionRecordEntry.getKey();
                            if (timestamp < sessionStartTimestamp || sessionStartTimestamp == 0) {
                                sessionStartTimestamp = timestamp;
                            }
                            if (timestamp > sessionStopTimestamp) {
                                sessionStopTimestamp = timestamp;
                            }
                            long sessionTime = sessionStopTimestamp - sessionStartTimestamp;
                            totalSessionTime += sessionTime;
                            IntentRecord intentRecord = sessionRecordEntry.getValue();
                            if (intentRecord.getIntentName().equals("Default_Fallback_Intent")) {
                                totalUnmatchedUtteranceCount++;
                            } else {
                                totalMatchedUtteranceCount++;
                            }
                        }
                    }
                    double avgMatchedUtterance = totalMatchedUtteranceCount / (double) sessionCount;
                    double avgUnmatchedUtterance = totalUnmatchedUtteranceCount / (double) sessionCount;
                    double avgSessionTime = totalSessionTime / (double) sessionCount;

                    result.addProperty("averageMatchedUtteranceCount", avgMatchedUtterance);
                    result.addProperty("averageUnmatchedUtteranceCount", avgUnmatchedUtterance);
                    // /1000 for seconds
                    result.addProperty("averageSessionTime", avgSessionTime / 1000);
                    return result;
                })));
    }

    /**
     * Creates a {@link JsonObject} representing the provided session record.
     *
     * @param sessionId   the identifier of the session to translate to a {@link JsonObject}
     * @param sessionData the database records associated to the provided {@code sessionId}
     * @return the created {@link JsonObject}
     */
    private JsonObject buildSessionObject(String sessionId, Map<Long, IntentRecord> sessionData) {
        JsonObject sessionObject = new JsonObject();
        sessionObject.addProperty("sessionId", sessionId);
        JsonArray sessionRecords = new JsonArray();
        sessionObject.add("entries", sessionRecords);
        int unmatchedCount = 0;
        int matchedCount = 0;
        double accConfidence = 0.0;
        for (Map.Entry<Long, IntentRecord> sessionEntry : sessionData.entrySet()) {
            JsonObject entryObject = new JsonObject();
            sessionRecords.add(entryObject);
            entryObject.addProperty("timestamp", sessionEntry.getKey());
            entryObject.addProperty("utterance", sessionEntry.getValue().getUtterance());
            entryObject.addProperty("intent", sessionEntry.getValue().getIntentName());
            entryObject.addProperty("confidence", sessionEntry.getValue().getRecognitionConfidence());
            if (sessionEntry.getValue().getIntentName().equals("Default_Fallback_Intent")) {
                unmatchedCount++;
            } else {
                accConfidence += sessionEntry.getValue().getRecognitionConfidence();
                matchedCount++;
            }
        }
        sessionObject.add("matchedUtteranceCount", new JsonPrimitive(matchedCount));
        sessionObject.add("unmatchedUtteranceCount", new JsonPrimitive(unmatchedCount));
        if (matchedCount > 0) {
            sessionObject.add("avgSessionConfidence",
                    new JsonPrimitive(accConfidence / (double) matchedCount));
        }
        return sessionObject;
    }

    /**
     * Logs the recognition information from the provided {@code recognizedIntent} and {@code session}.
     *
     * @param session          the {@link XatkitSession} from which the {@link RecognizedIntent} has been created
     * @param recognizedIntent the {@link RecognizedIntent} to log
     */
    public void logRecognizedIntent(XatkitSession session, RecognizedIntent recognizedIntent) {
        Long ts = System.currentTimeMillis();
        Map<Long, IntentRecord> sessionMap = records.get(session.getSessionId());
        if (isNull(sessionMap)) {
            sessionMap = new TreeMap<>();
        }
        sessionMap.put(ts, new IntentRecord(recognizedIntent));
        records.put(session.getSessionId(), sessionMap);
        db.commit();
    }


    /**
     * Commit the pending operations on the database and closes the connection.
     */
    public void shutdown() {
        this.db.commit();
        this.db.close();
    }

    /**
     * A database record holding intent-related information.
     */
    private static class IntentRecord implements Serializable {

        private static final long serialVersionUID = 42L;

        /**
         * The utterance that has been mapped to the intent.
         */
        private String utterance;

        /**
         * The name of the intent extracted from the utterance.
         */
        private String intentName;

        /**
         * The confidence level associated to the intent extracted from the utterance.
         * <p>
         * This value is a percentage contained in {@code [0..1]}
         */
        private Float recognitionConfidence;

        public IntentRecord(RecognizedIntent recognizedIntent) {
            this.utterance = recognizedIntent.getMatchedInput();
            this.intentName = recognizedIntent.getDefinition().getName();
            this.recognitionConfidence = recognizedIntent.getRecognitionConfidence();
        }

        public String getUtterance() {
            return this.utterance;
        }

        public String getIntentName() {
            return this.intentName;
        }

        public Float getRecognitionConfidence() {
            return this.recognitionConfidence;
        }

        @Override
        public int hashCode() {
            return this.utterance.hashCode() + this.intentName.hashCode() + this.recognitionConfidence.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof IntentRecord) {
                IntentRecord other = (IntentRecord) obj;
                return other.utterance.equals(this.utterance) && other.intentName.equals(this.intentName)
                        && other.recognitionConfidence.equals(this.recognitionConfidence);
            }
            return super.equals(obj);
        }
    }
}

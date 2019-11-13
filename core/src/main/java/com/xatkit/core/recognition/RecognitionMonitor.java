package com.xatkit.core.recognition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Objects.isNull;

/**
 * Provides monitoring capabilities for {@link IntentRecognitionProvider}s.
 * <p>
 * This class stores analytics information related to (un)matched intents, and registers a set of REST endpoints
 * allowing to query them from external applications. <b>Warning</b>: the REST endpoints must be accessed using the
 * <i>POST</i> method, this is a current limitation of the {@link XatkitServer}.
 * <p>
 * The following endpoints can be used to access the stored information:
 * <ul>
 * <li><b>/analytics/unmatched</b>: returns a JSON object containing a list of inputs that have been received
 * by the bot and hasn't been matched to an intent</li>
 * <li><b>/analytics/matched</b>: returns a JSON object containing the intents matched by the bot and their
 * associated inputs</li>
 * </ul>
 *
// * @see #registerMatchedInput(String, IntentDefinition)
// * @see #registerUnmatchedInput(String)
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
     * The {@link List} of inputs that haven't been matched to any intent.
     */
    private List<String> unmatchedInputs;

//    /**
//     * The {@link Map} containing the intents that have been matched and the corresponding inputs.
//     * <p>
//     * Matched intent information are stored in a dedicated {@link MatchedIntentInfos} instance.
//     *
//     * @see MatchedIntentInfos
//     */
    /*
     * We need to use Strings as the keys of the map because IntentDefinition are not serializable.
     */
//    private Map<String, MatchedIntentInfos> matchedIntents;

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
     * This constructor also registers two REST endpoints allowing to query the stored information from external
     * applications. <b>Warning</b>: the REST endpoints must be accessed using the
     * <i>POST</i> method, this is a current limitation of the {@link XatkitServer}.
     * <p>
     * The following endpoints can be used to access the stored information:
     * <ul>
     * <li><b>/analytics/unmatched</b>: returns a JSON object containing a list of inputs that have been received
     * by the bot and hasn't been matched to an intent</li>
     * <li><b>/analytics/matched</b>: returns a JSON object containing the intents matched by the bot and their
     * associated inputs</li>
     * </ul>
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
//        this.unmatchedInputs = db.indexTreeList("unmatched_inputs", Serializer.STRING).createOrOpen();
//        this.matchedIntents = (Map<String, MatchedIntentInfos>) db.hashMap("matched_intents").createOrOpen();

        this.records = (Map<String, Map<Long, IntentRecord>>) db.hashMap("intent_records").createOrOpen();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!this.db.isClosed()) {
                db.commit();
                db.close();
            }
        }));
        this.registerServerEndpoints(xatkitServer);
    }

//    /**
//     * Registers the REST endpoints used to retrieve monitoring information.
//     *
//     * @param xatkitServer the {@link XatkitServer} instance used to register the REST endpoints
//     */
    private void registerServerEndpoints(XatkitServer xatkitServer) {
        this.registerGetMonitoringData(xatkitServer);
        this.registerGetMonitoringDataForSession(xatkitServer);
        this.registerGetUnmatchedUtterances(xatkitServer);
        this.registerGetMatchedUtterances(xatkitServer);
        this.registerGetSessionsStats(xatkitServer);
//        this.registerUnmatchedEndpoint(xatkitServer);
//        this.registerMatchedEndpoint(xatkitServer);
    }

    private void registerGetMonitoringData(XatkitServer xatkitServer) {
        xatkitServer.registerRestEndpoint("/analytics/monitoring",
                RestHandlerFactory.createJsonRestHandler((headers, param, content) -> {
                    JsonArray result = new JsonArray();
                    for(Map.Entry<String, Map<Long, IntentRecord>> entry : records.entrySet()) {
                        JsonObject sessionObject = buildSessionObject(entry.getKey(), entry.getValue());
                        result.add(sessionObject);

                    }
                    return result;
                }));
    }

    private void registerGetMonitoringDataForSession(XatkitServer xatkitServer) {
        xatkitServer.registerRestEndpoint("/analytics/monitoring/session",
                RestHandlerFactory.createJsonRestHandler(((headers, params, content) -> {
                    String sessionId = content.getAsJsonObject().getAsJsonPrimitive("sessionId").getAsString();
                    Map<Long, IntentRecord> sessionRecords = records.get(sessionId);
                    JsonObject result;
                    if(isNull(sessionRecords)) {
                        result = new JsonObject();
                        result.addProperty("error", "Session " + sessionId + " not found");
                    } else {
                        result = buildSessionObject(sessionId, sessionRecords);
                    }
                    return result;
                })));
    }

    private void registerGetUnmatchedUtterances(XatkitServer xatkitServer) {
        xatkitServer.registerRestEndpoint("/analytics/monitoring/unmatched",
                RestHandlerFactory.createJsonRestHandler(((headers, params, content) -> {
                    JsonArray result = new JsonArray();
                    for(Map.Entry<String, Map<Long, IntentRecord>> recordEntry : records.entrySet()) {
                        String sessionId = recordEntry.getKey();
                        for(Map.Entry<Long, IntentRecord> sessionRecordEntry : recordEntry.getValue().entrySet()) {
                            Long timestamp = sessionRecordEntry.getKey();
                            IntentRecord intentRecord = sessionRecordEntry.getValue();
                            if(intentRecord.getIntentName().equals("Default_Fallback_Intent")) {
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

    private void registerGetMatchedUtterances(XatkitServer xatkitServer) {
        xatkitServer.registerRestEndpoint("/analytics/monitoring/matched",
                RestHandlerFactory.createJsonRestHandler((headers, params, content) -> {
                    JsonArray result = new JsonArray();
                    for(Map.Entry<String, Map<Long, IntentRecord>> recordEntry : records.entrySet()) {
                        String sessionId = recordEntry.getKey();
                        for(Map.Entry<Long, IntentRecord> sessionRecordEntry : recordEntry.getValue().entrySet()) {
                            Long timestamp = sessionRecordEntry.getKey();
                            IntentRecord intentRecord = sessionRecordEntry.getValue();
                            if(!intentRecord.getIntentName().equals("Default_Fallback_Intent")) {
                                JsonObject matchedUtteranceObject = new JsonObject();
                                matchedUtteranceObject.addProperty("sessionId", sessionId);
                                matchedUtteranceObject.addProperty("timestamp", timestamp);
                                matchedUtteranceObject.addProperty("utterance", intentRecord.getUtterance());
                                matchedUtteranceObject.addProperty("intent", intentRecord.getIntentName());
                                matchedUtteranceObject.addProperty("confidence", intentRecord.getRecognitionConfidence());
                                result.add(matchedUtteranceObject);
                            }
                        }
                    }
                    return result;
                }));
    }

    private void registerGetSessionsStats(XatkitServer xatkitServer) {
        xatkitServer.registerRestEndpoint("/analytics/monitoring/sessions/stats",
                RestHandlerFactory.createJsonRestHandler(((headers, params, content) -> {
                    JsonObject result = new JsonObject();
                    int sessionCount = 0;
                    int totalMatchedUtteranceCount = 0;
                    int totalUnmatchedUtteranceCount = 0;
                    long totalSessionTime = 0;
                    for(Map.Entry<String, Map<Long, IntentRecord>> recordEntry : records.entrySet()) {
                        sessionCount++;
                        long sessionStartTimestamp = 0;
                        long sessionStopTimestamp = 0;
                        for(Map.Entry<Long, IntentRecord> sessionRecordEntry : recordEntry.getValue().entrySet()) {
                            long timestamp = sessionRecordEntry.getKey();
                            if(timestamp < sessionStartTimestamp || sessionStartTimestamp == 0) {
                                sessionStartTimestamp = timestamp;
                            }
                            if(timestamp > sessionStopTimestamp) {
                                sessionStopTimestamp = timestamp;
                            }
                            long sessionTime = sessionStopTimestamp - sessionStartTimestamp;
                            totalSessionTime += sessionTime;
                            IntentRecord intentRecord = sessionRecordEntry.getValue();
                            if(intentRecord.getIntentName().equals("Default_Fallback_Intent")) {
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

    private JsonObject buildSessionObject(String sessionId, Map<Long, IntentRecord> sessionData) {
        JsonObject sessionObject = new JsonObject();
        sessionObject.addProperty("sessionId", sessionId);
        JsonArray sessionRecords = new JsonArray();
        sessionObject.add("entries", sessionRecords);
        int unmatchedCount = 0;
        int matchedCount = 0;
        for(Map.Entry<Long, IntentRecord> sessionEntry : sessionData.entrySet()) {
            JsonObject entryObject = new JsonObject();
            sessionRecords.add(entryObject);
            entryObject.addProperty("timestamp", sessionEntry.getKey());
            entryObject.addProperty("utterance", sessionEntry.getValue().getUtterance());
            entryObject.addProperty("intent", sessionEntry.getValue().getIntentName());
            entryObject.addProperty("confidence", sessionEntry.getValue().getRecognitionConfidence());
            if(sessionEntry.getValue().getIntentName().equals("Default_Fallback_Intent")) {
                unmatchedCount++;
            } else {
                matchedCount++;
            }
        }
        sessionObject.add("matchedUtteranceCount", new JsonPrimitive(matchedCount));
        sessionObject.add("unmatchedUtteranceCount", new JsonPrimitive(unmatchedCount));
        return sessionObject;
    }

//    /**
//     * Registers the {@code /analytics/unmatched} REST endpoint.
//     * <p>
//     * This endpoint returns a JSON object containing the following information:
//     * <pre>
//     * {@code
//     * {
//     *     "inputs": [
//     *         "Hi",
//     *         "Hello"
//     *     ]
//     * }
//     * }
//     * </pre>
//     *
//     * @param xatkitServer the {@link XatkitServer} instance used to register the REST endpoint
//     */
//    private void registerUnmatchedEndpoint(XatkitServer xatkitServer) {
//        xatkitServer.registerRestEndpoint("/analytics/unmatched",
//                RestHandlerFactory.createJsonRestHandler((headers, param, content) -> {
//                    JsonObject result = new JsonObject();
//                    JsonArray array = new JsonArray();
//                    result.add("inputs", array);
//                    for (String unmatchedInput : this.unmatchedInputs) {
//                        array.add(new JsonPrimitive(unmatchedInput));
//                    }
//                    return result;
//                }));
//    }
//
//    /**
//     * Registers the {@code /analytics/matched} REST endpoint.
//     * <p>
//     * This endpoint returns a JSON object containing the following information:
//     * <pre>
//     * {@code
//     * {
//     *     "intents": [
//     *         {
//     *             "name": "CanYou",
//     *             "times": 3,
//     *             "inputs": [
//     *                 {
//     *                     "value": "Can you sing?",
//     *                     "times": 1
//     *                 },
//     *                 {
//     *                     "value": "Can you eat?",
//     *                     "times": 1
//     *                 },
//     *                 {
//     *                     "value": "Can you dance?",
//     *                     "times": 1
//     *                 }
//     *             ]
//     *         }
//     *     ]
//     * }
//     * }
//     * </pre>
//     *
//     * @param xatkitServer the {@link XatkitServer} instance used to register the REST endpoint
//     */
//    private void registerMatchedEndpoint(XatkitServer xatkitServer) {
//        xatkitServer.registerRestEndpoint("/analytics/matched",
//                RestHandlerFactory.createJsonRestHandler((headers, param, content) -> {
//                    JsonObject result = new JsonObject();
//                    JsonArray array = new JsonArray();
//                    result.add("intents", array);
//                    for (Map.Entry<String, MatchedIntentInfos> entry : this.matchedIntents.entrySet()) {
//                        JsonObject intentObject = new JsonObject();
//                        array.add(intentObject);
//                        intentObject.add("name", new JsonPrimitive(entry.getKey()));
//                        intentObject.add("times", new JsonPrimitive(entry.getValue().getAllInputCounts()));
//                        JsonArray inputsArray = new JsonArray();
//                        intentObject.add("inputs", inputsArray);
//                        for (Map.Entry<String, Integer> inputCount : entry.getValue().getInputCounts().entrySet()) {
//                            JsonObject inputObject = new JsonObject();
//                            inputObject.add("value", new JsonPrimitive(inputCount.getKey()));
//                            inputObject.add("times", new JsonPrimitive(inputCount.getValue()));
//                            inputsArray.add(inputObject);
//                        }
//                    }
//                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
//                    Log.info(gson.toJson(result));
//                    return result;
//                }));
//    }


    public void logRecognizedIntent(XatkitSession session, RecognizedIntent recognizedIntent) {
        Long ts = System.currentTimeMillis();
        Map<Long, IntentRecord> sessionMap = records.get(session.getSessionId());
        if(isNull(sessionMap)) {
            sessionMap = new TreeMap<>();
        }
        sessionMap.put(ts, new IntentRecord(recognizedIntent));
        records.put(session.getSessionId(), sessionMap);
        db.commit();
    }


//    /**
//     * Registers a unmatched {@code input}.
//     * <p>
//     * The input can be accessed through the {@code /analytics/unmatched} REST endpoint.
//     *
//     * @param input the unmatched input
//     */
//    public void registerUnmatchedInput(String input) {
//        this.unmatchedInputs.add(input);
//        db.commit();
//    }
//
//    /**
//     * Registers a matched {@code input} and the corresponding {@code intent}.
//     * <p>
//     * The match record can be accessed through the {@code /analytics/matched} REST endpoint.
//     *
//     * @param input  the matched input
//     * @param intent the corresponding {@link IntentDefinition}
//     */
//    public void registerMatchedInput(String input, IntentDefinition intent) {
//        MatchedIntentInfos matchedIntentInfos = this.matchedIntents.get(intent.getName());
//        if (isNull(matchedIntentInfos)) {
//            matchedIntentInfos = new MatchedIntentInfos();
//        }
//        matchedIntentInfos.addInput(input);
//        /*
//         * MapDB put method creates a copy of the stored object, meaning that we can't update it directly, we need to
//         * put() it once all the modifications have been performed.
//         */
//        this.matchedIntents.put(intent.getName(), matchedIntentInfos);
//        db.commit();
//    }

    /**
     * Commit the pending operations on the database and closes the connection.
     */
    public void shutdown() {
        this.db.commit();
        this.db.close();
    }

    private static class IntentRecord implements Serializable {

        private static final long serialVersionUID = 42L;

        private String utterance;

        private String intentName;

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

//    /**
//     * Records matched intent information that are serialized in the {@link DB}.
//     * <p>
//     * This class tracks all the inputs that have been matched to a given {@link IntentDefinition}, and counts the
//     * number of occurrences for each one.
//     */
//    private static class MatchedIntentInfos implements Serializable {
//
//        /**
//         * The serialization version UID.
//         */
//        private static final long serialVersionUID = 42L;
//
//        /**
//         * The {@link Map} storing the matched inputs and the number of occurrences.
//         *
//         * @see #getInputCounts()
//         * @see #addInput(String)
//         */
//        private Map<String, Integer> inputCounts;
//
//
//        /**
//         * Creates a {@link MatchedIntentInfos} with empty records.
//         */
//        public MatchedIntentInfos() {
//            this.inputCounts = new HashMap<>();
//        }
//
//        /**
//         * Adds the provided {@code input} to the record and update the associated count.
//         *
//         * @param input the input to store
//         */
//        public void addInput(String input) {
//            Integer inputCount = inputCounts.get(input);
//            if (isNull(inputCount)) {
//                inputCounts.put(input, 1);
//            } else {
//                inputCounts.put(input, inputCount + 1);
//            }
//        }
//
//        /**
//         * Returns the {@link Map} containing the stored matched inputs and the associated number of occurrences.
//         *
//         * @return the {@link Map} containing the stored matched inputs and the associated number of occurrences
//         */
//        public Map<String, Integer> getInputCounts() {
//            return this.inputCounts;
//        }
//
//        /**
//         * Returns the number of inputs stored in this record.
//         *
//         * @return the number of inputs stored in this record
//         */
//        public int getAllInputCounts() {
//            return inputCounts.values().stream().reduce(Integer::sum).orElse(0);
//        }
//
//
//    }
}

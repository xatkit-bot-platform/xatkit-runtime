package edu.uoc.som.jarvis.core.recognition;

import com.google.gson.*;
import edu.uoc.som.jarvis.core.server.JarvisServer;
import edu.uoc.som.jarvis.intent.IntentDefinition;
import fr.inria.atlanmod.commons.log.Log;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

/**
 * Provides monitoring capabilities for {@link IntentRecognitionProvider}s.
 * <p>
 * This class stores analytics information related to (un)matched intents, and registers a set of REST endpoints
 * allowing to query them from external applications. <b>Warning</b>: the REST endpoints must be accessed using the
 * <i>POST</i> method, this is a current limitation of the {@link edu.uoc.som.jarvis.core.server.JarvisServer}.
 * <p>
 * The following endpoints can be used to access the stored information:
 * <ul>
 * <li><b>/analytics/unmatched</b>: returns a JSON object containing a list of inputs that have been received
 * by the bot and hasn't been matched to an intent</li>
 * <li><b>/analytics/matched</b>: returns a JSON object containing the intents matched by the bot and their
 * associated inputs</li>
 * </ul>
 *
 * @see #registerMatchedInput(String, IntentDefinition)
 * @see #registerUnmatchedInput(String)
 */
public class RecognitionMonitor {

    /**
     * The path of the folder containing the analytics data.
     */
    static final String ANALYTICS_DB_FOLDER = "data" + File.separator + "analytics";

    /**
     * The path of the analytics database.
     * <p>
     * This database is located in the {@link #ANALYTICS_DB_FOLDER}, and represents the concrete database storing the
     * analytics results.
     */
    static final String ANALYTICS_DB_FILE = ANALYTICS_DB_FOLDER + File.separator + "analytics.db";

    /**
     * The {@link List} of inputs that haven't been matched to any intent.
     */
    private List<String> unmatchedInputs;

    /**
     * The {@link Map} containing the intents that have been matched and the corresponding inputs.
     * <p>
     * Matched intent information are stored in a dedicated {@link MatchedIntentInfos} instance.
     *
     * @see MatchedIntentInfos
     */
    /*
     * We need to use Strings as the keys of the map because IntentDefinition are not serializable.
     */
    private Map<String, MatchedIntentInfos> matchedIntents;

    /**
     * The database used to persist and load the monitoring information.
     */
    private DB db;

    /**
     * Constructs a {@link RecognitionMonitor} with the provided {@code jarvisServer}.
     * <p>
     * This constructor loads the stored information from the <i>analytics</i> database and create the in-memory
     * data structures used to monitor intent recognition providers.
     * <p>
     * This constructor also registers two REST endpoints allowing to query the stored information from external
     * applications. <b>Warning</b>: the REST endpoints must be accessed using the
     * <i>POST</i> method, this is a current limitation of the {@link edu.uoc.som.jarvis.core.server.JarvisServer}.
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
     *
     * @param jarvisServer the {@link JarvisServer} instance used to register the REST endpoints
     */
    public RecognitionMonitor(JarvisServer jarvisServer) {
        Log.info("Starting intent recognition monitoring");
        File analyticsDbFolder = new File(ANALYTICS_DB_FOLDER);
        analyticsDbFolder.mkdirs();
        db = DBMaker.fileDB(new File(ANALYTICS_DB_FILE)).make();
        this.unmatchedInputs = db.indexTreeList("unmatched_inputs", Serializer.STRING).createOrOpen();
        this.matchedIntents = (Map<String, MatchedIntentInfos>) db.hashMap("matched_intents").createOrOpen();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!this.db.isClosed()) {
                db.commit();
                db.close();
            }
        }));
        this.registerServerEndpoints(jarvisServer);
    }

    /**
     * Registers the REST endpoints used to retrieve monitoring information.
     *
     * @param jarvisServer the {@link JarvisServer} instance used to register the REST endpoints
     */
    private void registerServerEndpoints(JarvisServer jarvisServer) {
        this.registerUnmatchedEndpoint(jarvisServer);
        this.registerMatchedEndpoint(jarvisServer);
    }

    /**
     * Registers the {@code /analytics/unmatched} REST endpoint.
     * <p>
     * This endpoint returns a JSON object containing the following information:
     * <pre>
     * {@code
     * {
     *     "inputs": [
     *         "Hi",
     *         "Hello"
     *     ]
     * }
     * }
     * </pre>
     *
     * @param jarvisServer the {@link JarvisServer} instance used to register the REST endpoint
     */
    private void registerUnmatchedEndpoint(JarvisServer jarvisServer) {
        jarvisServer.registerRestEndpoint("/analytics/unmatched",
                (headers, param, content) -> {
                    JsonObject result = new JsonObject();
                    JsonArray array = new JsonArray();
                    result.add("inputs", array);
                    for (String unmatchedInput : this.unmatchedInputs) {
                        array.add(new JsonPrimitive(unmatchedInput));
                    }
                    return result;
                });
    }

    /**
     * Registers the {@code /analytics/matched} REST endpoint.
     * <p>
     * This endpoint returns a JSON object containing the following information:
     * <pre>
     * {@code
     * {
     *     "intents": [
     *         {
     *             "name": "CanYou",
     *             "times": 3,
     *             "inputs": [
     *                 {
     *                     "value": "Can you sing?",
     *                     "times": 1
     *                 },
     *                 {
     *                     "value": "Can you eat?",
     *                     "times": 1
     *                 },
     *                 {
     *                     "value": "Can you dance?",
     *                     "times": 1
     *                 }
     *             ]
     *         }
     *     ]
     * }
     * }
     * </pre>
     *
     * @param jarvisServer the {@link JarvisServer} instance used to register the REST endpoint
     */
    private void registerMatchedEndpoint(JarvisServer jarvisServer) {
        jarvisServer.registerRestEndpoint("/analytics/matched",
                (headers, param, content) -> {
                    JsonObject result = new JsonObject();
                    JsonArray array = new JsonArray();
                    result.add("intents", array);
                    for (Map.Entry<String, MatchedIntentInfos> entry : this.matchedIntents.entrySet()) {
                        JsonObject intentObject = new JsonObject();
                        array.add(intentObject);
                        intentObject.add("name", new JsonPrimitive(entry.getKey()));
                        intentObject.add("times", new JsonPrimitive(entry.getValue().getAllInputCounts()));
                        JsonArray inputsArray = new JsonArray();
                        intentObject.add("inputs", inputsArray);
                        for (Map.Entry<String, Integer> inputCount : entry.getValue().getInputCounts().entrySet()) {
                            JsonObject inputObject = new JsonObject();
                            inputObject.add("value", new JsonPrimitive(inputCount.getKey()));
                            inputObject.add("times", new JsonPrimitive(inputCount.getValue()));
                            inputsArray.add(inputObject);
                        }
                    }
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    Log.info(gson.toJson(result));
                    return result;
                });
    }

    /**
     * Registers a unmatched {@code input}.
     * <p>
     * The input can be accessed through the {@code /analytics/unmatched} REST endpoint.
     *
     * @param input the unmatched input
     */
    public void registerUnmatchedInput(String input) {
        this.unmatchedInputs.add(input);
        db.commit();
    }

    /**
     * Registers a matched {@code input} and the corresponding {@code intent}.
     * <p>
     * The match record can be accessed through the {@code /analytics/matched} REST endpoint.
     *
     * @param input  the matched input
     * @param intent the corresponding {@link IntentDefinition}
     */
    public void registerMatchedInput(String input, IntentDefinition intent) {
        MatchedIntentInfos matchedIntentInfos = this.matchedIntents.get(intent.getName());
        if (isNull(matchedIntentInfos)) {
            matchedIntentInfos = new MatchedIntentInfos();
        }
        matchedIntentInfos.addInput(input);
        /*
         * MapDB put method creates a copy of the stored object, meaning that we can't update it directly, we need to
         * put() it once all the modifications have been performed.
         */
        this.matchedIntents.put(intent.getName(), matchedIntentInfos);
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
     * Records matched intent information that are serialized in the {@link DB}.
     * <p>
     * This class tracks all the inputs that have been matched to a given {@link IntentDefinition}, and counts the
     * number of occurrences for each one.
     */
    private static class MatchedIntentInfos implements Serializable {

        /**
         * The serialization version UID.
         */
        private static final long serialVersionUID = 42L;

        /**
         * The {@link Map} storing the matched inputs and the number of occurrences.
         *
         * @see #getInputCounts()
         * @see #addInput(String)
         */
        private Map<String, Integer> inputCounts;


        /**
         * Creates a {@link MatchedIntentInfos} with empty records.
         */
        public MatchedIntentInfos() {
            this.inputCounts = new HashMap<>();
        }

        /**
         * Adds the provided {@code input} to the record and update the associated count.
         *
         * @param input the input to store
         */
        public void addInput(String input) {
            Integer inputCount = inputCounts.get(input);
            if (isNull(inputCount)) {
                inputCounts.put(input, 1);
            } else {
                inputCounts.put(input, inputCount + 1);
            }
        }

        /**
         * Returns the {@link Map} containing the stored matched inputs and the associated number of occurrences.
         *
         * @return the {@link Map} containing the stored matched inputs and the associated number of occurrences
         */
        public Map<String, Integer> getInputCounts() {
            return this.inputCounts;
        }

        /**
         * Returns the number of inputs stored in this record.
         *
         * @return the number of inputs stored in this record
         */
        public int getAllInputCounts() {
            return inputCounts.values().stream().reduce(Integer::sum).orElse(0);
        }


    }
}

package com.xatkit.core.recognition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.xatkit.core.server.HttpMethod;
import com.xatkit.core.server.HttpUtils;
import com.xatkit.core.server.RestHandlerException;
import com.xatkit.core.server.RestHandlerFactory;
import com.xatkit.core.server.XatkitServer;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.isNull;

/**
 * Provides monitoring capabilities for {@link IntentRecognitionProvider}s.
 * <p>
 * This class stores analytics information related to intent recognition, and registers a set of REST endpoints
 * allowing to query them from external applications.
 * Also, this class manages the connection to an external database (InfluxDB) and stores the information.
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
public class RecognitionMonitorInflux implements RecognitionMonitor {

    /**
     * The {@link Configuration} key to specify the auth token for the bot to be able to store/query data from an
     * influx bucket.
     * <p>
     * This property is mandatory.
     */
    public static final String INFLUX_TOKEN_KEY = "xatkit.influx.token";

    /**
     * The TOKEN value specified in the {@link Configuration}, necessary to make petitions to the database.
     */
    private char[] token;

    /**
     * The {@link Configuration} key to specify a custom bucket instance to store the analytics.
     */
    public static final String INFLUX_BUCKET_KEY = "xatkit.influx.bucket";

    /**
     * The BUCKET value specified in the {@link Configuration}, necessary for the petitions to the database.
     */
    private String bucket;

    /**
     * The default value for BUCKET
     */
    public static final String DEFAULT_BUCKET = "xatbot";

    /**
     * The {@link Configuration} key to specify a custom organization workspace for influx.
     */
    public static final String INFLUX_ORG_KEY = "xatkit.influx.organization";

    /**
     * The ORGANIZATION value specified in the {@link Configuration}, necessary for the petitions to the database.
     */
    private String organization;

    /**
     * The default value for ORGANIZATION
     */
    public static final String DEFAULT_ORGANIZATION = "Xatkit";

    /**
     * The databases url key.
     * <p>
     * This property is optional and will use "http://localhost:7777" as default
     */
    public static final String INFLUX_URL_KEY = "xatkit.influx.url";

    /**
     * The default value for the URL of influx instance
     */
    public static final String DEFAULT_URL = "http://localhost:7777";

    /**
     * The bot ID key.
     * <p>
     * This property is optional and defaults to {@code "xatkitBot} if not specified.
     */
    public static final String INFLUX_BOT_ID_KEY = "xatkit.influx.bot_id";

    /**
     * The default ID of the bot
     */
    public static final String DEFAULT_BOT_ID = "xatkitBot";

    /**
     * The actual ID of the bot
     */
    private String bot_Id;

    /**
     * The database persistent client to make the petitions :)
     */
    private InfluxDBClient db;

    /**
     * Influxdb stores data, by default in "/var/lib/influxdb/wal" or "/var/lib/influxdb/data"
     * based on it's configuration: wal files are "temporal" until they reach 25MB (default config)
     * or when the data has been there for 10 minutes. This can be changed in the [data] section from influxdb.conf
     * To be able to query/input data with influx we need the following information:
     * TOKEN: this validates our bot into the database, so it will be authenticated.
     * BUCKET: named location where data is stored. It has a retention policy, a duration of time that each data
     * point persists, etc.
     * A bucket belongs to 1 organization.
     * ORGANIZATION:        it's the workspace/group of users. It can own multiple buckets.
     *
     * @param xatkitServer  the {@link XatkitServer} instance used to register the REST endpoints
     * @param configuration the Xatkit {@link Configuration}
     */
    public RecognitionMonitorInflux(XatkitServer xatkitServer, Configuration configuration) {
        Log.info("Starting new intent recognition monitoring with Influxdb");
        checkArgument(configuration.containsKey(INFLUX_TOKEN_KEY), "Cannot connect to the InfluxDB database, please " +
                "provide a valid token in the configuration (key: %s)", INFLUX_TOKEN_KEY);
        // Required for storing/querying data from influx:
        token = configuration.getString(INFLUX_TOKEN_KEY).toCharArray();
        bucket = configuration.getString(INFLUX_BUCKET_KEY, DEFAULT_BUCKET);
        organization = configuration.getString(INFLUX_ORG_KEY, DEFAULT_ORGANIZATION);
        bot_Id = configuration.getString(INFLUX_BOT_ID_KEY, DEFAULT_BOT_ID);
        String url = configuration.getString(INFLUX_URL_KEY, DEFAULT_URL);
        Log.info("Bucket: {0}", bucket);
        Log.info("Organization: {0}", organization);
        Log.info("Influxdb url: {0}", url);
        db = InfluxDBClientFactory.create(url, token, organization, bucket);
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
        this.registerGetOriginStats(xatkitServer);
    }

    /**
     * Resolves a simple query applying the given filters.
     *
     * @param filter
     * @return JsonArray
     * <p>
     * <pre>
     * {@code
     * [
     *     {
     *         "bot_id": "Xatkit bot",
     *         "is_Matched": true,
     *         "session_id": "72f8fa90-8d3e-4804-b00d-5612a95fb644",
     *         "origin": "xatkit.com",
     *         "platform": "react",
     *         "username": "Lluís",
     *         "confidence": 1.0
     *         "utterance": "How are you?",
     *         "intent": "HowAreYou",
     *         "parameters": "params"
     *         "timestamp": 1573750605388,
     *     }
     * ]
     * }
     * </pre>
     */
    private JsonArray basicQuery(String[] filter) {
        JsonArray res = new JsonArray();
        String query = queryBuilder("", filter, true, false);
        List<FluxTable> tables = db.getQueryApi().query(query);
        for (FluxTable table : tables) {
            //Each table equals to 1 session with the current Query
            List<FluxRecord> records = table.getRecords();
            for (FluxRecord record : records) {
                //Each record should hold the info of an utterance/intent for that session
                JsonObject obj = getIntentData(record);
                res.add(obj);
            }
        }
        return res;
    }

    /**
     * Registers the {@code GET: /analytics/monitoring/matched} endpoint.
     * <p>
     * This endpoint returns a JSON array containing all the matched intents (i.e. inputs that have been
     * successfully translated into intents).
     * <p>
     *
     * @param xatkitServer the {@link XatkitServer} instance used to register the REST endpoint
     */
    private void registerGetMatchedUtterances(XatkitServer xatkitServer) {
        xatkitServer.registerRestEndpoint(HttpMethod.GET, "/analytics/monitoring/matched",
                RestHandlerFactory.createJsonRestHandler((headers, params, content) -> {
                    JsonArray res = new JsonArray();
                    //Query database for matched intents and retrieve them.
                    String[] filter = {"r.is_Matched == \"true\""};
                    res = basicQuery(filter);
                    return res;
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
                    JsonArray res = new JsonArray();
                    //Query database for matched intents and retrieve them.
                    String[] aux = {"r.is_Matched == \"false\""};
                    res = basicQuery(aux);
                    return res;
                })
        );
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
                            String[] filters = {};
                            String query = queryBuilder("", filters, true, false);
                            query = query.concat("|> group(columns: [\"session_id\"])");

                            List<FluxTable> tables = db.getQueryApi().query(query);
                            sessionCount = tables.size(); //Data is grouped by session_id, which means tables.size()
                    // = nr sessions
                            //iterating through each session table to calculate it's time
                            for (FluxTable table : tables) {
                                //Assumption: Tables are ordered by time so pos 0 = older one and size()-1 contains
                                // the most recent one
                                List<FluxRecord> records = table.getRecords();
                                long timeStart =
                                        Instant.parse(String.valueOf(records.get(0).getValueByKey("_time"))).toEpochMilli();
                                long timeEnd =
                                        Instant.parse(String.valueOf(records.get(records.size() - 1).getValueByKey(
                                                "_time"))).toEpochMilli();
                                totalSessionTime += (timeEnd - timeStart);
                            }
                            filters = new String[]{"r.is_Matched == \"true\""};
                            query = queryBuilder("", filters, true, true);
                            tables = db.getQueryApi().query(query);
                            //Data is grouped and filtered which means tables.size() = 1 with nr matched utts as rows
                            totalMatchedUtteranceCount = tables.get(0).getRecords().size();
                            filters = new String[]{"r.is_Matched == \"false\""};
                            query = queryBuilder("", filters, true, true);
                            tables = db.getQueryApi().query(query);
                            //Data is grouped and filtered which means tables.size() = 1 with nr unmatched utts as rows
                            totalUnmatchedUtteranceCount = tables.get(0).getRecords().size();
                            double avgMatchedUtterances = totalMatchedUtteranceCount / (double) sessionCount;
                            double avgUnmatchedUtterances = totalUnmatchedUtteranceCount / (double) sessionCount;
                            double avgSessionTime = totalSessionTime / (double) sessionCount;
                            result.addProperty("averageMatchedUtteranceCount", avgMatchedUtterances);
                            result.addProperty("averageUnmatchedUtteranceCount", avgUnmatchedUtterances);
                            result.addProperty("averageSessionTime", avgSessionTime / 1000); //divided by 1000 to get
                    // value in seconds
                            return result;
                        })
                ));
    }

    /**
     * @param xatkitServer
     */
    private void registerGetMonitoringDataForSession(XatkitServer xatkitServer) {
        xatkitServer.registerRestEndpoint(HttpMethod.GET, "/analytics/monitoring/session",
                RestHandlerFactory.createJsonRestHandler(((headers, params, content) -> {
                            String sessionId = HttpUtils.getParameterValue("sessionId", params);
                            if (isNull(sessionId)) {
                                throw new RestHandlerException(404, "Missing parameter sessionId");
                            }
                            String[] filters = {"r.session_id == \"" + sessionId + "\""};
                            String query = queryBuilder("", filters, true, true);
                            //Tables will contain an unique table with all utterances/intents for that session,
                    // should be ordered by timestamp
                            List<FluxTable> tables = db.getQueryApi().query(query);
                            if (tables.size() == 0) {
                                throw new RestHandlerException(404, "Session " + sessionId + " not found");
                            } else {
                                return buildSessionObject(sessionId, tables.get(0).getRecords());
                            }
                        })
                ));
    }

    /**
     * Registers the {@code GET: /analytics/monitoring} endpoint.
     * <p>
     * This endpoint returns a JSON array containing all the persisted monitoring information (note that this method
     * doesn't support pagination yet, so the returned JSON may be big for long-running applications).
     * <p>
     *
     * @param xatkitServer the {@link XatkitServer} instance used to register the REST endpoint
     */
    private void registerGetMonitoringData(XatkitServer xatkitServer) {
        xatkitServer.registerRestEndpoint(HttpMethod.GET, "/analytics/monitoring",
                RestHandlerFactory.createJsonRestHandler((headers, param, content) -> {
                    JsonArray sessionsArray = new JsonArray();
                    JsonObject globalInfo = new JsonObject();
                    double accRecognitionConfidence = 0.0;
                    int matchedCount = 0;
                    int unmatchedCount = 0;
                    int nSessions = 0;
                    //query pivoted and grouped by sessionID
                    String[] filters = {};
                    String query = queryBuilder("", filters, true, true);
                    query = query.concat("|> group(columns: [\"session_id\"])");
                    List<FluxTable> tables = db.getQueryApi().query(query);
                    //Each table equals to 1 session
                    nSessions = tables.size();
                    for (FluxTable table : tables) {
                        String sessionId = String.valueOf(table.getRecords().get(0).getValueByKey("session_id"));
                        JsonObject sessionObject = buildSessionObject(sessionId, table.getRecords());
                        int sessionMatchedCount = sessionObject.get("matchedUtteranceCount").getAsInt();
                        matchedCount += sessionMatchedCount;
                        unmatchedCount += sessionObject.get("unmatchedUtteranceCount").getAsInt();
                        if (sessionObject.has("avgSessionConfidence")) {
                            double avgSessionConfidence = sessionObject.get("avgSessionConfidence").getAsDouble();
                            accRecognitionConfidence += avgSessionConfidence * (double) sessionMatchedCount;
                        }
                        sessionsArray.add(sessionObject);
                    }
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
                })
        );
    }

    /**
     * Registers the {@code GET: /analytics/origin} endpoint.
     * <p>
     * This endpoint returns a JSON object containing origins' information (note that this method
     * doesn't support pagination yet, so the returned JSON may be big for long-running applications).
     * <p>
     *
     * @param xatkitServer the {@link XatkitServer} instance used to register the REST endpoint
     */
    private void registerGetOriginStats(XatkitServer xatkitServer) {
        xatkitServer.registerRestEndpoint(HttpMethod.GET, "/analytics/origin",
                RestHandlerFactory.createJsonRestHandler((headers, param, content) -> {
                    JsonObject result = new JsonObject();
                    JsonArray originArray = new JsonArray();
                    result.add("Origins", originArray);
                    int totalSessions = 0;
                    //query pivoted and grouped by sessionID
                    String[] filters = {};
                    String query = queryBuilder("", filters, true, false);
                    query = query.concat("|> group(columns: [\"origin\"])");
                    //Each table equals 1 origin, so list lenght = number of different origins
                    List<FluxTable> tables = db.getQueryApi().query(query);
                    int numberOrigins = tables.size();
                    for (FluxTable table : tables) {
                        //We have to get the field from "origin", which is common in the entire table
                        //And the number of different sessions for that origin.
                        //Create JsonObject with the values of each origin to be inserted into the array:
                        JsonObject aux = new JsonObject();
                        try {
                            String origin = table.getRecords().get(0).getValueByKey("origin").toString();
                            //Now search the unique sessions in a new Query
                            String[] filterByOrigin = {"r.origin == \"" + origin + "\""};
                            String query2 = queryBuilder("", filterByOrigin, true, false);
                            query2 = query2.concat("|> group(columns: [\"session_id\"])");
                            //The number of tables equal to the number of sessions in that origin
                            int nrSessions = db.getQueryApi().query(query2).size();
                            totalSessions += nrSessions;
                            aux.addProperty("origin", origin);
                            aux.addProperty("nrSessions", nrSessions);
                        } catch (Exception e) {
                            //We got a case where we have no origins, or origin is blank/null
                            //but I did not find a way to query "null" columns in influxdb :S
                            //So, I made a SET instance, which does not accept duplicates, but takes O(n) to create.
                            //This way we can see how many unique session_id entries do not have an "origin" value in
                            // the database.
                            Set<String> sessionSet = new HashSet<String>();
                            for (FluxRecord record : table.getRecords()) {
                                String id = String.valueOf(record.getValueByKey("session_id"));
                                if (!sessionSet.contains(id)) {
                                    sessionSet.add(id);
                                }
                            }
                            totalSessions += sessionSet.size();
                            aux.addProperty("origin", "");
                            aux.addProperty("nrSessions", sessionSet.size());
                        }
                        originArray.add(aux);
                    }
                    double avgSessPerOrigin;
                    if (numberOrigins > 0) avgSessPerOrigin = (double) totalSessions / (double) numberOrigins;
                    else avgSessPerOrigin = 0; //Prevent nulls/negative numbers
                    result.add("avgSessionsPerOrigin", new JsonPrimitive(avgSessPerOrigin));
                    result.add("nrOrigins", new JsonPrimitive(numberOrigins));
                    return result;
                })
        );
    }

    /**
     * Creates a {@link JsonObject} representing the provided session record.
     *
     * @param sessionId   the identifier of the session to translate to a {@link JsonObject}
     * @param sessionData the database records associated to the provided {@code sessionId}
     * @return the created {@link JsonObject}
     */
    private JsonObject buildSessionObject(String sessionId, List<FluxRecord> sessionData) {
        JsonObject sessionObject = new JsonObject();
        sessionObject.addProperty("sessionId", sessionId);
        JsonArray sessionRecords = new JsonArray();
        sessionObject.add("entries", sessionRecords);
        int unmatchedCount = 0;
        int matchedCount = 0;
        double accConfidence = 0.0;
        for (FluxRecord sessionEntry : sessionData) {
            JsonObject entryObject = getIntentData(sessionEntry);
            sessionRecords.add(entryObject);
            if (String.valueOf(sessionEntry.getValueByKey("is_Matched")).equals("false")) {
                unmatchedCount++;
            } else {
                accConfidence += Double.parseDouble(
                        String.valueOf(
                                sessionEntry.getValueByKey("confidence")
                        )
                );
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
     * Reads Record's Intent/utterance data and parses it as a JsonObject to be returned by the API.
     *
     * @param record
     * @return JsonObject with record's data
     */
    private JsonObject getIntentData(FluxRecord record) {
        String aux = String.valueOf(record.getValueByKey("_time"));
        long time = Instant.parse(aux).toEpochMilli();
        JsonObject obj = new JsonObject();
        // Adding "generic" data
        obj.addProperty("bot_id", String.valueOf(record.getValueByKey("bot_id")));
        obj.addProperty("origin", String.valueOf(record.getValueByKey("origin")));
        obj.addProperty("platform", String.valueOf(record.getValueByKey("platform")));
        // Not sure if this is related to the intents or not for the session, but I am putting it just before the
        // session data
        obj.addProperty("parameters", String.valueOf(record.getValueByKey("matched_params")));
        // Adding utterance/intent specific data 
        obj.addProperty("session_id", String.valueOf(record.getValueByKey("session_id")));
        obj.addProperty("timestamp", time);
        obj.addProperty("utterance", String.valueOf(record.getValueByKey("utterance")));
        obj.addProperty("is_matched", String.valueOf(record.getValueByKey("is_Matched")));
        obj.addProperty("intent", String.valueOf(record.getValueByKey("matched_intent")));
        obj.addProperty("confidence", Double.parseDouble(String.valueOf(record.getValueByKey("confidence"))));
        //confidence ha de ser un double al JSON!
        return obj;
    }


    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        this.db.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logRecognizedIntent(StateContext context, RecognizedIntent intent) {
        //Write intent data into db.
        try (WriteApi writer = db.getWriteApi()) {
            Point point = generateIntentPoint(context, intent);
            writer.writePoint(point);
        }
    }

    /**
     * Generates a point object to be written into the database, based on {@code recognizedIntent} and {@code session}.
     *
     * @param context the {@link StateContext} from which the {@link RecognizedIntent} has been created
     * @param intent  the {@link RecognizedIntent} to log
     * @return Point with data ready to be inserted into an influx db.
     */
    private Point generateIntentPoint(StateContext context, RecognizedIntent intent) {
        boolean isMatched = !intent.getDefinition().getName().equals(new DefaultFallbackIntent().getName());

        return Point.measurement("intent")
                .addTag("bot_id", bot_Id)
                .addTag("is_Matched", String.valueOf(isMatched))
                .addTag("session_id", context.getContextId())
                // Store an empty String if the origin is null (can't store null tags in InfluxDB)
                .addTag("origin", context.getOrigin() == null ? "" : context.getOrigin())
                .addTag("platform", intent.getTriggeredBy() == null ? "" : intent.getTriggeredBy())
                .addTag("username", "Not sure where to find the username ;)")
                .addField("confidence", intent.getRecognitionConfidence())
                .addField("utterance", intent.getMatchedInput())
                .addField("matched_intent", intent.getDefinition().getName())
                .addField("matched_params", "this is a placeholder for matched params")
                .time(Instant.now().toEpochMilli(), WritePrecision.MS); //maybe not the best format? idk
    }

    /**
     * Builds a query string based of the params passed.
     *
     * @param rfcStartTime rfc3339 format (similar to ISO-8601): YYYY-MM-DDThh:mm:ssZ Can be null/empty.
     * @param filters      Array of strings (remember to use backslashes!). Each position is a condition in string
     *                     format. i.e: r.is_Matched == \"true\"
     * @param pivot        If the query should include the "pivot" call.
     * @param group        If the query should group results in a single table
     * @return String - query string built to be used in influx client
     */
    private String queryBuilder(String rfcStartTime, String[] filters, boolean pivot, boolean group) {
        String query = "from(bucket: \"" + bucket + "\") ";
        if (isNull(rfcStartTime) || rfcStartTime.isEmpty()) {
            query = query.concat("|> range(start: 2018-05-22T23:30:00Z, stop: now()) ");
        } else { //Assuming the rfcStartTime is correct
            query = query.concat("|> range(start: " + rfcStartTime + ", stop: now()) ");
        }
        //adding filters 
        query = query.concat("|> filter(fn:(r) => r._measurement == \"intent\" and r.bot_id == \"" + bot_Id + "\"");
        for (String s : filters) {
            query = query.concat(" and " + s);
        }
        query = query.concat(")");
        if (pivot)
            query = query.concat("|> pivot(columnKey: [\"_field\"], rowKey: [\"_time\"], valueColumn: \"_value\") ");
        if (group) query = query.concat("|> group()");
        query = query.concat("|> sort(columns: [\"_time\"])"); // Order results by timestamp, older data first
        return query;
    }
}   
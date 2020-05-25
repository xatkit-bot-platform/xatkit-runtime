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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxColumn;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

import java.io.File;
import java.io.Serializable;

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
public class RecognitionMonitorInflux extends RecognitionMonitor{

    /**
    * The {@link Configuration} key to specify the auth token for the bot to be able to store/query data from an influx bucket.
    * <p>
    * This property is mandatory.
    */
    static final String INFLUX_TOKEN_KEY = "xatkit.influx.token";

    /**
     * The TOKEN value specified in the {@link Configuration}, necessary to make petitions to the database.
     */
    private static char[] TOKEN;

    /**
    * The {@link Configuration} key to specify a custom bucket instance to store the analytics.
    */
    static final String INFLUX_BUCKET_KEY = "xatkit.influx.bucket";

    /**
     * The BUCKET value specified in the {@link Configuration}, necessary for the petitions to the database.
     */
    private static String BUCKET;

    /**
    * The {@link Configuration} key to specify a custom organization workspace for influx.
    */
    static final String INFLUX_ORG_KEY = "xatkit.influx.organization";

    /**
     * The ORGANIZATION value specified in the {@link Configuration}, necessary for the petitions to the database.
     */
    private static String ORGANIZATION;

    /**
     * The databases url key.
     * <p>
     * This property is optional and will use "http://localhost:7777" as default
     */
    private static final String INFLUX_URL_KEY = "xatkit.influx.url";

    /**
     * The default value for the URL of influx instance
     */
    private static final String DEFAULT_URL = "http://localhost:7777";

    /**
     * The default ID of the bot
     */
    private static final String DEFAULT_BOT_ID = "xatkitBot";

    /**
     * The database persistent client to make the petitions :)
     */
     private InfluxDBClient db;

    /**
    *   Influxdb stores data, by default in "/var/lib/influxdb/wal" or "/var/lib/influxdb/data"
    *   based on it's configuration: wal files are "temporal" until they reach 25MB (default config)
    *   or when the data has been there for 10 minutes. This can be changed in the [data] section from influxdb.conf
    *   To be able to query/input data with influx we need the following information:
    *   TOKEN: this validates our bot into the database, so it will be authenticated.
    *   BUCKET: named location where data is stored. It has a retention policy, a duration of time that each data point persists, etc.
    *   A bucket belongs to 1 organization.
    *   ORGANIZATION:        it's the workspace/group of users. It can own multiple buckets.
    *   @param xatkitServer  the {@link XatkitServer} instance used to register the REST endpoints
    *   @param configuration the Xatkit {@link Configuration}
    */
    public RecognitionMonitorInflux(XatkitServer xatkitServer, Configuration configuration){
        Log.info("Starting new intent recognition monitoring with Influxdb");
        // Requirements for storing/querying data from influx:
        //  auth token that validates our bot into the database.
        //  organization name
        //  bucket name
        TOKEN = configuration.getString(INFLUX_TOKEN_KEY).toCharArray();
        BUCKET = configuration.getString(INFLUX_BUCKET_KEY);
        Log.info("Bucket: " + BUCKET);
        ORGANIZATION = configuration.getString(INFLUX_ORG_KEY); 
        Log.info("Organization: " + ORGANIZATION);
        String url = configuration.getString(INFLUX_URL_KEY, DEFAULT_URL);
        Log.info("Influxdb url: " + url);
        db = InfluxDBClientFactory.create(url, TOKEN, ORGANIZATION, BUCKET);

        registerServerEndpoints(xatkitServer);
    }

    /**
     * Registers the REST endpoints used to retrieve monitoring information.
     *
     * @param xatkitServer the {@link XatkitServer} instance used to register the REST endpoints
     */
    private void registerServerEndpoints(XatkitServer xatkitServer) {
        //this.registerGetMonitoringData(xatkitServer);
        //this.registerGetMonitoringDataForSession(xatkitServer);
        //this.registerGetUnmatchedUtterances(xatkitServer);
        this.registerGetMatchedUtterances(xatkitServer);
        //this.registerGetSessionsStats(xatkitServer);
    }

    /**
    * Registers the {@code GET: /analytics/monitoring/matched} endpoint.
    * <p>
    * This endpoint returns a JSON array containing all  the matched intents (i.e. inputs that have been
    * successfully translated into intents).
    * <p>
    * @param xatkitServer the {@link XatkitServer} instance used to register the REST endpoint
    */
    private void registerGetMatchedUtterances(XatkitServer xatkitServer){
        xatkitServer.registerRestEndpoint(HttpMethod.GET, "/analytics/monitoring/matched", 
            RestHandlerFactory.createJsonRestHandler((headers, params, content) -> {
                JsonArray res = new JsonArray();
                //Query database for matched intents and retrieve them.
                //Query data;
                String query = "from(bucket: \"" + BUCKET + "\") " +
                "|> range(start: -30d) " + //range could be changed for some constant I guess
                "|> filter(fn:(r) => r._measurement == \"intent\" and r.is_Matched == \"true\")";

                List<FluxTable> tables = db.getQueryApi().query(query);
                for(FluxTable table : tables){
                    List<FluxRecord> records = table.getRecords();
                    for(FluxRecord record : records){
                        //figuring out how to do this properly :))
                    }
                }
                return res;
            })
        );
    }

    /**
     * Closes connection to database. Changes should be commited, but check influxDB doc in case some actions need to be performed!
     */
    public void shutdown(){
        this.db.close();
    }

    /**
     * Logs the recognition information from the provided {@code recognizedIntent} and {@code session}.
     *
     * @param session          the {@link XatkitSession} from which the {@link RecognizedIntent} has been created
     * @param recognizedIntent the {@link RecognizedIntent} to log
     */
    public void logRecognizedIntent(XatkitSession session, RecognizedIntent recognizedIntent) {
        //Write intent data into db.
        try (WriteApi writer = db.getWriteApi()){
            Point point = generateIntentPoint(session, recognizedIntent);
            //Log.info("Point created! lets try to write :)");
            writer.writePoint(point);
            //Log.info("allegedly performed a write :))");
        }
    }

    /**
     * Generates a point object to be written into the database, based on {@code recognizedIntent} and {@code session}.
     * 
     * @param session          the {@link XatkitSession} from which the {@link RecognizedIntent} has been created
     * @param recognizedIntent the {@link RecognizedIntent} to log
     */
    private Point generateIntentPoint(XatkitSession session, RecognizedIntent recognizedIntent){
        boolean isMatched = !recognizedIntent.getDefinition().getName().equals(new DefaultFallbackIntent().getName());
        Log.info("is Matched = " + isMatched);
        Log.info("Platform trigger: " + recognizedIntent.getTriggeredBy());
        return          Point.measurement("intent")
                            .addTag("bot_id",                   DEFAULT_BOT_ID)
                            .addTag("is_Matched",               String.valueOf(isMatched))
                            .addTag("session_id",               session.getSessionId())
                            .addTag("origin",                   "this is a placeholder for origin :)")
                            .addTag("platform",                 "recognizedIntent.getTriggeredBy()") //getTriggeredBy() is returning always null to me, causing the write into influx to fail :S (it's easy to catch but maybe we should know why this happens)
                            .addField("confidence",             recognizedIntent.getRecognitionConfidence())
                            .addField("utterance",              recognizedIntent.getMatchedInput())
                            .addField("matched_intent",         recognizedIntent.getDefinition().getName())
                            .addField("matched_params",         "this is a placeholder for matched params")
                            .time(Instant.now().toEpochMilli(), WritePrecision.MS); //maybe not the best format? idk
                            //.time(new Timestamp(System.currentTimeMillis()));
    }
}   
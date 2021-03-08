package com.xatkit.core.recognition.processor.toxicity.detoxify;

import com.mashape.unirest.http.Unirest;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;
import org.json.JSONObject;

/**
 * Client for a Detoxify server.
 * <p>
 * The URL of the Detoxify server must be specified in the provided configuration. See
 * {@link DetoxifyConfiguration#DETOXIFY_SERVER_URL} for more information.
 */
public class DetoxifyClient {

    /**
     * The configuration of the client.
     */
    private DetoxifyConfiguration configuration;

    /**
     * Initializes the client with the provided {@code baseConfiguration}.
     * <p>
     * The provided {@code baseConfiguration} must define the URL of the Detoxify server
     * (see {@link DetoxifyConfiguration#DETOXIFY_SERVER_URL} for more information).
     *
     * @param baseConfiguration the {@link Configuration} used to initialize the client
     * @throws NullPointerException if the provided {@code baseConfiguration} is {@code null} or if it does not
     *                              contain the Detoxify server URL
     */
    public DetoxifyClient(@NonNull Configuration baseConfiguration) {
        this.configuration = new DetoxifyConfiguration(baseConfiguration);
    }

    /**
     * Computes the toxicity scores for the provided {@code input}.
     *
     * @param input the text to analyze
     * @return the computed scores, or {@link DetoxifyScore#UNSET_SCORE} if an error occurred while computing the scores
     */
    public DetoxifyScore analyzeRequest(String input) {
        JSONObject request = new JSONObject();
        request.put("input", input);
        try {

            JSONObject response = Unirest.post(this.configuration.getDetoxifyServerUrl() + "/analyzeRequest")
                    .header("Content-Type", "application/json")
                    .body(request)
                    .asJson().getBody().getObject();
            return new DetoxifyScore(response);
        } catch (Exception e) {
            Log.error(e, "An error occurred while computing the toxicity scores, see the attached exception for more "
                    + "information");
        }
        return DetoxifyScore.UNSET_SCORE;
    }
}

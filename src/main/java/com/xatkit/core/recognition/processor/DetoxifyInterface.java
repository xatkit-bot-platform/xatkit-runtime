package com.xatkit.core.recognition.processor;

import com.mashape.unirest.http.Unirest;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * The type Detoxify interface.
 */
public class DetoxifyInterface {

    private static double UNSET_SCORE = -1.;

    private DetoxifyInterfaceConfiguration configuration;

    public DetoxifyInterface(Configuration baseConfiguration) {
        this.configuration = new DetoxifyInterfaceConfiguration(baseConfiguration);
    }

    /**
     * Analyzes the comment and returns the punctuation for each attribute
     *
     * @param comment the comment
     * @return the hash map with attributes and their respective scores
     */
    public HashMap<String, Double> analyzeRequest(String comment) {
        JSONObject response = new JSONObject();
        HashMap<String, Double> scores = new HashMap<>();
        JSONObject request = new JSONObject();
        try {
            for (ToxicityLabel k : ToxicityLabel.values()) {
                scores.put(k.toString(), UNSET_SCORE);
            }
            request.put("comment", comment);
            response = Unirest.post(this.configuration.getDetoxifyServerUrl() + "/analyzeRequest")
                    .header("Content-Type", "application/json")
                    .body(request)
                    .asJson().getBody().getObject();
            response.keySet();
            for (String k : response.keySet()) {
                Double score = response.getDouble(k);
                scores.put(k, score);
            }
        } catch (Exception e) {
            // TODO look at the logger issues with JSON
            Log.warn("Detoxify failed to compute toxicity scores");
            System.out.println("DetoxifyResponse=" + response.toString());
            e.printStackTrace();
        }
        return scores;
    }
}

package fr.zelus.jarvis.dialogflow.stream;

import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.StreamingDetectIntentResponse;
import fr.inria.atlanmod.commons.log.Log;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

public class DialogFlowResponseObserver implements ApiStreamObserver<StreamingDetectIntentResponse> {

    private List<StreamingDetectIntentResponse> responses;

    private List<Throwable> responseThrowables;

    private boolean hasResponse;

    private QueryResult lastQueryResult;

    public DialogFlowResponseObserver() {
        this.responses = new ArrayList<>();
        this.responseThrowables = new ArrayList<>();
        this.hasResponse = false;
    }

    @Override
    public void onNext(StreamingDetectIntentResponse response) {
        Log.info("Received DialogFlow response");
        responses.add(response);
        if(response.hasRecognitionResult()) {
            Log.info("Intermediate Transcript: {0}", response.getRecognitionResult().getTranscript());
        }
        if(response.hasQueryResult()) {
            Log.info("Has query result");
            if(nonNull(response.getQueryResult().getIntent())) {
                Log.info("Found Intent: {0}, (confidence = {1})", response.getQueryResult().getIntent()
                        .getDisplayName(), response.getQueryResult().getIntentDetectionConfidence());
                hasResponse = true;
                lastQueryResult = response.getQueryResult();
                onCompleted();
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        Log.error(throwable);
        responseThrowables.add(throwable);
    }

    @Override
    public void onCompleted() {

    }

    public boolean hasResponse() {
        return hasResponse;
    }

    public QueryResult getLastQueryResult() {
        return lastQueryResult;
    }
}

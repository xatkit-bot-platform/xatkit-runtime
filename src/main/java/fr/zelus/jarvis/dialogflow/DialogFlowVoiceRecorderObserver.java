package fr.zelus.jarvis.dialogflow;

import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.cloud.dialogflow.v2.*;
import com.google.protobuf.ByteString;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.io.VoiceRecorder;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CountDownLatch;

public class DialogFlowVoiceRecorderObserver implements Observer {

    private ApiStreamObserver<StreamingDetectIntentRequest> requestObserver;
    private DialogFlowResponseObserver responseObserver;
    private CountDownLatch notification;

    public DialogFlowVoiceRecorderObserver(SessionsClient sessionsClient, SessionName session) {
        this.responseObserver = new DialogFlowResponseObserver();
        this.requestObserver = sessionsClient.streamingDetectIntentCallable()
                .bidiStreamingCall(responseObserver);
        this.notification = new CountDownLatch(1);

        AudioEncoding audioEncoding = AudioEncoding.AUDIO_ENCODING_LINEAR_16;
        int sampleRateHertz = 44100;

        // Instructs the speech recognizer how to process the audio content.
        InputAudioConfig inputAudioConfig = InputAudioConfig.newBuilder()
                .setAudioEncoding(audioEncoding) // audioEncoding = AudioEncoding.AUDIO_ENCODING_LINEAR_16
                .setLanguageCode("en-US") // languageCode = "en-US"
                .setSampleRateHertz(sampleRateHertz) // sampleRateHertz = 16000
                .build();

        QueryInput queryInput = QueryInput.newBuilder().setAudioConfig(inputAudioConfig).build();
        StreamingDetectIntentRequest request = StreamingDetectIntentRequest.newBuilder()
                .setSession(session.toString())
                .setQueryInput(queryInput)
                .setSingleUtterance(true) // == recognition stops after a break
                .build();

        // Make the first request
        requestObserver.onNext(request);

    }

    public QueryResult getQueryResult() {
        try {
            notification.await();
        } catch(InterruptedException e) {
            Log.error(e);
        }
        return responseObserver.getLastQueryResult();
    }

    @Override
    public void update(Observable o, Object arg) {
        if(o instanceof VoiceRecorder) {
            byte[] buffer = (byte[])arg;
            if(responseObserver.hasResponse()) {
                requestObserver.onCompleted();
                notification.countDown();
            } else {
                requestObserver.onNext(StreamingDetectIntentRequest.newBuilder()
                        .setInputAudio(ByteString.copyFrom(buffer, 0, buffer.length))
                        .build());
            }
        } else {
            throw new JarvisException("DialogFlowVoiceRecorderObserver should only observe VoiceRecorder instances");
        }
    }
}

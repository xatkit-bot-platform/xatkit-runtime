import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.cloud.dialogflow.v2.*;
import com.google.protobuf.ByteString;

import javax.sound.sampled.*;
import java.io.FileInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class App {

    public static void main(String[] args) throws Throwable {
//        record();
        //detectIntentTexts("room-reservation-ee77e", Arrays.asList("Greetings"), "sessionID", "en");
        detectIntentStream("room-reservation-ee77e", "/Users/gwend/Desktop/hello.wav", "sessionID", "en-US");
    }

    public static void record() {
        AudioFormat format = new AudioFormat(44100, 16, 1, true, true);

        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format);
        DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, format);

        try {
            TargetDataLine targetLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
            targetLine.open(format);
            targetLine.start();

            SourceDataLine sourceLine = (SourceDataLine) AudioSystem.getLine(sourceInfo);
            sourceLine.open(format);
            sourceLine.start();

            int numBytesRead;
            byte[] targetData = new byte[targetLine.getBufferSize() / 5];

            while (true) {
                numBytesRead = targetLine.read(targetData, 0, targetData.length);

                if (numBytesRead == -1)	break;

                sourceLine.write(targetData, 0, numBytesRead);
            }
        }
        catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
     * Returns the result of detect intent with texts as inputs.
     * <p>
     * Using the same `session_id` between requests allows continuation of the conversation.
     *
     * @param projectId    Project/Agent Id.
     * @param texts        The text intents to be detected based on what a user says.
     * @param sessionId    Identifier of the DetectIntent session.
     * @param languageCode Language code of the query.
     */
    public static void detectIntentTexts(String projectId, List<String> texts, String sessionId,
                                         String languageCode) throws Exception {
        // Instantiates a client
        try (SessionsClient sessionsClient = SessionsClient.create()) {
            // Set the session name using the sessionId (UUID) and projectID (my-project-id)
            SessionName session = SessionName.of(projectId, sessionId);
            System.out.println("Session Path: " + session.toString());

            // Detect intents for each text input
            for (String text : texts) {
                // Set the text (hello) and language code (en-US) for the query
                TextInput.Builder textInput = TextInput.newBuilder().setText(text).setLanguageCode(languageCode);

                // Build the query with the TextInput
                QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();

                // Performs the detect intent request
                DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);

                // Display the query result
                QueryResult queryResult = response.getQueryResult();

                System.out.println("====================");
                System.out.format("Query Text: '%s'\n", queryResult.getQueryText());
                System.out.format("Detected Intent: %s (confidence: %f)\n",
                        queryResult.getIntent().getDisplayName(), queryResult.getIntentDetectionConfidence());
                System.out.format("Fulfillment Text: '%s'\n", queryResult.getFulfillmentText());
            }
        }
    }

    /**
     * Returns the result of detect intent with streaming audio as input.
     *
     * Using the same `session_id` between requests allows continuation of the conversation.
     * @param projectId Project/Agent Id.
     * @param audioFilePath The audio file to be processed.
     * @param sessionId Identifier of the DetectIntent session.
     * @param languageCode Language code of the query.
     */
    public static void detectIntentStream(String projectId, String audioFilePath, String sessionId,
                                          String languageCode) throws Throwable {
        // Start bi-directional StreamingDetectIntent stream.
        final CountDownLatch notification = new CountDownLatch(1);
        final List<Throwable> responseThrowables = new ArrayList<>();
        final List<StreamingDetectIntentResponse> responses = new ArrayList<>();

        // Instantiates a client
        try (SessionsClient sessionsClient = SessionsClient.create()) {
            // Set the session name using the sessionId (UUID) and projectID (my-project-id)
            SessionName session = SessionName.of(projectId, sessionId);
            System.out.println("Session Path: " + session.toString());

            // Note: hard coding audioEncoding and sampleRateHertz for simplicity.
            // Audio encoding of the audio content sent in the query request.
            AudioEncoding audioEncoding = AudioEncoding.AUDIO_ENCODING_LINEAR_16;
            int sampleRateHertz = 44100;

            // Instructs the speech recognizer how to process the audio content.
            InputAudioConfig inputAudioConfig = InputAudioConfig.newBuilder()
                    .setAudioEncoding(audioEncoding) // audioEncoding = AudioEncoding.AUDIO_ENCODING_LINEAR_16
                    .setLanguageCode(languageCode) // languageCode = "en-US"
                    .setSampleRateHertz(sampleRateHertz) // sampleRateHertz = 16000
                    .build();

            ApiStreamObserver<StreamingDetectIntentResponse> responseObserver =
                    new ApiStreamObserver<StreamingDetectIntentResponse>() {
                        @Override
                        public void onNext(StreamingDetectIntentResponse response) {
                            // Do something when receive a response
                            System.out.println("next: " + response);
                            responses.add(response);
                        }

                        @Override
                        public void onError(Throwable t) {
                            // Add error-handling
                            System.out.println("Error: " + t);
                            responseThrowables.add(t);
                        }

                        @Override
                        public void onCompleted() {
                            // Do something when complete.
                            notification.countDown();
                        }
                    };

            // Performs the streaming detect intent callable request
            ApiStreamObserver<StreamingDetectIntentRequest> requestObserver =
                    sessionsClient.streamingDetectIntentCallable().bidiStreamingCall(responseObserver);

            // Build the query with the InputAudioConfig
            QueryInput queryInput = QueryInput.newBuilder().setAudioConfig(inputAudioConfig).build();

            try (FileInputStream audioStream = new FileInputStream(audioFilePath)) {
                // The first request contains the configuration
                StreamingDetectIntentRequest request = StreamingDetectIntentRequest.newBuilder()
                        .setSession(session.toString())
                        .setQueryInput(queryInput)
                        //.setSingleUtterance(true) // == recognition stops after a break
                        .build();

                // Make the first request
                requestObserver.onNext(request);

                // Following messages: audio chunks. We just read the file in fixed-size chunks. In reality
                // you would split the user input by time.
                byte[] buffer = new byte[10000];
//                int bytes;

                AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
                System.out.println(format.getEncoding());

                DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format);
                DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, format);

                try {
                    TargetDataLine targetLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
                    targetLine.open(format);
                    targetLine.start();

                    SourceDataLine sourceLine = (SourceDataLine) AudioSystem.getLine(sourceInfo);
                    sourceLine.open(format);
                    sourceLine.start();

                    int numBytesRead;
//                    byte[] targetData = new byte[targetLine.getBufferSize() / 5];

                    Instant begin = Instant.now();
                    while (true) {
                        numBytesRead = targetLine.read(buffer, 0, buffer.length);

                        if (numBytesRead == -1)	break;

                        requestObserver.onNext(
                                StreamingDetectIntentRequest.newBuilder()
                                        .setInputAudio(ByteString.copyFrom(buffer, 0, numBytesRead))
                                        .build());
                        Instant after = Instant.now();
                        if(after.minusSeconds(10).isAfter(begin)) {
                            // More than 10 sec
                            System.out.println("after");
//                            requestObserver.onCompleted();
                            break;
                        }

//                        sourceLine.write(buffer, 0, numBytesRead);
                    }
                }
                catch (Exception e) {
                    System.err.println(e);
                }


//                while ((bytes = audioStream.read(buffer)) != -1) {
//                    requestObserver.onNext(
//                            StreamingDetectIntentRequest.newBuilder()
//                                    .setInputAudio(ByteString.copyFrom(buffer, 0, bytes))
//                                    .build());
//                }
                System.out.println("ggg");
            } catch (RuntimeException e) {
                // Cancel stream.
                requestObserver.onError(e);
            }
            System.out.println("mais ou est on?");
            // Half-close the stream.
            requestObserver.onCompleted();
            System.out.println("on est pas là ?");
            // Wait for the final response (without explicit timeout).
            notification.await();
            System.out.println("par contre ici mes couilles");
            // Process errors/responses.
            if (!responseThrowables.isEmpty()) {
                throw responseThrowables.get(0);
            }
            if (responses.isEmpty()) {
                throw new RuntimeException("No response from Dialogflow.");
            }
            System.out.println("gn");

            for (StreamingDetectIntentResponse response : responses) {
                if (response.hasRecognitionResult()) {
                    System.out.format(
                            "Intermediate transcript: '%s'\n", response.getRecognitionResult().getTranscript());
                }
            }

            // Display the last query result
            QueryResult queryResult = responses.get(responses.size() - 1).getQueryResult();
            System.out.println("====================");
            System.out.format("Query Text: '%s'\n", queryResult.getQueryText());
            System.out.format("Detected Intent: %s (confidence: %f)\n",
                    queryResult.getIntent().getDisplayName(), queryResult.getIntentDetectionConfidence());
            System.out.format("Fulfillment Text: '%s'\n", queryResult.getFulfillmentText());
        }
    }
}
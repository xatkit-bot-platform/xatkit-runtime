package fr.zelus.jarvis.dialogflow.stream;

import com.google.cloud.dialogflow.v2.Intent;
import com.google.cloud.dialogflow.v2.SessionName;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.dialogflow.DialogFlowApi;
import fr.zelus.jarvis.dialogflow.DialogFlowApiTest;
import fr.zelus.jarvis.io.VoiceRecorder;
import org.junit.Ignore;
import org.junit.Test;

public class DialogFlowApiFromStreamTest extends DialogFlowApiTest {

    @Ignore
    @Test
    public void getIntentFromVoiceRecorder() {
        api = new DialogFlowApi(VALID_PROJECT_ID);
        SessionName session = api.createSession();
        Intent intent = api.getIntentFromAudio(new VoiceRecorder(), session);
        Log.info("Found intent {0}", intent.getDisplayName());
    }
}

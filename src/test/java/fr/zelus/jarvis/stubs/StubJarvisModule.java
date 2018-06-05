package fr.zelus.jarvis.stubs;

import com.google.cloud.dialogflow.v2.Intent;
import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.JarvisModule;

public class StubJarvisModule implements JarvisModule {

    @Override
    public boolean acceptIntent(Intent intent) {
        return false;
    }

    @Override
    public JarvisAction handleIntent(Intent intent) {
        return null;
    }
}

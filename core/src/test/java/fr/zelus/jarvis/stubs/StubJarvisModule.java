package fr.zelus.jarvis.stubs;

import com.google.cloud.dialogflow.v2.Intent;
import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.JarvisModule;
import fr.zelus.jarvis.intent.RecognizedIntent;
import fr.zelus.jarvis.module.Action;
import fr.zelus.jarvis.stubs.action.StubJarvisAction;

/**
 * A stub {@link JarvisModule} used to check whether {@link Intent}s are handled and {@link JarvisAction} performed.
 * <p>
 * This class provides two accessors that allow to check if (i) the module has handled an {@link Intent}, and (ii) if
 * the created {@link JarvisAction} has been processed.
 *
 * @see #isIntentHandled()
 * @see #isActionProcessed()
 */
public class StubJarvisModule extends JarvisModule {

    private StubJarvisAction jarvisAction;

    public StubJarvisModule() {
        super();
        this.jarvisAction = new StubJarvisAction();
    }

    public StubJarvisAction getAction() {
        return jarvisAction;
    }

    @Override
    public JarvisAction createJarvisAction(Action action, RecognizedIntent intent) {
        return jarvisAction;
    }

}

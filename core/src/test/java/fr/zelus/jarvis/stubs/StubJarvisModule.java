package fr.zelus.jarvis.stubs;

import com.google.cloud.dialogflow.v2.Intent;
import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.JarvisModule;

import java.util.Collections;
import java.util.List;

/**
 * A stub {@link JarvisModule} used to check whether {@link Intent}s are handled and {@link JarvisAction} performed.
 * <p>
 * This class provides two accessors that allow to check if (i) the module has handled an {@link Intent}, and (ii) if
 * the created {@link JarvisAction} has been processed.
 *
 * @see #isIntentHandled()
 * @see #isActionProcessed()
 */
public class StubJarvisModule implements JarvisModule {

    /**
     * A flag representing whether an {@link Intent} has been handled by this module.
     */
    private boolean intentHandled = false;

    /**
     * A flag representing whether a {@link JarvisAction} created by this module has been processed.
     */
    private boolean actionProcessed = false;

    /**
     * Returns whether an {@link Intent} has been handled by this module.
     *
     * @return whether an {@link Intent} has been handled by this module
     */
    public boolean isIntentHandled() {
        return intentHandled;
    }

    /**
     * Returns whether a {@link JarvisAction} created by this module has been processed.
     *
     * @return whether a {@link JarvisAction} created by this module has been processed
     */
    public boolean isActionProcessed() {
        return actionProcessed;
    }

    @Override
    public String getName() {
        return "StubModule";
    }

    @Override
    public boolean acceptIntent(Intent intent) {
        return intent.getDisplayName().equals("Default Welcome Intent");
    }

    @Override
    public JarvisAction handleIntent(Intent intent) {
        intentHandled = true;
        return new JarvisAction("StubAction") {
            @Override
            public void run() {
                actionProcessed = true;
            }
        };
    }

    @Override
    public List<Class<JarvisAction>> getRegisteredActions() {
        return Collections.emptyList();
    }

    @Override
    public Class<JarvisAction> getActionWithName(String name) {
        return null;
    }
}

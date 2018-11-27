package edu.uoc.som.jarvis.stubs.action;

import edu.uoc.som.jarvis.core.platform.RuntimePlatform;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.platform.action.RuntimeMessageAction;
import fr.zelus.jarvis.core.platform.RuntimePlatform;
import fr.zelus.jarvis.core.session.JarvisSession;

import java.util.UUID;

public class StubRuntimeMessageAction extends RuntimeMessageAction {

    public static String RESULT = "result";

    protected int attempts;

    private JarvisSession clientSession = new JarvisSession(UUID.randomUUID().toString());

    public StubRuntimeMessageAction(RuntimePlatform runtimePlatform, JarvisSession session, String rawMessage) {
        super(runtimePlatform, session, rawMessage);
        attempts = 0;
    }

    @Override
    protected Object compute() throws Exception {
        Log.info("Computing {0}, message stub: {1}", this.getClass().getSimpleName(), this.message);
        attempts++;
        return RESULT;
    }

    @Override
    protected JarvisSession getClientSession() {
        return clientSession;
    }

    public int getAttempts() {
        return attempts;
    }
}

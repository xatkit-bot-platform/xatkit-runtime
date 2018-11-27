package fr.zelus.jarvis.plugins.core.platform.action;

import fr.zelus.jarvis.core.platform.action.RuntimeAction;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.plugins.core.platform.CorePlatform;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A {@link RuntimeAction} that retrieves the current time and returns it.
 * <p>
 * This class relies on {@link SimpleDateFormat} to format the retrieved time with the following template:
 * <i>HH:mm:ss</i>.
 *
 * @see GetDate
 */
public class GetTime extends RuntimeAction<CorePlatform> {

    /**
     * Constructs a new {@link GetTime} action from the provided {@code runtimePlatform} and {@code session}.
     *
     * @param runtimePlatform the {@link CorePlatform} containing this action
     * @param session          the {@link JarvisSession} associated to this action
     * @throws NullPointerException if the provided {@code runtimePlatform} or {@code session} is {@code null}
     */
    public GetTime(CorePlatform runtimePlatform, JarvisSession session) {
        super(runtimePlatform, session);
    }

    /**
     * Retrieves the current time and formats it.
     *
     * @return the formatted time
     */
    @Override
    public Object compute() {
        return new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
    }
}

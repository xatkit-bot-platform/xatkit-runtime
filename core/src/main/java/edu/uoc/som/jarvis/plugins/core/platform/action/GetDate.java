package edu.uoc.som.jarvis.plugins.core.platform.action;

import edu.uoc.som.jarvis.core.platform.action.RuntimeAction;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.plugins.core.platform.CorePlatform;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A {@link RuntimeAction} that retrieves the current date and returns it.
 * <p>
 * This class relies on {@link SimpleDateFormat} to format the retrieved date with the following template:
 * <i>dd/MM/yyyy</i>.
 *
 * @see GetTime
 */
public class GetDate extends RuntimeAction<CorePlatform> {

    /**
     * Constructs a new {@link GetDate} action from the provided {@code runtimePlatform} and {@code session}.
     *
     * @param runtimePlatform the {@link CorePlatform} containing this action
     * @param session          the {@link JarvisSession} associated to this action
     * @throws NullPointerException if the provided {@code runtimePlatform} or {@code session} is {@code null}
     */
    public GetDate(CorePlatform runtimePlatform, JarvisSession session) {
        super(runtimePlatform, session);
    }

    /**
     * Retrieves the current date and formats it.
     *
     * @return the formatted date.
     */
    @Override
    public Object compute() {
        return new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime());
    }
}

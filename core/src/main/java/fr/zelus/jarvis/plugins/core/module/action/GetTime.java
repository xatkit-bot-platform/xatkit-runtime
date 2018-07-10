package fr.zelus.jarvis.plugins.core.module.action;

import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.plugins.core.module.CoreModule;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A {@link JarvisAction} that retrieves the current time and returns it.
 * <p>
 * This class relies on {@link SimpleDateFormat} to format the retrieved time with the following template:
 * <i>HH:mm:ss</i>.
 *
 * @see GetDate
 */
public class GetTime extends JarvisAction<CoreModule> {

    /**
     * Constructs a new {@link GetTime} action from the provided {@code containingModule} and {@code session}.
     *
     * @param containingModule the {@link CoreModule} containing this action
     * @param session          the {@link JarvisSession} associated to this action
     * @throws NullPointerException if the provided {@code containingModule} or {@code session} is {@code null}
     */
    public GetTime(CoreModule containingModule, JarvisSession session) {
        super(containingModule, session);
    }

    /**
     * Retrieves the current time and formats it.
     *
     * @return the formatted time
     */
    @Override
    public Object call() {
        return new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
    }
}

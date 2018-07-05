package fr.zelus.jarvis.plugins.core.module.action;

import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.plugins.core.module.CoreModule;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A {@link JarvisAction} that retrieves the current date and returns it.
 * <p>
 * This class relies on {@link SimpleDateFormat} to format the retrieved date with the following template:
 * <i>dd/MM/yyyy</i>.
 *
 * @see GetTime
 */
public class GetDate extends JarvisAction<CoreModule> {

    /**
     * Constructs a new {@link GetDate} action from the provided {@code containingModule} and {@code context}.
     *
     * @param containingModule the {@link CoreModule} containing this action
     * @param context          the {@link JarvisContext} associated to this action
     * @throws NullPointerException if the provided {@code containingModule} or {@code context} is {@code null}
     */
    public GetDate(CoreModule containingModule, JarvisContext context) {
        super(containingModule, context);
    }

    /**
     * Retrieves the current date and formats it.
     *
     * @return the formatted date.
     */
    @Override
    public Object call() {
        return new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime());
    }
}

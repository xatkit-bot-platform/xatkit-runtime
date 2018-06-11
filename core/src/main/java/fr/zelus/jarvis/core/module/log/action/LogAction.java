package fr.zelus.jarvis.core.module.log.action;

import fr.inria.atlanmod.commons.log.Level;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisAction;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

public abstract class LogAction extends JarvisAction {

    protected String message;

    protected Level logLevel;

    public LogAction(String message, Level logLevel) {
        checkNotNull(message, "Cannot construct a {0} action with null as its message", this.getClass().getSimpleName
                ());
        this.message = message;
        this.logLevel = logLevel;
    }

    public final String getMessage() {
        return this.message;
    }

    @Override
    public void run() {
        Log.log(this.logLevel, this.message);
    }
}

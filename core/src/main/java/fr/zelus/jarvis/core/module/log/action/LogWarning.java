package fr.zelus.jarvis.core.module.log.action;

import fr.inria.atlanmod.commons.log.Level;

public class LogWarning extends LogAction {

    public LogWarning(String message) {
        super(message, Level.WARN);
    }
}

package fr.zelus.jarvis.core;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.utils.MessageUtils;

import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

public abstract class JarvisMessageAction<T extends JarvisModule> extends JarvisAction<T> {

    protected String message;

    public JarvisMessageAction(T containingModule, JarvisContext context, String message) {
        super(containingModule, context);
        checkArgument(nonNull(message) && !message.isEmpty(), "Cannot construct a {0} action with the provided " +
                "message {1}, expected a non-null and not empty String", this.getClass().getSimpleName(), message);
        this.message = MessageUtils.fillContextValues(message, context);
    }
}

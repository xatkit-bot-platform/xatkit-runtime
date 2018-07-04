package fr.zelus.jarvis.core;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.session.JarvisContext;

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
        this.message = fillContextValues(message);
    }

    protected String fillContextValues(String message) {
        Log.info("Processing message {0}", message);
        String outMessage = message;
        Matcher m = Pattern.compile("\\{\\$\\S+\\}").matcher(message);
        while(m.find()) {
            String group = m.group();
            Log.info("Found context variable {0}", group);
            /*
             * Cannot be empty.
             */
            String filteredGroup = group.substring(2);
            String[] splitGroup = filteredGroup.split("\\.");
            if(splitGroup.length == 2) {
                Log.info("Looking for context \"{0}\"", splitGroup[0]);
                Map<String, Object> variables = this.context.getContextVariables(splitGroup[0]);
                if(nonNull(variables)) {
                    String variableIdentifier = splitGroup[1].substring(0, splitGroup[1].length() -1);
                    Object value = variables.get(variableIdentifier);
                    Log.info("Looking for variable \"{0}\"", variableIdentifier);
                    if(nonNull(value)) {
                        String printedValue = null;
                        if(value instanceof Future) {
                            try {
                                printedValue = ((Future) value).get().toString();
                            } catch(InterruptedException | ExecutionException e) {
                                String errorMessage = MessageFormat.format("An error occured when retrieving the " +
                                        "value of the variable {0}", variableIdentifier);
                                Log.error(errorMessage);
                                throw new JarvisException(e);
                            }
                        }
                        else {
                            printedValue = value.toString();
                        }
                        outMessage = outMessage.replace(group, printedValue);
                    } else {
                        Log.error("The context variable {0} is null", group);
                    }
                } else {
                    Log.error("The context variable {0} does not exist", group);
                }
            } else {
                Log.error("Invalid context variable access: {0}", group);
            }
        }
        return outMessage;
    }
}

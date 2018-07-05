package fr.zelus.jarvis.utils;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.core.session.JarvisContext;

import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.nonNull;

public class MessageUtils {

    public static String fillContextValues(String message, JarvisContext context) {
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
                Map<String, Object> variables = context.getContextVariables(splitGroup[0]);
                if(nonNull(variables)) {
                    String variableIdentifier = splitGroup[1].substring(0, splitGroup[1].length() -1);
                    Object value = variables.get(variableIdentifier);
                    Log.info("Looking for variable \"{0}\"", variableIdentifier);
                    if(nonNull(value)) {
                        String printedValue = null;
                        if(value instanceof Future) {
                            try {
                                printedValue = ((Future) value).get().toString();
                                Log.info("found value {0} for {1}.{2}", printedValue, splitGroup[0],
                                        variableIdentifier);
                            } catch(InterruptedException | ExecutionException e) {
                                String errorMessage = MessageFormat.format("An error occured when retrieving the " +
                                        "value of the variable {0}", variableIdentifier);
                                Log.error(errorMessage);
                                throw new JarvisException(e);
                            }
                        }
                        else {
                            printedValue = value.toString();
                            Log.info("found value {0} for {1}.{2}", printedValue, splitGroup[0],
                                    variableIdentifier);
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

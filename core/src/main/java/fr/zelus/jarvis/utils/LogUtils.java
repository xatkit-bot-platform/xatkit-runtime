package fr.zelus.jarvis.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * An utility class that provides formatting facilities for logging.
 */
public class LogUtils {

    /**
     * Formats the provided {@code array} in a {@link String} used to log parameter values.
     * <p>
     * The returned {@link String} is "a1.toString(), a2.toString(), an.toString()", where <i>a1</i>,
     * <i>a2</i>, and <i>an</i> are elements in the provided {@code array}.
     *
     * @param array the array containing the parameter to print
     * @return a {@link String} containing the formatted parameters
     */
    public static String prettyPrint(Object[] array) {
        List<String> toStringList = StreamSupport.stream(Arrays.asList(array).spliterator(), false).map(o ->
        {
            if(o instanceof String) {
                return "\"" + o.toString() + "\"";
            } else {
                return o.toString();
            }
        }).collect(Collectors.toList());
        return String.join(",", toStringList);
    }
}

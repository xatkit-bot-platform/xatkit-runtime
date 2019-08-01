package com.xatkit.core.platform;

import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.lang3.ClassUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.nonNull;

/**
 * Provides mapping function from execution-level {@link Object}s to their {@link String} representation.
 * <p>
 * This class contains a set of {@code format functions} that can be registered using
 * {@link #registerFormatFunction(Class, Function)}. A {@code format function} is defined by a {@link Class} (i.e.
 * the class to format), and a {@link Function} that takes an instance of it as input and returns a {@link String}.
 * <p>
 * {@link Object}s can be formatted using {@link #format(Object)}, that retrieves the {@code format function}
 * associated to the {@link Object} and applies it.
 * <p>
 * Xatkit embeds a <i>Default</i> {@link Formatter} that maps {@link Object}s to their {@code toString()} method.
 * Custom platforms can register new {@code format functions} to the default formatter, or define a new formatter
 * using {@link com.xatkit.core.XatkitCore#registerFormatter(String, Formatter)}.
 */
public final class Formatter {

    /**
     * The format {@link Function}s.
     * <p>
     * The keys of this {@link Map} correspond to the {@link Class}es that can be formatted by the formatter, and the
     * values are {@link Function}s accepting instances of their corresponding {@link Class} and returning
     * {@link String}s.
     * <p>
     * This {@link Formatter} embeds a generic {@link Function} associated to {@link Object} that returns its {@code
     * toString()} method.
     */
    private Map<Class, Function> formatFunctions;

    /**
     * Constructs the {@link Formatter} and initializes its {@code format functions}.
     * <p>
     * This {@link Formatter} embeds a generic {@link Function} associated to {@link Object} that returns its {@code
     * toString()} method.
     */
    public Formatter() {
        this.formatFunctions = new HashMap<>();
        /*
         * Registers the functions for Object, it will be matched for any object that does not have a better
         * formatting function.
         */
        this.registerFormatFunction(Object.class, Object::toString);
    }

    /**
     * Registers the provided {@code function} to the given {@code clazz}.
     * <p>
     * If the {@link Formatter} already contains a {@code format function} for the provided {@code clazz} it is
     * erased and a warning is printed.
     *
     * @param clazz    the {@link Class} associated to the {@link Function}
     * @param function the {@link Function} to register
     */
    public <T> void registerFormatFunction(Class<T> clazz, Function<T, String> function) {
        if (this.formatFunctions.containsKey(clazz)) {
            Log.warn("A format function is already registered for class {0}, erasing it", clazz.getSimpleName());
        }
        this.formatFunctions.put(clazz, function);
    }

    /**
     * Formats the provided {@code obj}.
     * <p>
     * This method searches for a registered {@code format function} associated to {@code obj}'s {@link Class}. If
     * their is no function for this {@link Class} the method will look for functions associated to its super-classes
     * and interfaces, recursively. The function associated to the {@link Class} with the closest distance to the
     * {@code obj}'s one is returned, in order to support polymorphic formatting.
     * <p>
     * <b>Note:</b> if there is no function associated to the {@code obj}'s {@link Class} hierarchy this method will
     * return the result of applying the default {@code Object format function}.
     *
     * @param obj the {@link Object} to format
     */
    public String format(Object obj) {
        Iterable<Class<?>> hierarchy = ClassUtils.hierarchy(obj.getClass(), ClassUtils.Interfaces.INCLUDE);
        for (Class<?> clazz : hierarchy) {
            Function formatFunction = formatFunctions.get(clazz);
            if (nonNull(formatFunction)) {
                return (String) formatFunction.apply(obj);
            }
        }
        /*
         * This shouldn't happen, the Object format function should always be retrieved if there is no function
         * associated to a class closer in obj's hierarchy.
         */
        return obj.toString();
    }

}

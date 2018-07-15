package fr.zelus.jarvis.util;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Provides a set of utility methods to dynamically load and construct classes.
 * <p>
 * The {@link Loader} wraps Java reflection API exceptions in {@link JarvisException}s, easing client code to handle
 * loading and construction errors.
 */
public class Loader {

    /**
     * Loads the Class with the provided {@code qualifiedName}, and casts it to the provided {@code superClass}.
     *
     * @param qualifiedName the name of the {@link Class} to load
     * @param superClass    the super-class of the {@link Class} to load
     * @param <T>           the super type of the {@link Class} to load
     * @return the loaded {@link Class} casted to the provided {@code superClass}
     * @throws JarvisException if the provided {@code qualifiedName} is not a valid {@link Class} name, or if the
     *                         loaded {@link Class} is not a sub-class of the provided {@code superClass}
     */
    public static <T> Class<? extends T> loadClass(String qualifiedName, Class<T> superClass) throws JarvisException {
        Log.info("Loading class {0}", qualifiedName);
        try {
            Class<?> clazz = Loader.class.getClassLoader().loadClass(qualifiedName);
            if (superClass.isAssignableFrom(clazz)) {
                return (Class<? extends T>) clazz;
            } else {
                String errorMessage = MessageFormat.format("The class {0} is not a subclass of {1}",
                        clazz.getSimpleName(), superClass.getSimpleName());
                Log.error(errorMessage);
                throw new JarvisException(errorMessage);
            }
        } catch (ClassNotFoundException e) {
            String errorMessage = MessageFormat.format("Cannot find the {0} with the name {1}", superClass
                    .getSimpleName(), qualifiedName);
            Log.error(errorMessage);
            throw new JarvisException(errorMessage, e);
        }
    }

    /**
     * Constructs a new instance of {@code clazz} with the given {@code parameters}.
     * <p>
     * This method iterates through the {@code clazz} {@link Constructor}s and attempt to find one that accepts the
     * provided {@code parameters}. Note that this method tries to call all the {@link Constructor}s that define as
     * many parameters as the ones provided. If the {@code parameters} types are known use
     * {@link #construct(Class, List, List)} that directly calls the appropriate {@link Constructor}.
     *
     * @param clazz      the {@link Class} to construct a new instance of
     * @param parameters the concrete parameters of the constructor to call
     * @param <T>        the type of the {@link Class} to construct an instance of
     * @return the constructed instance
     * @throws NoSuchMethodException if the provided {@clazz} does not define a constructor matching the provided
     *                               {@code parameters}
     * @see #construct(Class, List, List)
     */
    public static <T> T construct(Class<T> clazz, Object[] parameters) throws NoSuchMethodException {
        Log.info("Constructing {0} with the parameters ({1})", clazz.getSimpleName(), printArray(parameters));
        Constructor<?>[] constructors = clazz.getConstructors();
        for (int i = 0; i < constructors.length; i++) {
            Constructor<?> constructor = constructors[i];
            if (constructor.getParameterCount() == parameters.length) {
                /*
                 * Only try to call constructors defining as many parameters as the ones provided.
                 */
                try {
                    return (T) constructor.newInstance(parameters);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    /*
                     * The constructor does not accept the provided parameters, or an error occurred when calling it.
                     * In any case, we need to continue the iteration to try to find a suitable constructor.
                     */
                }
            }
        }
        String errorMessage = MessageFormat.format("Cannot find a {0} constructor for the parameters ({1})", clazz
                .getSimpleName(), printArray(parameters));
        Log.error(errorMessage);
        throw new NoSuchMethodException(errorMessage);
    }

    /**
     * Constructs a new instance of the provided {@code clazz} using its default constructor.
     * <p>
     * Use {@link #construct(Class, Class, Object)}, {@link #construct(Class, Class, Class, Object, Object)},
     * {@link #construct(Class, List, List)}, or {@link #construct(Class, Object[])} to construct an instance of
     * {@code clazz} with parameters.
     *
     * @param clazz the {@link Class} to construct a new instance of
     * @param <T>   the type of the {@link Class} to construct an instance of
     * @return the constructed instance
     * @throws JarvisException if an error occurred when calling the {@code clazz}'s constructor
     * @see #construct(Class, Class, Object)
     * @see #construct(Class, List, List)
     * @see #construct(Class, Object[])
     */
    public static <T> T construct(Class<T> clazz) throws JarvisException {
        Log.info("Constructing {0} with its default constructor", clazz.getSimpleName());
        try {
            return clazz.newInstance();
        } catch (ReflectiveOperationException e) {
            String errorMessage = MessageFormat.format("Cannot construct an instance of {0} with its default " +
                    "constructor", clazz.getSimpleName());
            Log.error(errorMessage);
            throw new JarvisException(errorMessage, e);
        }
    }

    /**
     * Constructs a new instance of the provided {@code clazz} with the given {@code parameter}.
     * <p>
     * This method is equivalent to {@code construct(clazz, Arrays.asList(parameterType), Arrays.asList(parameters)}.
     *
     * @param clazz         the {@link Class} to construct a new instance of
     * @param parameterType the parameter type in the constructor signature
     * @param parameter     the concrete parameter of the constructor to call
     * @param <T>           the type of the {@link Class} to construct an instance of
     * @return the constructed instance
     * @throws NoSuchMethodException if the provided {@code clazz} does not define a constructor matching the
     *                               provided {@code parameterType}
     * @throws JarvisException       if an error occurred when calling the {@code clazz}'s constructor
     * @see #construct(Class, List, List)
     */
    public static <T> T construct(Class<T> clazz, Class<?> parameterType, Object parameter) throws
            NoSuchMethodException, JarvisException {
        return construct(clazz, Arrays.asList(parameterType), Arrays.asList(parameter));
    }

    /**
     * Constructs a new instance of the provided {@code clazz} with the given {@code parameter1} and {@code parameter2}.
     * <p>
     * This method is equivalent to {@code construct(clazz, Arrays.asList(parameterType1, parameterType2), Arrays
     * .asList(parameter1, parameter2)}.
     *
     * @param clazz          the {@link Class} to construct a new instance of
     * @param parameterType1 the first parameter type in the constructor signature
     * @param parameterType2 the second parameter type in the constructor signature
     * @param parameter1     the first concrete parameter of the constructor to call
     * @param parameter2     the second concrete parameter of the constructor to call
     * @param <T>            the type of the {@link Class} to construct an instance of
     * @return the constructed instance
     * @throws NoSuchMethodException if the provided {@code clazz} does not define a constructor matching the
     *                               provided {@code parameterType1} and {@code parameterType2}
     * @throws JarvisException       if an error occurred when calling the {@code clazz}'s constructor
     * @see #construct(Class, List, List)
     */
    public static <T> T construct(Class<T> clazz, Class<?> parameterType1, Class<?> parameterType2, Object parameter1,
                                  Object parameter2) throws NoSuchMethodException, JarvisException {
        return construct(clazz, Arrays.asList(parameterType1, parameterType2), Arrays.asList(parameter1, parameter2));
    }

    /**
     * Constructs a new instance of the provided {@code clazz} with the given {@code parameters}.
     * <p>
     * This method uses the Java reflection API to find the constructor accepting the provided {@code
     * parameterTypes}, and attempts to instantiate the provided {@code clazz} using the given {@code parameters}.
     *
     * @param clazz          the {@link Class} to construct a new instance of
     * @param parameterTypes the {@link List} of parameter types in the constructor signature
     * @param parameters     the concrete parameters of the constructor to call
     * @param <T>            the type of the {@link Class} to construct an instance of
     * @return the constructed instance
     * @throws NoSuchMethodException if the provided {@code clazz} does not define a constructor matching the
     *                               provided {@code parameterTypes}
     * @throws JarvisException       if an error occurred when calling the {@code clazz}'s constructor
     */
    public static <T> T construct(Class<T> clazz, List<Class<?>> parameterTypes, List<Object> parameters) throws
            NoSuchMethodException, JarvisException {
        Log.info("Constructing {0} with parameters ({1})", clazz.getSimpleName(), printArray(parameters.toArray()));
        try {
            if (parameterTypes.isEmpty()) {
                return clazz.newInstance();
            } else {
                try {
                    /*
                     * Create a new Array to fill with the toArray method in order to avoid ClassCastExceptions.
                     */
                    Constructor<T> constructor = clazz.getConstructor(parameterTypes.toArray(new
                            Class<?>[parameterTypes.size()]));
                    return constructor.newInstance(parameters.toArray());
                } catch (NoSuchMethodException e) {
                    throw e;
                }
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new JarvisException(e);
        }
    }

    /**
     * Formats the provided {@code array} in a {@link String} used to log parameter values.
     * <p>
     * The returned {@link String} is "a1.toString(), a2.toString(), an.toString()", where <i>a1</i>,
     * <i>a2</i>, and <i>an</i> are elements in the provided {@code array}.
     *
     * @param array the array containing the parameter to print
     * @return a {@link String} containing the formatted parameters
     */
    private static String printArray(Object[] array) {
        List<String> toStringList = StreamSupport.stream(Arrays.asList(array).spliterator(), false).map(o ->
        {
            if (o instanceof String) {
                return "\"" + o.toString() + "\"";
            } else {
                return o.toString();
            }
        }).collect(Collectors.toList());
        return String.join(",", toStringList);
    }
}

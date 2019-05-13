package edu.uoc.som.jarvis.util;

import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.JarvisException;
import edu.uoc.som.jarvis.core.platform.RuntimePlatform;
import edu.uoc.som.jarvis.core.platform.io.RuntimeEventProvider;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
     * @throws NoSuchMethodException if the provided {@code clazz} does not define a constructor matching the provided
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
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
                        InvocationTargetException e) {
                    /*
                     * The constructor does not accept the provided parameters, or an error occurred when calling it.
                     * In any case, we need to continue the iteration to try to find a suitable constructor.
                     */
                }
            }
        }
        throw new NoSuchMethodException(MessageFormat.format("Cannot find a {0} constructor for the parameters ({1})",
                clazz.getSimpleName(), printArray(parameters)));
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
     * Constructs a new instance of the provided {@code runtimePlatformClass} with the given {@code jarvisCore} and
     * {@code configuration}.
     * <p>
     * This method first tries to construct an instance of the provided {@code runtimePlatformClass} with the provided
     * {@code jarvisCore} and {@code configuration}. If the {@link RuntimePlatform} does not define such constructor,
     * the method logs a warning and tries to construct an instance with only the {@code jarvisCore} parameter.
     * <p>
     * The {@code runtimePlatformClass} parameter can be loaded by using this class' {@link #loadClass(String, Class)}
     * utility method.
     *
     * @param runtimePlatformClass the {@link RuntimePlatform} {@link Class} to construct a new instance of
     * @param jarvisCore        the {@link JarvisCore} instance used to construct the {@link RuntimePlatform}
     * @param configuration     the {@link Configuration} instance used to construct the {@link RuntimePlatform}
     * @return the constructed {@link RuntimePlatform}
     * @throws JarvisException if the {@link RuntimePlatform} does not define a constructor matching the provided
     *                         parameters.
     * @see #construct(Class, Class, Class, Object, Object)
     * @see #loadClass(String, Class)
     */
    public static RuntimePlatform constructRuntimePlatform(Class<? extends RuntimePlatform> runtimePlatformClass, JarvisCore
            jarvisCore, Configuration configuration) {
        RuntimePlatform platform;
        try {
            platform = Loader.construct(runtimePlatformClass, JarvisCore.class, Configuration.class, jarvisCore,
                    configuration);
        } catch (NoSuchMethodException e) {
            Log.warn("Cannot find the method {0}({1},{2}), trying to initialize the platform with the its {0}({1})" +
                    "constructor", runtimePlatformClass.getSimpleName(), JarvisCore.class.getSimpleName(), Configuration
                    .class.getSimpleName());
            try {
                platform = Loader.construct(runtimePlatformClass, JarvisCore.class, jarvisCore);
                Log.warn("{0} {1} loaded with its default constructor, the platform will not be initialized with " +
                        "jarvis configuration", RuntimePlatform.class.getSimpleName(), runtimePlatformClass
                        .getSimpleName());
            } catch (NoSuchMethodException e1) {
                throw new JarvisException(MessageFormat.format("Cannot initialize {0}, the constructor {0}({1}) does " +
                        "not exist", runtimePlatformClass.getSimpleName(), JarvisCore.class.getSimpleName()), e1);
            }
        }
        return platform;
    }

    /**
     * Constructs a new instance of the provided {@code eventProviderClass} with the given {@code jarvisCore} and
     * {@code configuration}.
     * <p>
     * This method first tries to construct an instance of the provided {@code eventProviderClass} with the provided
     * {@code jarvisCore} and {@code configuration}. If the {@link RuntimeEventProvider} does not define such constructor,
     * the method logs a warning and tries to construct an instance with only the {@code jarvisCore} parameter.
     * <p>
     * The {@code eventProviderClass} parameter can be loaded by using this class" {@link #loadClass(String, Class)}
     * utility method.
     *
     * @param eventProviderClass the {@link RuntimeEventProvider} {@link Class} to construct a new instance of
     * @param runtimePlatform       the {@link RuntimePlatform} instance used to construct the {@link RuntimeEventProvider}
     * @param configuration      the {@link Configuration} instance used to construct the {@link RuntimeEventProvider}
     * @return the constructed {@link RuntimeEventProvider}
     * @throws JarvisException if the {@link RuntimeEventProvider} does not define a constructor matching the provided
     *                         parameters.
     * @see #construct(Class, Class, Class, Object, Object)
     * @see #loadClass(String, Class)
     */
    public static RuntimeEventProvider constructRuntimeEventProvider(Class<? extends RuntimeEventProvider> eventProviderClass, RuntimePlatform
            runtimePlatform, Configuration configuration) {
        RuntimeEventProvider runtimeEventProvider;
        try {
            runtimeEventProvider = Loader.construct(eventProviderClass, runtimePlatform.getClass(), Configuration.class,
                    runtimePlatform,
                    configuration);
        } catch (NoSuchMethodException e) {
            Log.warn("Cannot find the method {0}({1},{2}), trying to initialize the RuntimeEventProvider using its " +
                    "{0}({1}) constructor", eventProviderClass.getSimpleName(), runtimePlatform.getClass()
                    .getSimpleName(), Configuration.class.getSimpleName());
            try {
                runtimeEventProvider = Loader.construct(eventProviderClass, runtimePlatform.getClass(), runtimePlatform);
            } catch (NoSuchMethodException e1) {
                throw new JarvisException(MessageFormat.format("Cannot initialize {0}, the constructor {0}({1}) does " +
                        "not exist", eventProviderClass.getSimpleName(), runtimePlatform.getClass().getSimpleName()), e1);
            }
        }
        return runtimeEventProvider;

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
                return Objects.toString(o);
            }
        }).collect(Collectors.toList());
        return String.join(",", toStringList);
    }
}

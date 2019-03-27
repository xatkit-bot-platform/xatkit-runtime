package edu.uoc.som.jarvis.core.interpreter.operation.object;

import edu.uoc.som.jarvis.core.interpreter.operation.Operation;
import edu.uoc.som.jarvis.core.interpreter.operation.OperationException;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A reflexive {@link Operation} that invokes its corresponding source object's method.
 * <p>
 * This {@link Operation} is a wrapper that searches for a method of its source object with the provided {@code
 * methodName}, and accepting the provided {@code args}.
 */
public class ObjectOperation implements Operation {

    /**
     * The name of the method to invoke.
     */
    private String methodName;

    /**
     * Constructs a new {@link ObjectOperation} with the provided {@code methodName}.
     *
     * @param methodName the name of the method to invoke
     */
    public ObjectOperation(String methodName) {
        this.methodName = methodName;
    }

    /**
     * Searches for a source's method matching the provided {@code methodName} and {@code args} and invoke it.
     *
     * @param source the {@link Object} to invoke the {@link Operation} on
     * @param args   the arguments of the operation to invoke
     * @return the execution's result
     * @throws OperationException if there is no source's method matching the provided {@code methodName} and {@code
     *                            args}, or if an error occurred when invoking the retrieve method
     */
    @Override
    public Object invoke(Object source, List<Object> args) {
        try {
            /*
             * Create the array in the toArray() call allows to return typed array.
             */
            Class<?>[] argTypes = args.stream()
                    .map(p -> p.getClass())
                    .toArray(size -> new Class<?>[size]);
            Method method = getMethod(source.getClass(), methodName, argTypes);
            return method.invoke(source, args.toArray());
        } catch (NoSuchMethodException | SecurityException e) {
            throw new OperationException(
                    MessageFormat.format("Cannot compute operation {0}: the method {0}({1}) does not exist for object" +
                                    " {2} (class={3})", methodName, String.join(", ", getArgTypeNames(args)), source,
                            source.getClass().getSimpleName()), e);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new OperationException(MessageFormat.format("An error occurred when executing method {0}({1}) on " +
                    "object {2} (class={3}), see attached exception", methodName, String.join(", ",
                    getArgTypeNames(args)), source, source.getClass().getSimpleName()), e);
        }
    }

    /**
     * Retrieves the {@code sourceClass}'s {@link Method} matching the provided {@code methodName} and {@code argTypes}.
     *
     * @param sourceClass the {@link Class} to retrieve the {@link Method} from
     * @param methodName  the name of the {@link Method} to retrieve
     * @param argTypes    an array containing the types of the {@link Method} arguments
     * @return the retrieved {@link Method} if it exists
     * @throws NoSuchMethodException if there is no method matching the provided {@code methodName} and {@code argTypes}
     */
    private Method getMethod(Class<?> sourceClass, String methodName, Class<?>[] argTypes) throws NoSuchMethodException {
        if (argTypes.length == 0) {
            return sourceClass.getMethod(methodName);
        } else {
            for (Method method : sourceClass.getMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == argTypes.length) {
                    Class<?>[] methodParameterClasses = method.getParameterTypes();
                    boolean match = true;
                    for (int i = 0; i < methodParameterClasses.length; i++) {
                        /*
                         * Use commons-lang3 ClassUtils.isAssignable to check if the parameters can be assigned: this
                         * method takes care of unboxing primitive types and allows null assignations.
                         */
                        if (!ClassUtils.isAssignable(argTypes[i], methodParameterClasses[i])) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        return method;
                    }
                }
            }
        }
        throw new NoSuchMethodException(MessageFormat.format("Cannot find method {0} for class {2}",
                methodName, sourceClass.getSimpleName()));
    }

    /**
     * Returns a {@link List} of pretty-printed {@link Class} names for the provided {@code args}.
     *
     * @param args the {@link Object}s to return the {@link Class} names of
     * @return a {@link List} of pretty-printed {@link Class} names for the provided {@code args}
     */
    private List<String> getArgTypeNames(List<Object> args) {
        return args.stream()
                .map(p -> p.getClass().getSimpleName())
                .collect(Collectors.toList());
    }
}

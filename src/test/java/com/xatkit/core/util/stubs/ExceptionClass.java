package com.xatkit.core.util.stubs;

/**
 * A {@link Class} that always throws an exception when constructed.
 * <p>
 * This class is used to test the Xatkit {@link com.xatkit.util.Loader}.
 */
public class ExceptionClass {

    public ExceptionClass(String s1, String s2) {
        throw new RuntimeException("Error");
    }
}

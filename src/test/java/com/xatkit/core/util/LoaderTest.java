package com.xatkit.core.util;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.util.stubs.ExceptionClass;
import com.xatkit.core.util.stubs.LoadableClass;
import com.xatkit.util.Loader;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;

public class LoaderTest extends AbstractXatkitTest {

    @Test
    public void constructValidParameters() throws Exception {
        LoadableClass loaded = Loader.construct(LoadableClass.class, new String[]{"test", "test2"});
        assertThat(loaded).as("Not null").isNotNull();
    }

    @Test(expected = InvocationTargetException.class)
    public void constructExceptionInConstructor() throws Exception {
        ExceptionClass loaded = Loader.construct(ExceptionClass.class, new String[]{"test", "test2"});
    }

    @Test(expected = NoSuchMethodException.class)
    public void constructInvalidParameters() throws Exception {
        LoadableClass loaded = Loader.construct(LoadableClass.class, new Object[]{"test", 1});
    }

    @Test(expected = NoSuchMethodException.class)
    public void constructTooManyParameters() throws Exception {
        LoadableClass loaded = Loader.construct(LoadableClass.class, new Object[]{"test", "test", "test"});
    }
}

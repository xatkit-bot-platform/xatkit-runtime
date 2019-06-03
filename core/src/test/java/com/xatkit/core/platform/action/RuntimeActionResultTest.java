package com.xatkit.core.platform.action;

import com.xatkit.AbstractXatkitTest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeActionResultTest extends AbstractXatkitTest {

    private RuntimeActionResult result;

    @Test
    public void constructNullResultValidExecutionTime() {
        result = new RuntimeActionResult(null, 3);
        assertThat(result.getResult()).as("Null result").isNull();
        assertThat(result.getExecutionTime()).as("Valid execution time").isEqualTo(3);
        checkObjectLongConstructor(result);
    }

    @Test
    public void constructNotNullResultValidExecutionTime() {
        Integer rawResult = new Integer(10);
        result = new RuntimeActionResult(rawResult, 3);
        assertThat(result.getResult()).as("Valid result").isEqualTo(rawResult);
        assertThat(result.getExecutionTime()).as("Valid execution time").isEqualTo(3);
        checkObjectLongConstructor(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNullResultInvalidExecutionTime() {
        result = new RuntimeActionResult(null, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNotNullResultInvalidExecutionTime() {
        result = new RuntimeActionResult(new Integer(10), -1);
    }

    @Test
    public void constructNullResultNotNullExceptionValidExecutionTime() {
        Exception e = new RuntimeException("message");
        result = new RuntimeActionResult(null, e, 3);
        assertThat(result.getResult()).as("Null result").isNull();
        assertThat(result.getExecutionTime()).as("Valid execution time").isEqualTo(3);
        assertThat(result.getThrownException()).as("Valid exception").isEqualTo(e);
        assertThat(result.isError()).as("Is error").isTrue();
    }

    @Test
    public void constructNullResultNullExceptionValidExecutionTime() {
        result = new RuntimeActionResult(null, null, 3);
        assertThat(result.getResult()).as("Null result").isNull();
        assertThat(result.getExecutionTime()).as("Valid execution time").isEqualTo(3);
        assertThat(result.getThrownException()).as("Null exception").isNull();
        assertThat(result.isError()).as("Not error").isFalse();
    }

    @Test
    public void constructNotNullResultNotNullExceptionValidExecutionTime() {
        Integer rawResult = new Integer(10);
        Exception e = new RuntimeException("message");
        result = new RuntimeActionResult(rawResult, e, 3);
        assertThat(result.getResult()).as("Valid result").isEqualTo(rawResult);
        assertThat(result.getExecutionTime()).as("Valid execution time").isEqualTo(3);
        assertThat(result.getThrownException()).as("Valid exception").isEqualTo(e);
        assertThat(result.isError()).as("Is error").isTrue();
    }

    @Test
    public void constructNotNullResultNullExceptionValidExecutionTime() {
        Integer rawResult = new Integer(10);
        result = new RuntimeActionResult(rawResult, null, 3);
        assertThat(result.getResult()).as("Valid result").isEqualTo(rawResult);
        assertThat(result.getExecutionTime()).as("Valid execution time").isEqualTo(3);
        assertThat(result.getThrownException()).as("Null exception").isNull();
        assertThat(result.isError()).as("Not error").isFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNullResultNotNullExceptionInvalidExecutionTime() {
        result = new RuntimeActionResult(null, new RuntimeException("message"), -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNullResultNullExceptionInvalidExecutionTime() {
        result = new RuntimeActionResult(null, null, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNotNullResultNotNullExceptionInvalidExecutionTime() {
        result = new RuntimeActionResult(new Integer(10), new RuntimeException("message"), -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNotNullResultNullExceptionInvalidExecutionTime() {
        result = new RuntimeActionResult(new Integer(10), null, -1);
    }

    /*
     * No need to test the getters, they are already tested in the constructor tests.
     */

    /*
     * Checks the {@code result} values that should not be modified after calling RuntimeActionResult(Object, long).
     */
    private void checkObjectLongConstructor(RuntimeActionResult result) {
        assertThat(result.getThrownException()).as("No thrown exception").isNull();
        assertThat(result.isError()).as("Not error").isFalse();
    }

}

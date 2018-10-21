package fr.zelus.jarvis.core;

import fr.zelus.jarvis.AbstractJarvisTest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JarvisActionResultTest extends AbstractJarvisTest {

    private JarvisActionResult result;

    @Test
    public void constructNullResultValidExecutionTime() {
        result = new JarvisActionResult(null, 3);
        assertThat(result.getResult()).as("Null result").isNull();
        assertThat(result.getExecutionTime()).as("Valid execution time").isEqualTo(3);
        checkObjectLongConstructor(result);
    }

    @Test
    public void constructNotNullResultValidExecutionTime() {
        Integer rawResult = new Integer(10);
        result = new JarvisActionResult(rawResult, 3);
        assertThat(result.getResult()).as("Valid result").isEqualTo(rawResult);
        assertThat(result.getExecutionTime()).as("Valid execution time").isEqualTo(3);
        checkObjectLongConstructor(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNullResultInvalidExecutionTime() {
        result = new JarvisActionResult(null, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNotNullResultInvalidExecutionTime() {
        result = new JarvisActionResult(new Integer(10), -1);
    }

    @Test
    public void constructNullResultNotNullExceptionValidExecutionTime() {
        Exception e = new RuntimeException("message");
        result = new JarvisActionResult(null, e, 3);
        assertThat(result.getResult()).as("Null result").isNull();
        assertThat(result.getExecutionTime()).as("Valid execution time").isEqualTo(3);
        assertThat(result.getThrownException()).as("Valid exception").isEqualTo(e);
        assertThat(result.isError()).as("Is error").isTrue();
    }

    @Test
    public void constructNullResultNullExceptionValidExecutionTime() {
        result = new JarvisActionResult(null, null, 3);
        assertThat(result.getResult()).as("Null result").isNull();
        assertThat(result.getExecutionTime()).as("Valid execution time").isEqualTo(3);
        assertThat(result.getThrownException()).as("Null exception").isNull();
        assertThat(result.isError()).as("Not error").isFalse();
    }

    @Test
    public void constructNotNullResultNotNullExceptionValidExecutionTime() {
        Integer rawResult = new Integer(10);
        Exception e = new RuntimeException("message");
        result = new JarvisActionResult(rawResult, e, 3);
        assertThat(result.getResult()).as("Valid result").isEqualTo(rawResult);
        assertThat(result.getExecutionTime()).as("Valid execution time").isEqualTo(3);
        assertThat(result.getThrownException()).as("Valid exception").isEqualTo(e);
        assertThat(result.isError()).as("Is error").isTrue();
    }

    @Test
    public void constructNotNullResultNullExceptionValidExecutionTime() {
        Integer rawResult = new Integer(10);
        result = new JarvisActionResult(rawResult, null, 3);
        assertThat(result.getResult()).as("Valid result").isEqualTo(rawResult);
        assertThat(result.getExecutionTime()).as("Valid execution time").isEqualTo(3);
        assertThat(result.getThrownException()).as("Null exception").isNull();
        assertThat(result.isError()).as("Not error").isFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNullResultNotNullExceptionInvalidExecutionTime() {
        result = new JarvisActionResult(null, new RuntimeException("message"), -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNullResultNullExceptionInvalidExecutionTime() {
        result = new JarvisActionResult(null, null, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNotNullResultNotNullExceptionInvalidExecutionTime() {
        result = new JarvisActionResult(new Integer(10), new RuntimeException("message"), -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNotNullResultNullExceptionInvalidExecutionTime() {
        result = new JarvisActionResult(new Integer(10), null, -1);
    }

    /*
     * No need to test the getters, they are already tested in the constructor tests.
     */

    /*
     * Checks the {@code result} values that should not be modified after calling JarvisActionResult(Object, long).
     */
    private void checkObjectLongConstructor(JarvisActionResult result) {
        assertThat(result.getThrownException()).as("No thrown exception").isNull();
        assertThat(result.isError()).as("Not error").isFalse();
    }

}

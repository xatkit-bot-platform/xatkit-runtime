package fr.zelus.jarvis.core.session;

import fr.zelus.jarvis.AbstractJarvisTest;
import fr.zelus.jarvis.core.JarvisException;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Test;

import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JarvisContextTest extends AbstractJarvisTest {

    private JarvisContext context;

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        context = new JarvisContext(null);
    }

    @Test
    public void constructEmptyConfiguration() {
        context = new JarvisContext(new BaseConfiguration());
        checkJarvisContext(context);
        assertThat(context.getVariableTimeout()).as("Default variable timeout value").isEqualTo(2);
    }

    @Test
    public void constructValidConfiguration() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisContext.VARIABLE_TIMEOUT_KEY, 10);
        context = new JarvisContext(configuration);
        checkJarvisContext(context);
        assertThat(context.getVariableTimeout()).as("Valid variable timeout value").isEqualTo(10);
    }

    @Test
    public void constructValidContext() {
        context = new JarvisContext();
        checkJarvisContext(context);
    }

    @Test(expected = NullPointerException.class)
    public void setContextValueNullContext() {
        context = new JarvisContext();
        context.setContextValue(null, "key", "value");
    }

    @Test(expected = NullPointerException.class)
    public void setContextValueNullKey() {
        context = new JarvisContext();
        context.setContextValue("context", null, "value");
    }

    @Test
    public void setContextValueNullValue() {
        context = new JarvisContext();
        context.setContextValue("context", "key", null);
        checkContextMap(context, "context", "key", null);
    }

    @Test
    public void setContextValueValidValue() {
        context = new JarvisContext();
        context.setContextValue("context", "key", "value");
        checkContextMap(context, "context", "key", "value");
    }

    @Test(expected = NullPointerException.class)
    public void getContextVariablesNullContext() {
        context = new JarvisContext();
        context.getContextVariables(null);
    }

    @Test
    public void getContextVariablesNotSetContext() {
        context = new JarvisContext();
        Map<String, Object> contextVariables = context.getContextVariables("context");
        assertThat(contextVariables).as("Empty context variables").isEmpty();
    }

    @Test
    public void getContextVariablesSetContextSetKey() {
        context = new JarvisContext();
        context.setContextValue("context", "key", "value");
        Map<String, Object> contextVariables = context.getContextVariables("context");
        assertThat(contextVariables).as("Not empty context variables").isNotEmpty();
        assertThat(contextVariables).as("Context variables contains the set key").containsKey("key");
        assertThat(contextVariables.get("key")).as("Context variables contains the set value").isEqualTo("value");
    }

    @Test(expected = NullPointerException.class)
    public void getContextValueNullContext() {
        context = new JarvisContext();
        context.getContextValue(null, "key");
    }

    @Test(expected = NullPointerException.class)
    public void getContextValueNullKey() {
        context = new JarvisContext();
        context.getContextValue("context", null);
    }

    @Test
    public void getContextValueNotSetContext() {
        context = new JarvisContext();
        Object value = context.getContextValue("context", "key");
        assertThat(value).as("Null context value").isNull();
    }

    @Test
    public void getContextValueSetContextNotSetKey() {
        context = new JarvisContext();
        context.setContextValue("context", "key", "value");
        Object value = context.getContextValue("context", "test");
        assertThat(value).as("Null context value").isNull();
    }

    @Test
    public void getContextValueSetContextSetKey() {
        context = new JarvisContext();
        context.setContextValue("context", "key", "value");
        Object value = context.getContextValue("context", "key");
        assertThat(value).as("Not null context value").isNotNull();
        assertThat(value).as("Valid value").isEqualTo("value");
    }

    @Test(expected = NullPointerException.class)
    public void mergeNullJarvisContext() {
        context = new JarvisContext();
        context.merge(null);
    }

    @Test(expected = JarvisException.class)
    public void mergeDuplicatedContext() {
        context = new JarvisContext();
        context.setContextValue("context", "key", "value");
        JarvisContext context2 = new JarvisContext();
        context2.setContextValue("context", "key2", "value2");
        context.merge(context2);
    }

    @Test
    public void mergeEmptyTargetContext() {
        context = new JarvisContext();
        context.setContextValue("context", "key", "value");
        context.merge(new JarvisContext());
        assertThat(context.getContextValue("context", "key")).as("Not null context value").isNotNull();
        assertThat(context.getContextValue("context", "key")).as("Valid context value").isEqualTo("value");
    }

    @Test
    public void mergeEmptySourceContext() {
        context = new JarvisContext();
        JarvisContext context2 = new JarvisContext();
        context2.setContextValue("context", "key", "value");
        context.merge(context2);
        assertThat(context.getContextValue("context", "key")).as("Not null merged context value").isNotNull();
        assertThat(context.getContextValue("context", "key")).as("Valid merged context value").isEqualTo("value");
    }

    @Test
    public void mergeNotEmptyContextsNoDuplicates() {
        context = new JarvisContext();
        context.setContextValue("context", "key", "value");
        JarvisContext context2 = new JarvisContext();
        context2.setContextValue("context2", "key2", "value2");
        context.merge(context2);
        assertThat(context.getContextValue("context", "key")).as("Not null context value").isNotNull();
        assertThat(context.getContextValue("context", "key")).as("Valid context value").isEqualTo("value");
        assertThat(context.getContextValue("context2", "key2")).as("Not null merged context value").isNotNull();
        assertThat(context.getContextValue("context2", "key2")).as("Valid context value").isEqualTo("value2");
    }

    @Test
    public void updateTargetContextAfterMerge() {
        context = new JarvisContext();
        JarvisContext context2 = new JarvisContext();
        context2.setContextValue("context2", "key2", "value2");
        context.merge(context2);
        // Add a context
        context2.setContextValue("context3", "key3", "value3");
        // Add a value in the merged context
        context2.setContextValue("context2", "newKey", "newValue");
        assertThat(context.getContextVariables("context3")).as("New context not merged").isEmpty();
        assertThat(context.getContextValue("context2", "newKey")).as("New value not merged").isNull();
    }

    /*
     * We do not test updates on context values themselves, because they are not cloned (see #129)
     */

    @Test(expected = NullPointerException.class)
    public void fillContextValuesNullMessage() {
        context = new JarvisContext();
        context.fillContextValues(null);
    }

    @Test
    public void fillContextValuesEmptyMessage() {
        context = new JarvisContext();
        String result = context.fillContextValues("");
        assertThat(result).as("Empty result").isEmpty();
    }

    @Test
    public void fillContextValuesNotSetContext() {
        context = new JarvisContext();
        String result = context.fillContextValues("{$context.test}");
        assertThat(result).as("Not replaced variable").isEqualTo("{$context.test}");
    }

    @Test
    public void fillContextValuesSetContextNotSetKey() {
        context = new JarvisContext();
        context.setContextValue("context", "key", "value");
        String result = context.fillContextValues("{$context.test}");
        assertThat(result).as("Not replaced variable").isEqualTo("{$context.test}");
    }

    @Test
    public void fillContextValuesSetContextSetKey() {
        context = new JarvisContext();
        context.setContextValue("context", "key", "value");
        String result = context.fillContextValues("{$context.key}");
        assertThat(result).as("Replaced variable").isEqualTo("value");
    }

    @Test
    public void fillContextValuesSetContextSetKeyLongMessage() {
        context = new JarvisContext();
        context.setContextValue("context", "key", "value");
        String result = context.fillContextValues("This is a {$context.key} test");
        assertThat(result).as("Replaced variable").isEqualTo("This is a value test");
    }

    @Test
    public void fillContextValuesSetContextSetKeyFuture() {
        context = new JarvisContext();
        Future<String> f = CompletableFuture.completedFuture("value");
        context.setContextValue("context", "key", f);
        String result = context.fillContextValues("This is a {$context.key} test");
        assertThat(result).as("Replaced future variable").isEqualTo("This is a value test");
    }

    @Test
    public void fillContextValuesSetContextSetKeyFutureNotString() {
        context = new JarvisContext();
        Future<Integer> f = CompletableFuture.completedFuture(1);
        context.setContextValue("context", "key", f);
        String result = context.fillContextValues("This is a {$context.key} test");
        assertThat(result).as("Replaced future non String variable").isEqualTo("This is a 1 test");
    }

    @Test
    public void fillContextValuesTwoValuesSetUnset() {
        context = new JarvisContext();
        context.setContextValue("context", "key", "value");
        String result = context.fillContextValues("This is a {$context.key} test from {$context.test}");
        assertThat(result).as("First variable replaced / second variable ignored").isEqualTo("This is a value test " +
                "from {$context.test}");
    }

    @Test
    public void fillContextValuesTwoValuesSet() {
        context = new JarvisContext();
        context.setContextValue("context", "key", "value");
        context.setContextValue("context", "test", "testValue");
        String result = context.fillContextValues("This is a {$context.key} test from {$context.test}");
        assertThat(result).as("Replaced variables").isEqualTo("This is a value test from testValue");
    }

    @Test
    public void fillContextValuesTwoFutureValuesSet() {
        context = new JarvisContext();
        Future<String> valueFuture = CompletableFuture.completedFuture("value");
        Future<String> testValueFuture = CompletableFuture.completedFuture("testValue");
        context.setContextValue("context", "key", valueFuture);
        context.setContextValue("context", "test", testValueFuture);
        String result = context.fillContextValues("This is a {$context.key} test from {$context.test}");
        assertThat(result).as("Replaced future variables").isEqualTo("This is a value test from testValue");
    }

    @Test
    public void fillContextValueSlowFuture() {
        context = new JarvisContext();
        Future<Boolean> future = CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                try {
                    wait(10000);
                    return false;
                } catch (InterruptedException e) {
                    return true;
                }
            }
        });
        context.setContextValue("context", "key", future);
        String result = context.fillContextValues("This is a boolean value: {$context.key}");
        assertThatThrownBy(() -> future.get()).as("Future.get() throws an exception").isInstanceOf
                (CancellationException.class);
        assertThat(result).as("Not replaced future variable").isEqualTo("This is a boolean value: <Task took too long" +
                " to complete>");
    }

    @Test
    public void fillContextValueSlowFutureCustomVariableTimeout() throws InterruptedException, ExecutionException {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(JarvisContext.VARIABLE_TIMEOUT_KEY, 5);
        context = new JarvisContext(configuration);
        Future<Boolean> future = CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                try {
                    wait(4000);
                    return false;
                } catch (InterruptedException e) {
                    return true;
                }
            }
        });
        context.setContextValue("context", "key", future);
        String result = context.fillContextValues("This is a boolean value: {$context.key}");
        /*
         * This should not throw an exception: the task should not be cancelled according to the provided variable
         * timeout value.
         */
        Boolean futureValue = future.get();
        assertThat(result).as("Replaced future variable").isEqualTo(MessageFormat.format("This is a boolean value: " +
                "{0}", futureValue));

    }

    private void checkJarvisContext(JarvisContext context) {
        assertThat(context.getContextMap()).as("Not null context map").isNotNull();
        assertThat(context.getContextMap()).as("Empty context map").isEmpty();
    }

    private void checkContextMap(JarvisContext context, String expectedContext, String expectedKey, Object
            expectedValue) {
        Map<String, Map<String, Object>> rawContext = context.getContextMap();
        assertThat(rawContext).as("Context map contains the set context").containsKey(expectedContext);
        Map<String, Object> contextVariables = rawContext.get(expectedContext);
        assertThat(contextVariables).as("Context map contains the set key").containsKey(expectedKey);
        assertThat(contextVariables.get(expectedKey)).as("Context map contains the value").isEqualTo(expectedValue);
    }
}

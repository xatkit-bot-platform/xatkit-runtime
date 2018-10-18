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
        context.setContextValue(null, 5, "key", "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setContextValueZeroLifespanCount() {
        context = new JarvisContext();
        context.setContextValue("context", 0, "key", "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setContextValueNegativeLifespanCount() {
        context = new JarvisContext();
        context.setContextValue("context", -1, "key", "value");
    }

    @Test(expected = NullPointerException.class)
    public void setContextValueNullKey() {
        context = new JarvisContext();
        context.setContextValue("context", 5, null, "value");
    }

    @Test
    public void setContextValueNullValue() {
        context = new JarvisContext();
        context.setContextValue("context", 5, "key", null);
        checkContextMap(context, "context", "key", null);
        checkLifespanMap(context, "context",5);
    }

    @Test
    public void setContextValueValidValue() {
        context = new JarvisContext();
        context.setContextValue("context", 5, "key", "value");
        checkContextMap(context, "context", "key", "value");
        checkLifespanMap(context, "context", 5);
    }

    @Test
    public void setContextValueUpdateValueGreaterLifespanCount() {
        context = new JarvisContext();
        context.setContextValue("context", 5, "key", "value");
        context.setContextValue("context", 6, "key", "newValue");
        checkContextMap(context, "context", "key", "newValue");
        checkLifespanMap(context, "context", 6);
    }

    @Test
    public void setContextValueUpdateValueLesserLifespanCount() {
        context = new JarvisContext();
        context.setContextValue("context", 5, "key", "value");
        context.setContextValue("context", 4, "key", "newValue");
        checkContextMap(context, "context", "key", "newValue");
        checkLifespanMap(context, "context", 5);
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
        context.setContextValue("context", 5, "key", "value");
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
        context.setContextValue("context", 5, "key", "value");
        Object value = context.getContextValue("context", "test");
        assertThat(value).as("Null context value").isNull();
    }

    @Test
    public void getContextValueSetContextSetKey() {
        context = new JarvisContext();
        context.setContextValue("context", 5, "key", "value");
        Object value = context.getContextValue("context", "key");
        assertThat(value).as("Not null context value").isNotNull();
        assertThat(value).as("Valid value").isEqualTo("value");
    }

    @Test(expected = NullPointerException.class)
    public void getContextLifespanCountNullContext() {
        context = new JarvisContext();
        context.getContextLifespanCount(null);
    }

    @Test(expected = JarvisException.class)
    public void getContextLifespanCountNotSetContext() {
        context = new JarvisContext();
        context.getContextLifespanCount("context");
    }

    @Test
    public void getContextLifespanCountSetContext() {
        context = new JarvisContext();
        context.setContextValue("context", 5, "key", "value");
        int lifespanCount = context.getContextLifespanCount("context");
        assertThat(lifespanCount).as("Valid lifespan count").isEqualTo(5);
    }

    @Test
    public void getContextLifespanCountUpdatedContextGreater() {
        context = new JarvisContext();
        context.setContextValue("context", 2, "key", "value");
        context.setContextValue("context", 5, "key", "value");
        int lifespanCount = context.getContextLifespanCount("context");
        assertThat(lifespanCount).as("Updated lifespan count").isEqualTo(5);
    }

    @Test
    public void getContextLifespanCountUpdatedContextLesser() {
        context = new JarvisContext();
        context.setContextValue("context", 5, "key", "value");
        context.setContextValue("context", 2, "key", "value");
        int lifespanCount = context.getContextLifespanCount("context");
        assertThat(lifespanCount).as("Not updated lifespan count").isEqualTo(5);
    }

    @Test
    public void getContextLifespanCountDecrementedContext() {
        context = new JarvisContext();
        context.setContextValue("context", 5, "key", "value");
        context.decrementLifespanCounts();
        int lifespanCount = context.getContextLifespanCount("context");
        assertThat(lifespanCount).as("Decremented lifespan count").isEqualTo(4);
    }

    @Test(expected = JarvisException.class)
    public void getContextLifespanCountFullyDecrementedContext() {
        context = new JarvisContext();
        context.setContextValue("context", 1, "key", "value");
        context.decrementLifespanCounts();
        context.getContextLifespanCount("context");
    }

    @Test
    public void decrementLifespanCountsNoRegisteredContext() {
        /*
         * This test ensures that the method does not have side effects on the context maps when there is no context
         * to decrement.
         */
        context = new JarvisContext();
        context.decrementLifespanCounts();
        assertThat(context.getContextMap()).as("Empty context map").isEmpty();
        assertThat(context.getLifespanCountsMap()).as("Empty lifespan count map").isEmpty();
    }

    @Test
    public void decrementLifespanCountSingleContext() {
        context = new JarvisContext();
        context.setContextValue("context", 5, "key", "value");
        context.decrementLifespanCounts();
        assertThat(context.getLifespanCountsMap()).as("Lifespan count map contains 1 element").hasSize(1);
        checkLifespanMap(context, "context", 4);
        assertThat(context.getContextMap()).as("Context map contains 1 element").hasSize(1);
        checkContextMap(context, "context", "key", "value");
        // getContextLifespanCount(context) already tested
    }

    @Test
    public void decrementLifespanCountMultipleContext() {
        context = new JarvisContext();
        context.setContextValue("context1", 5, "key1", "value1");
        context.setContextValue("context2", 5, "key2", "value2");
        context.decrementLifespanCounts();
        assertThat(context.getLifespanCountsMap()).as("Lifespan count map contains 2 elements").hasSize(2);
        checkLifespanMap(context, "context1", 4);
        checkLifespanMap(context, "context2", 4);
        assertThat(context.getContextMap()).as("Context map contains 2 elements").hasSize(2);
        checkContextMap(context, "context1", "key1", "value1");
        checkContextMap(context, "context2", "key2", "value2");
    }

    @Test
    public void decrementLifespanCountMultipleContextFullyDecrementOne() {
        context = new JarvisContext();
        context.setContextValue("context1", 1, "key1", "value1");
        context.setContextValue("context2", 5, "key2", "value2");
        context.decrementLifespanCounts();
        context.decrementLifespanCounts();
        assertThat(context.getLifespanCountsMap()).as("Lifespan count map contains 1 element").hasSize(1);
        checkLifespanMap(context, "context2", 3);
        assertThat(context.getContextMap()).as("Context map contains 1 element");
        checkContextMap(context, "context2", "key2", "value2");
    }

    @Test
    public void decrementLifespanCountFullyDecrementMultipleContexts() {
        context = new JarvisContext();
        context.setContextValue("context1", 2, "key1", "value1");
        context.setContextValue("context2", 2, "key2", "value2");
        context.decrementLifespanCounts();
        context.decrementLifespanCounts();
        assertThat(context.getLifespanCountsMap()).as("Lifespan count map is empty").isEmpty();
        assertThat(context.getContextMap()).as("Context map is empty").isEmpty();
    }

    @Test(expected = NullPointerException.class)
    public void mergeNullJarvisContext() {
        context = new JarvisContext();
        context.merge(null);
    }

    @Test(expected = JarvisException.class)
    public void mergeDuplicatedContext() {
        context = new JarvisContext();
        context.setContextValue("context", 5, "key", "value");
        JarvisContext context2 = new JarvisContext();
        context2.setContextValue("context", 5, "key2", "value2");
        context.merge(context2);
    }

    @Test
    public void mergeEmptyTargetContext() {
        context = new JarvisContext();
        context.setContextValue("context", 5, "key", "value");
        context.merge(new JarvisContext());
        assertThat(context.getContextValue("context", "key")).as("Not null context value").isNotNull();
        assertThat(context.getContextValue("context", "key")).as("Valid context value").isEqualTo("value");
        assertThat(context.getContextLifespanCount("context")).as("Valid context lifespan count").isEqualTo(5);
    }

    @Test
    public void mergeEmptySourceContext() {
        context = new JarvisContext();
        JarvisContext context2 = new JarvisContext();
        context2.setContextValue("context", 5, "key", "value");
        context.merge(context2);
        assertThat(context.getContextValue("context", "key")).as("Not null merged context value").isNotNull();
        assertThat(context.getContextValue("context", "key")).as("Valid merged context value").isEqualTo("value");
        assertThat(context.getContextLifespanCount("context")).as("Valid context lifespan count").isEqualTo(5);
    }

    @Test
    public void mergeNotEmptyContextsNoDuplicates() {
        context = new JarvisContext();
        context.setContextValue("context", 4, "key", "value");
        JarvisContext context2 = new JarvisContext();
        context2.setContextValue("context2", 5, "key2", "value2");
        context.merge(context2);
        assertThat(context.getContextValue("context", "key")).as("Not null context value").isNotNull();
        assertThat(context.getContextValue("context", "key")).as("Valid context value").isEqualTo("value");
        assertThat(context.getContextValue("context2", "key2")).as("Not null merged context value").isNotNull();
        assertThat(context.getContextValue("context2", "key2")).as("Valid context value").isEqualTo("value2");
        assertThat(context.getContextLifespanCount("context")).as("Valid context 1 lifespan count").isEqualTo(4);
        assertThat(context.getContextLifespanCount("context2")).as("Valid context 2 lifespan count").isEqualTo(5);
    }

    @Test
    public void updateTargetContextAfterMerge() {
        context = new JarvisContext();
        JarvisContext context2 = new JarvisContext();
        context2.setContextValue("context2", 5, "key2", "value2");
        context.merge(context2);
        // Add a context
        context2.setContextValue("context3", 5, "key3", "value3");
        // Add a value in the merged context
        context2.setContextValue("context2", 5, "newKey", "newValue");
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
        context.setContextValue("context", 5, "key", "value");
        String result = context.fillContextValues("{$context.test}");
        assertThat(result).as("Not replaced variable").isEqualTo("{$context.test}");
    }

    @Test
    public void fillContextValuesSetContextSetKey() {
        context = new JarvisContext();
        context.setContextValue("context", 5, "key", "value");
        String result = context.fillContextValues("{$context.key}");
        assertThat(result).as("Replaced variable").isEqualTo("value");
    }

    @Test
    public void fillContextValuesSetContextSetKeyLongMessage() {
        context = new JarvisContext();
        context.setContextValue("context", 5, "key", "value");
        String result = context.fillContextValues("This is a {$context.key} test");
        assertThat(result).as("Replaced variable").isEqualTo("This is a value test");
    }

    @Test
    public void fillContextValuesSetContextSetKeyFuture() {
        context = new JarvisContext();
        Future<String> f = CompletableFuture.completedFuture("value");
        context.setContextValue("context", 5, "key", f);
        String result = context.fillContextValues("This is a {$context.key} test");
        assertThat(result).as("Replaced future variable").isEqualTo("This is a value test");
    }

    @Test
    public void fillContextValuesSetContextSetKeyFutureNotString() {
        context = new JarvisContext();
        Future<Integer> f = CompletableFuture.completedFuture(1);
        context.setContextValue("context", 5, "key", f);
        String result = context.fillContextValues("This is a {$context.key} test");
        assertThat(result).as("Replaced future non String variable").isEqualTo("This is a 1 test");
    }

    @Test
    public void fillContextValuesTwoValuesSetUnset() {
        context = new JarvisContext();
        context.setContextValue("context", 5, "key", "value");
        String result = context.fillContextValues("This is a {$context.key} test from {$context.test}");
        assertThat(result).as("First variable replaced / second variable ignored").isEqualTo("This is a value test " +
                "from {$context.test}");
    }

    @Test
    public void fillContextValuesTwoValuesSet() {
        context = new JarvisContext();
        context.setContextValue("context", 5, "key", "value");
        context.setContextValue("context", 5, "test", "testValue");
        String result = context.fillContextValues("This is a {$context.key} test from {$context.test}");
        assertThat(result).as("Replaced variables").isEqualTo("This is a value test from testValue");
    }

    @Test
    public void fillContextValuesNoSpace() {
        context = new JarvisContext();
        context.setContextValue("context", 5, "key1", "value1");
        context.setContextValue("context", 5, "key2", "value2");
        String result = context.fillContextValues("{$context.key1}{$context.key2}");
        assertThat(result).as("Replaced variables").isEqualTo("value1value2");
    }

    @Test
    public void fillContextValuesTwoFutureValuesSet() {
        context = new JarvisContext();
        Future<String> valueFuture = CompletableFuture.completedFuture("value");
        Future<String> testValueFuture = CompletableFuture.completedFuture("testValue");
        context.setContextValue("context", 5, "key", valueFuture);
        context.setContextValue("context", 5, "test", testValueFuture);
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
        context.setContextValue("context", 5, "key", future);
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
        context.setContextValue("context", 5, "key", future);
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

    private void checkLifespanMap(JarvisContext context, String expectedContext, int expectedLifespanCount) {
        Map<String, Integer> rawLifespanCounts = context.getLifespanCountsMap();
        assertThat(rawLifespanCounts).as("Context lifespan count map contains the set context").containsKey
                (expectedContext);
        assertThat(rawLifespanCounts.get(expectedContext)).as("Valid lifespan count value for the set context")
                .isEqualTo(expectedLifespanCount);
    }
}

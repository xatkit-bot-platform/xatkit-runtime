package com.xatkit;

import fr.inria.atlanmod.commons.log.Log;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.time.Duration;
import java.time.Instant;

/**
 * A generic test case that defines utility methods and JUnit {@link Rule}s to use in Xatkit test cases.
 * <p>
 * All the Xatkit tests should extend this utility class, that adds execution logs before/after executing each test
 * method, easing build issue debugging.
 */
public abstract class AbstractXatkitTest {

    /**
     * The {@link TestWatcher} used to log execution information when starting and finishing a test method execution.
     */
    @Rule
    public TestRule watcher = new TestWatcher() {

        /**
         * The test method execution starting {@link Instant}.
         */
        private Instant startingInstant;

        /**
         * Adds an utility log displaying the name of the current executed method.
         * @param description the {@link Description} of the executed test method
         */
        @Override
        protected void starting(Description description) {
            startingInstant = Instant.now();
            Log.info("Starting test {0}.{1}", description.getClassName(), description.getMethodName());
        }

        /**
         * Adds an utility log displaying the name of the executed method and its computation time.
         * @param description the {@link Description} of the executed test method
         */
        @Override
        protected void finished(Description description) {
            Instant finishedInstant = Instant.now();
            Log.info("Test {0}.{1} completed in {2} ms", description.getClassName(), description.getMethodName(),
                    Duration.between(startingInstant, finishedInstant).toMillis());
        }
    };
}

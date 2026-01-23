package com.example.camel.io.spi;

import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * Minimal base to simplify action unit tests:
 * - call init(providerId, actionName) to discover via ServiceLoader
 *   or pass a Supplier in the constructor
 * - use send(job) to execute the action via LocalActionRunner
 */
public abstract class BaseActionTest {
    private Supplier<? extends ActionExecutor> factory;

    protected BaseActionTest() {
    }

    protected BaseActionTest(Supplier<? extends ActionExecutor> factory) {
        this.factory = factory;
    }

    /**
     * Alternate constructor: discover the action via ServiceLoader by providerId and actionName.
     */
    protected BaseActionTest(String providerId, String actionName) {
        init(providerId, actionName);
    }

    /**
     * Initialize using ServiceLoader lookup. Useful for tests with @BeforeEach.
     */
    @SuppressWarnings("unchecked")
    protected void init(String providerId, String actionName) {
        this.factory = () -> {
            for (ActionExecutor exec : ServiceLoader.load(ActionExecutor.class)) {
                if (providerId.equals(exec.providerId()) && actionName.equals(exec.actionName())) {
                    return exec;
                }
            }
            throw new IllegalStateException("No ActionExecutor found for " + providerId + "/" + actionName);
        };
    }

    protected ActionExecutor action() {
        if (factory == null) {
            throw new IllegalStateException("Action factory not initialized; call init(...) first.");
        }
        return factory.get();
    }

    protected Object send(Job job) throws Exception {
        return LocalActionRunner.send(action(), job);
    }
}

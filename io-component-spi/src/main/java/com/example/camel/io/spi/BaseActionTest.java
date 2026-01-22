package com.example.camel.io.spi;

import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * Minimal base to simplify action unit tests:
 * - provide a supplier for the action under test
 * - use send(job) to execute the action via LocalActionRunner
 */
public abstract class BaseActionTest<T extends ActionExecutor> {
    private final Supplier<T> factory;

    protected BaseActionTest(Supplier<T> factory) {
        this.factory = factory;
    }

    /**
     * Alternate constructor: discover the action via ServiceLoader by providerId and actionName.
     */
    @SuppressWarnings("unchecked")
    protected BaseActionTest(String providerId, String actionName) {
        this.factory = () -> {
            for (ActionExecutor exec : ServiceLoader.load(ActionExecutor.class)) {
                if (providerId.equals(exec.providerId()) && actionName.equals(exec.actionName())) {
                    return (T) exec;
                }
            }
            throw new IllegalStateException("No ActionExecutor found for " + providerId + "/" + actionName);
        };
    }

    protected T action() {
        return factory.get();
    }

    protected Object send(Job job) throws Exception {
        return LocalActionRunner.send(action(), job);
    }
}

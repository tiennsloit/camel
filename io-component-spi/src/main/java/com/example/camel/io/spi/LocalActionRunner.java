package com.example.camel.io.spi;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight runner to execute an ActionExecutor directly using a Job.
 * Intended for unit tests of IO components without spinning up the camel app.
 */
public final class LocalActionRunner {
    private LocalActionRunner() {}

    /**
     * Convenience wrapper: send job using the executor's declared actionName().
     */
    public static Object send(ActionExecutor executor, Job job) throws Exception {
        return run(executor, job, executor.actionName());
    }

    public static Object run(ActionExecutor executor, Job job, String actionName) throws Exception {
        String providerId = executor.providerId();
        Map<String, Object> merged = new HashMap<>();
        merged.putAll(job.parameters());
        merged.putAll(job.headers());
        // Flatten attached params from AuthTokens, if present
        Object authHeader = job.headers().get(AuthTokens.getHeaderName());
        if (authHeader instanceof AuthTokens authTokens) {
            authTokens.tokens().values().forEach(entry -> merged.putAll(entry.attachedParams()));
        }
        Map<String, java.io.File> files = new HashMap<>(job.files());
        ExecuteInput input = new ExecuteInput(providerId, actionName, merged, files);
        return executor.execute(input);
    }
}

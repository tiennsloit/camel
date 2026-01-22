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
        merged.put("files", job.files());
        ExecuteInput input = new ExecuteInput(providerId, actionName, merged);
        return executor.execute(input);
    }
}

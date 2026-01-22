package com.example.camel.io.spi;

/**
 * Generic action executor for dynamic plugin actions.
 * Implementations must be registered via META-INF/services/com.example.camel.io.spi.ActionExecutor
 * inside the plugin JAR.
 */
public interface ActionExecutor {
    /**
     * The provider id this action belongs to (e.g., "onedrive").
     */
    String providerId();

    /**
     * The logical action name (may start with "_" if meant to override Execute).
     */
    String actionName();

    /**
     * Execute the action with the given input.
     */
    Object execute(ExecuteInput input) throws Exception;
}

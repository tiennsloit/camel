package com.example.camel.io.spi;

/**
 * Convenience base class to avoid repeating providerId/actionName boilerplate.
 * Subclasses can:
 * - Call the no-arg constructor to derive providerId from the package name and actionName from the class simple name.
 * - Or call the (providerId, actionName) constructor to set them explicitly.
 */
public abstract class AbstractActionExecutor implements ActionExecutor {
    private final String providerId;
    private final String actionName;

    protected AbstractActionExecutor() {
        this(null, null);
    }

    protected AbstractActionExecutor(String providerId, String actionName) {
        this.providerId = providerId;
        this.actionName = actionName;
    }

    /**
     * Returns the explicitly provided actionName (may be null).
     */
    protected String explicitActionName() {
        return actionName;
    }

    @Override
    public String providerId() {
        if (providerId != null) {
            return providerId;
        }
        return deriveProviderId(getClass());
    }

    @Override
    public String actionName() {
        if (actionName != null) {
            return actionName;
        }
        return deriveUnderscoredActionName();
    }

    private static String deriveProviderId(Class<?> clazz) {
        String pkg = clazz.getPackageName();
        int lastDot = pkg.lastIndexOf('.');
        return lastDot >= 0 ? pkg.substring(lastDot + 1) : pkg;
    }

    /**
     * Helper to derive an action name from the class name:
     * - strips "Action" suffix
     * - strips providerId prefix (case-insensitive)
     * - lower-cases first character
     * - ensures a leading "_"
     */
    protected String deriveUnderscoredActionName() {
        String provider = providerId();
        String simple = getClass().getSimpleName();
        if (simple.endsWith("Action") && simple.length() > "Action".length()) {
            simple = simple.substring(0, simple.length() - "Action".length());
        }
        if (provider != null && simple.toLowerCase().startsWith(provider.toLowerCase())) {
            simple = simple.substring(provider.length());
        }
        if (!simple.isEmpty()) {
            simple = Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
        }
        if (!simple.startsWith("_")) {
            simple = "_" + simple;
        }
        return simple;
    }
}

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
        return getClass().getSimpleName();
    }

    private static String deriveProviderId(Class<?> clazz) {
        String pkg = clazz.getPackageName();
        int lastDot = pkg.lastIndexOf('.');
        return lastDot >= 0 ? pkg.substring(lastDot + 1) : pkg;
    }
}

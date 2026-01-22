package com.example.camel.app;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class FolderProviderRegistry {
    private static final Logger log = LoggerFactory.getLogger(FolderProviderRegistry.class);
    private static final String PROVIDER_CLASS = "com.example.camel.io.spi.FolderInfoProvider";
    private static final String REQUEST_CLASS = "com.example.camel.io.spi.FolderInfoRequest";
    private static final String ACTION_EXECUTOR_CLASS = "com.example.camel.io.spi.ActionExecutor";
    private static final String EXECUTE_INPUT_CLASS = "com.example.camel.io.spi.ExecuteInput";

    private final Map<String, ProviderHandle> providers = new ConcurrentHashMap<>();
    private final Map<String, Map<String, ActionHandle>> actions = new ConcurrentHashMap<>();
    private final Map<String, List<ActionDescriptor>> actionDescriptors = new ConcurrentHashMap<>();
    private final List<URLClassLoader> pluginClassLoaders = new ArrayList<>();
    private final PluginLoaderProperties properties;

    public FolderProviderRegistry(PluginLoaderProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void loadProviders() {
        loadFromClassPath();
        loadFromPluginDirectory();
        log.info("IO providers available: {}", providers.keySet());
        log.info("Actions available: {}", summarizeActions());
    }

    public synchronized void reloadPlugins() {
        closePluginClassLoaders();
        providers.clear();
        actions.clear();
        actionDescriptors.clear();
        pluginClassLoaders.clear();
        loadFromClassPath();
        loadFromPluginDirectory();
        log.info("Reloaded IO providers: {}", providers.keySet());
        log.info("Reloaded actions: {}", summarizeActions());
    }

    public Collection<String> listProviderIds() {
        return providers.keySet();
    }

    public Map<String, Object> invokeGetFolderInfo(String providerId, String folderId, Map<String, String> options) throws Exception {
        ProviderHandle handle = providers.get(providerId);
        if (handle == null) {
            return null;
        }
        Object request = handle.requestCtor().newInstance(folderId, options);
        Object response = handle.getFolderInfo().invoke(handle.instance(), request);
        return toResponseMap(response);
    }

    public List<ActionDescriptorView> listActions(String providerId) {
        List<ActionDescriptor> list = actionDescriptors.getOrDefault(providerId, List.of());
        List<ActionDescriptorView> views = new ArrayList<>();
        for (ActionDescriptor d : list) {
            views.add(new ActionDescriptorView(normalizeActionName(d.name()), d.name(), d.parameters()));
        }
        return views;
    }

    public Object invokeAction(String providerId, String actionPathName, Map<String, Object> params) throws Exception {
        String actualName = resolveActualActionName(providerId, actionPathName);
        Map<String, ActionHandle> byProvider = actions.get(providerId);
        if (byProvider == null) {
            return null;
        }
        ActionHandle handle = byProvider.get(actualName);
        if (handle == null) {
            return null;
        }
        Object input;
        if (handle.executeInputCtor().getParameterCount() == 4) {
            input = handle.executeInputCtor().newInstance(providerId, actualName, params, Map.of());
        } else {
            input = handle.executeInputCtor().newInstance(providerId, actualName, params);
        }
        return handle.execute().invoke(handle.executor(), input);
    }

    private void loadFromClassPath() {
        ClassLoader cl = this.getClass().getClassLoader();
        Class<?> providerInterface = loadClass(PROVIDER_CLASS, cl);
        Class<?> requestClass = loadClass(REQUEST_CLASS, cl);
        Class<?> actionExecutorClass = loadClass(ACTION_EXECUTOR_CLASS, cl);
        Class<?> executeInputClass = loadClass(EXECUTE_INPUT_CLASS, cl);
        if (providerInterface == null || requestClass == null) {
            log.info("SPI classes not on classpath; classpath providers will be skipped");
        } else {
            ServiceLoader<?> loader = ServiceLoader.load(providerInterface, cl);
            for (Object provider : loader) {
                register(provider, requestClass, "classpath");
            }
        }
        if (actionExecutorClass == null || executeInputClass == null) {
            log.info("Action SPI classes not on classpath; classpath actions will be skipped");
        } else {
            ServiceLoader<?> actionLoader = ServiceLoader.load(actionExecutorClass, cl);
            for (Object exec : actionLoader) {
                registerAction(exec, executeInputClass, "classpath");
            }
        }
    }

    private void loadFromPluginDirectory() {
        Path pluginDir = properties.getPluginsDir();
        if (pluginDir == null || !Files.exists(pluginDir)) {
            log.info("Plugin directory {} does not exist, skipping external IO components", pluginDir);
            return;
        }
        try (Stream<Path> stream = Files.list(pluginDir)) {
            URL[] urls = stream
                    .filter(p -> p.toString().endsWith(".jar"))
                    .map(this::toUrl)
                    .toArray(URL[]::new);
            if (urls.length == 0) {
                log.info("No plugin jars found in {}", pluginDir.toAbsolutePath());
                return;
            }
            URLClassLoader cl = new URLClassLoader(urls, this.getClass().getClassLoader());
            pluginClassLoaders.add(cl); // keep open for the lifetime of the app

            Class<?> providerInterface = loadClass(PROVIDER_CLASS, cl);
            Class<?> requestClass = loadClass(REQUEST_CLASS, cl);
            Class<?> actionExecutorClass = loadClass(ACTION_EXECUTOR_CLASS, cl);
            Class<?> executeInputClass = loadClass(EXECUTE_INPUT_CLASS, cl);
            if (providerInterface == null || requestClass == null) {
                log.warn("SPI classes not found in plugin classloader; cannot load providers");
            } else {
                ServiceLoader<?> loader = ServiceLoader.load(providerInterface, cl);
                for (Object provider : loader) {
                    register(provider, requestClass, "pluginDir");
                }
            }
            if (actionExecutorClass == null || executeInputClass == null) {
                log.info("Action SPI classes not found in plugin classloader; skipping actions");
            } else {
                ServiceLoader<?> actionLoader = ServiceLoader.load(actionExecutorClass, cl);
                for (Object exec : actionLoader) {
                    registerAction(exec, executeInputClass, "pluginDir");
                }
            }
            loadActionDescriptorsFromJars(urls);
            loadActionDescriptorsFromJson(pluginDir);
        } catch (IOException e) {
            log.warn("Unable to scan plugin directory {}: {}", pluginDir, e.getMessage());
        }
    }

    private URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid plugin jar path: " + path, e);
        }
    }

    private Class<?> loadClass(String name, ClassLoader cl) {
        try {
            return Class.forName(name, true, cl);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private void register(Object provider, Class<?> requestClass, String source) {
        try {
            Method idMethod = provider.getClass().getMethod("id");
            Method getFolderInfo = provider.getClass().getMethod("getFolderInfo", requestClass);
            Constructor<?> ctor = requestClass.getConstructor(String.class, Map.class);
            String id = (String) idMethod.invoke(provider);
            if (id == null || id.isBlank()) {
                log.warn("Ignoring provider with empty id from {}", source);
                return;
            }
            providers.merge(id, new ProviderHandle(provider, idMethod, getFolderInfo, ctor), (existing, incoming) -> {
                log.warn("Provider id {} already registered from {}, keeping existing instance", id, source);
                return existing;
            });
            log.info("Registered provider '{}' from {}", id, source);
        } catch (Exception e) {
            log.warn("Failed to register provider from {}: {}", source, e.getMessage());
        }
    }

    private void registerAction(Object executor, Class<?> executeInputClass, String source) {
        try {
            Method providerId = executor.getClass().getMethod("providerId");
            Method actionName = executor.getClass().getMethod("actionName");
            Method execute = executor.getClass().getMethod("execute", executeInputClass);

            String pid = (String) providerId.invoke(executor);
            String actName = (String) actionName.invoke(executor);
            if (pid == null || pid.isBlank() || actName == null || actName.isBlank()) {
                log.warn("Ignoring action with empty provider/action from {}", source);
                return;
            }
            Constructor<?> ctor = findExecuteInputCtor(executeInputClass);
            actions.computeIfAbsent(pid, k -> new ConcurrentHashMap<>())
                    .put(actName, new ActionHandle(executor, execute, ctor));
            log.info("Registered action '{}' for provider '{}' from {}", actName, pid, source);
        } catch (Exception e) {
            log.warn("Failed to register action from {}: {}", source, e.getMessage());
        }
    }

    private record ProviderHandle(Object instance, Method idMethod, Method getFolderInfo, Constructor<?> requestCtor) {
    }

    private Map<String, Object> toResponseMap(Object response) throws Exception {
        if (response == null) {
            return null;
        }
        Class<?> clazz = response.getClass();
        Method providerId = clazz.getMethod("providerId");
        Method parentFolderId = clazz.getMethod("parentFolderId");
        Method folders = clazz.getMethod("folders");

        Map<String, Object> out = new HashMap<>();
        out.put("providerId", providerId.invoke(response));
        out.put("parentFolderId", parentFolderId.invoke(response));

        Object folderListObj = folders.invoke(response);
        List<Map<String, Object>> folderMaps = new ArrayList<>();
        if (folderListObj instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                folderMaps.add(toItemMap(item));
            }
        }
        out.put("folders", folderMaps);
        return out;
    }

    private Map<String, Object> toItemMap(Object item) throws Exception {
        Class<?> clazz = item.getClass();
        Method id = clazz.getMethod("id");
        Method name = clazz.getMethod("name");
        Method path = clazz.getMethod("path");
        Method hasChildren = clazz.getMethod("hasChildren");
        Map<String, Object> map = new HashMap<>();
        map.put("id", id.invoke(item));
        map.put("name", name.invoke(item));
        map.put("path", path.invoke(item));
        map.put("hasChildren", hasChildren.invoke(item));
        return map;
    }

    @jakarta.annotation.PreDestroy
    public synchronized void closePluginClassLoaders() {
        for (URLClassLoader cl : pluginClassLoaders) {
            try {
                cl.close();
            } catch (IOException e) {
                log.debug("Error closing plugin classloader: {}", e.getMessage());
            }
        }
    }

    private void loadActionDescriptorsFromJson(Path pluginDir) {
        try (Stream<Path> stream = Files.list(pluginDir)) {
            stream.filter(p -> p.toString().endsWith(".json")).forEach(this::readActionDescriptorFile);
        } catch (IOException e) {
            log.warn("Unable to scan action descriptor JSON in {}: {}", pluginDir, e.getMessage());
        }
    }

    private void loadActionDescriptorsFromJars(URL[] urls) {
        for (URL url : urls) {
            try (JarInputStream jis = new JarInputStream(url.openStream())) {
                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    if (entry.getName().toLowerCase().endsWith(".json")) {
                        readActionDescriptorStream(jis, entry.getName());
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to read action descriptors from jar {}: {}", url, e.getMessage());
            }
        }
    }

    private void readActionDescriptorFile(Path path) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            ActionDescriptorFile file = mapper.readValue(path.toFile(), ActionDescriptorFile.class);
            storeActionDescriptor(file, path.getFileName().toString());
        } catch (Exception e) {
            log.warn("Failed to parse action descriptor {}: {}", path.getFileName(), e.getMessage());
        }
    }

    private void readActionDescriptorStream(java.io.InputStream in, String sourceName) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            ActionDescriptorFile file = mapper.readValue(in, ActionDescriptorFile.class);
            storeActionDescriptor(file, sourceName);
        } catch (Exception e) {
            log.warn("Failed to parse action descriptor {}: {}", sourceName, e.getMessage());
        }
    }

    private void storeActionDescriptor(ActionDescriptorFile file, String sourceName) {
        if (file == null || file.provider == null || file.provider.isBlank()) {
            log.warn("Skipping action descriptor {} because provider is missing", sourceName);
            return;
        }
        List<ActionDescriptor> list = file.actions == null ? List.of() : file.actions;
        actionDescriptors.put(file.provider, list);
        log.info("Loaded {} action descriptors for provider {} from {}", list.size(), file.provider, sourceName);
    }

    private String resolveActualActionName(String providerId, String actionPathName) {
        List<ActionDescriptor> list = actionDescriptors.get(providerId);
        if (list != null) {
            for (ActionDescriptor d : list) {
                if (normalizeActionName(d.name()).equals(actionPathName)) {
                    return d.name();
                }
            }
        }
        return actionPathName;
    }

    private String normalizeActionName(String name) {
        if (name != null && name.startsWith("_") && name.length() > 1) {
            return name.substring(1);
        }
        return name;
    }

    private Map<String, List<String>> summarizeActions() {
        Map<String, List<String>> summary = new HashMap<>();
        actions.forEach((pid, map) -> summary.put(pid, new ArrayList<>(map.keySet())));
        return summary;
    }

    private record ActionDescriptorFile(String provider, List<ActionDescriptor> actions) {}

    private record ActionDescriptor(String name, List<ActionParameter> parameters) {}

    public record ActionParameter(String name, String type) {}

    public record ActionDescriptorView(String exposedName, String actualName, List<ActionParameter> parameters) {}

    private Constructor<?> findExecuteInputCtor(Class<?> executeInputClass) throws NoSuchMethodException {
        try {
            return executeInputClass.getConstructor(String.class, String.class, Map.class, Map.class);
        } catch (NoSuchMethodException e) {
            return executeInputClass.getConstructor(String.class, String.class, Map.class);
        }
    }

    private record ActionHandle(Object executor, Method execute, Constructor<?> executeInputCtor) {}
}

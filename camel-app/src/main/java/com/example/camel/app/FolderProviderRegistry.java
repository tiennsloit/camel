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

    private final Map<String, ProviderHandle> providers = new ConcurrentHashMap<>();
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
    }

    public synchronized void reloadPlugins() {
        closePluginClassLoaders();
        providers.clear();
        pluginClassLoaders.clear();
        loadFromClassPath();
        loadFromPluginDirectory();
        log.info("Reloaded IO providers: {}", providers.keySet());
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

    private void loadFromClassPath() {
        ClassLoader cl = this.getClass().getClassLoader();
        Class<?> providerInterface = loadClass(PROVIDER_CLASS, cl);
        Class<?> requestClass = loadClass(REQUEST_CLASS, cl);
        if (providerInterface == null || requestClass == null) {
            log.info("SPI classes not on classpath; classpath providers will be skipped");
            return;
        }
        ServiceLoader<?> loader = ServiceLoader.load(providerInterface, cl);
        for (Object provider : loader) {
            register(provider, requestClass, "classpath");
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
            if (providerInterface == null || requestClass == null) {
                log.warn("SPI classes not found in plugin classloader; cannot load providers");
                return;
            }
            ServiceLoader<?> loader = ServiceLoader.load(providerInterface, cl);
            for (Object provider : loader) {
                register(provider, requestClass, "pluginDir");
            }
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
}

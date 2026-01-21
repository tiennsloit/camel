package com.example.camel.app;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Component
public class FolderProviderRegistry {
    private static final Logger log = LoggerFactory.getLogger(FolderProviderRegistry.class);
    private static final String PROVIDER_CLASS = "com.example.camel.io.spi.FolderInfoProvider";
    private static final String REQUEST_CLASS = "com.example.camel.io.spi.FolderInfoRequest";

    private final Map<String, ProviderHandle> providers = new ConcurrentHashMap<>();
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

    public Collection<String> listProviderIds() {
        return providers.keySet();
    }

    public Object invokeGetFolderInfo(String providerId, String folderId, Map<String, String> options) throws Exception {
        ProviderHandle handle = providers.get(providerId);
        if (handle == null) {
            return null;
        }
        Object request = handle.requestCtor().newInstance(folderId, options);
        return handle.getFolderInfo().invoke(handle.instance(), request);
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
            try (URLClassLoader cl = new URLClassLoader(urls, this.getClass().getClassLoader())) {
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
}

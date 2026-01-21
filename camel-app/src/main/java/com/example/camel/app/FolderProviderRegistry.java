package com.example.camel.app;

import com.example.camel.io.spi.FolderInfoProvider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class FolderProviderRegistry {
    private static final Logger log = LoggerFactory.getLogger(FolderProviderRegistry.class);

    private final Map<String, FolderInfoProvider> providers = new ConcurrentHashMap<>();
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

    public FolderInfoProvider getProvider(String id) {
        return providers.get(id);
    }

    public Collection<String> listProviderIds() {
        return providers.keySet();
    }

    private void loadFromClassPath() {
        ServiceLoader<FolderInfoProvider> loader = ServiceLoader.load(FolderInfoProvider.class);
        for (FolderInfoProvider provider : loader) {
            register(provider, "classpath");
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
                ServiceLoader<FolderInfoProvider> loader = ServiceLoader.load(FolderInfoProvider.class, cl);
                for (FolderInfoProvider provider : loader) {
                    register(provider, "pluginDir");
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

    private void register(FolderInfoProvider provider, String source) {
        String id = provider.id();
        if (id == null || id.isBlank()) {
            log.warn("Ignoring provider with empty id from {}", source);
            return;
        }
        providers.merge(id, provider, (a, b) -> {
            log.warn("Provider id {} already registered, keeping existing instance", id);
            return a;
        });
        log.info("Registered provider '{}' from {}", id, source);
    }
}

package com.example.camel.app;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties("app.plugin")
public class PluginLoaderProperties {
    /**
     * Directory that holds external IO component jars. Default: ./plugins
     */
    private Path pluginsDir = Path.of("plugins");

    public Path getPluginsDir() {
        return pluginsDir;
    }

    public void setPluginsDir(Path pluginsDir) {
        this.pluginsDir = pluginsDir;
    }
}

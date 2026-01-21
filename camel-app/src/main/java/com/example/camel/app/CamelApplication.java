package com.example.camel.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PluginLoaderProperties.class)
public class CamelApplication {
    public static void main(String[] args) {
        SpringApplication.run(CamelApplication.class, args);
    }
}

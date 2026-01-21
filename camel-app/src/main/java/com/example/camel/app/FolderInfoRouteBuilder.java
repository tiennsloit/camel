package com.example.camel.app;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FolderInfoRouteBuilder extends RouteBuilder {
    private static final Logger log = LoggerFactory.getLogger(FolderInfoRouteBuilder.class);
    private final FolderProviderRegistry registry;

    public FolderInfoRouteBuilder(FolderProviderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void configure() {
        onException(IllegalArgumentException.class)
                .handled(true)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
                .setBody(simple("${exception.message}"));

        restConfiguration()
                .component("platform-http")
                .bindingMode(RestBindingMode.json)
                .dataFormatProperty("prettyPrint", "true")
                .contextPath("/")
                .enableCORS(true);

        rest("/iocomponent")
                .get("/providers")
                    .route()
                    .setBody(exchange -> registry.listProviderIds())
                    .endRest()

                .get("/{provider}/getFolderInfo")
                    .description("List folders for the given provider")
                    .param().name("folderId").type().query().description("Parent folder id (optional for root)").endParam()
                    .route()
                    .process(exchange -> {
                        String providerId = exchange.getMessage().getHeader("provider", String.class);
                        String folderId = exchange.getMessage().getHeader("folderId", String.class);
                        Map<String, String> options = extractOptions(exchange);
                        Object response = registry.invokeGetFolderInfo(providerId, folderId, options);
                        if (response == null) {
                            throw new IllegalArgumentException("Provider not found: " + providerId);
                        }
                        exchange.getMessage().setBody(response);
                    })
                    .log(LoggingLevel.INFO, log.getName(), "Handled getFolderInfo for ${header.provider} parent=${header.folderId}")
                    .endRest();
    }

    private Map<String, String> extractOptions(Exchange exchange) {
        Map<String, Object> headers = exchange.getMessage().getHeaders();
        Map<String, String> options = new HashMap<>();
        headers.forEach((k, v) -> {
            if (k.startsWith("option.")) {
                options.put(k.substring("option.".length()), v == null ? null : v.toString());
            }
        });
        return options;
    }
}

package com.example.camel.app;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FolderInfoRouteBuilder extends RouteBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(FolderInfoRouteBuilder.class);
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
                    .to("direct:listProviders")

                .get("/{provider}/getFolderInfo")
                    .description("List folders for the given provider")
                    .param().name("folderId").type(RestParamType.query).description("Parent folder id (optional for root)").endParam()
                    .to("direct:getFolderInfo")

                .get("/{provider}/actions")
                    .description("List dynamic actions for a provider")
                    .to("direct:listActions")

                .post("/{provider}/{action}")
                    .description("Execute a dynamic action for the provider")
                    .to("direct:executeAction")

                .post("/refresh")
                    .description("Reload IO provider plugins from the plugins directory")
                    .to("direct:reloadPlugins");

        from("direct:listProviders")
                .setBody(exchange -> registry.listProviderIds());

        from("direct:getFolderInfo")
                .process(exchange -> {
                    String providerId = exchange.getMessage().getHeader("provider", String.class);
                    String folderId = exchange.getMessage().getHeader("folderId", String.class);
                    Map<String, String> options = extractOptions(exchange);
                    Map<String, Object> response = registry.invokeGetFolderInfo(providerId, folderId, options);
                    if (response == null) {
                        throw new IllegalArgumentException("Provider not found: " + providerId);
                    }
                    exchange.getMessage().setBody(response);
                })
                .log(LoggingLevel.INFO, LOGGER.getName(), "Handled getFolderInfo for ${header.provider} parent=${header.folderId}");

        from("direct:listActions")
                .setBody(exchange -> {
                    String providerId = exchange.getMessage().getHeader("provider", String.class);
                    return registry.listActions(providerId);
                });

        from("direct:executeAction")
                .process(exchange -> {
                    String providerId = exchange.getMessage().getHeader("provider", String.class);
                    String action = exchange.getMessage().getHeader("action", String.class);
                    Map<String, Object> params = extractBodyAsMap(exchange);
                    Map<String, String> queryParams = extractOptions(exchange);
                    params.putAll(new HashMap<>(queryParams));
                    Object result = registry.invokeAction(providerId, action, params);
                    if (result == null) {
                        throw new IllegalArgumentException("Action not found: " + providerId + "/" + action);
                    }
                    exchange.getMessage().setBody(result);
                })
                .log(LoggingLevel.INFO, LOGGER.getName(), "Handled action ${header.provider}/${header.action}");

        from("direct:reloadPlugins")
                .process(exchange -> registry.reloadPlugins())
                .setBody(constant(Map.of("status", "ok", "message", "Plugins reloaded")));
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractBodyAsMap(Exchange exchange) {
        Object body = exchange.getMessage().getBody();
        if (body instanceof Map<?, ?> m) {
            return new HashMap<>((Map<String, Object>) m);
        }
        Map<?, ?> converted = exchange.getContext().getTypeConverter().convertTo(Map.class, exchange, body);
        if (converted != null) {
            return new HashMap<>((Map<String, Object>) converted);
        }
        return new HashMap<>();
    }
}

package com.hogwai.perf.optimized.config;

import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebServerConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatHttp2Customizer() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> {
                // HTTP/2 enabled via server.http2.enabled=true in properties
            });
        };
    }
}

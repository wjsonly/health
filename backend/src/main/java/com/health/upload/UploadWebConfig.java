package com.health.upload;

import java.nio.file.Path;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(UploadProperties.class)
public class UploadWebConfig implements WebMvcConfigurer {
    private final UploadProperties properties;

    public UploadWebConfig(UploadProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(properties.getDirectory()).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler(properties.normalizedPublicPrefix() + "**")
                .addResourceLocations(location);
    }
}

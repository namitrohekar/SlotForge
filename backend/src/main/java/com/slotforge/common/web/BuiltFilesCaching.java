package com.slotforge.common.web;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets browsers keep the built JavaScript and CSS for a year. Vite puts a content hash in each name under /assets/,
 * so a new build gets new names. index.html is left alone, so a new deploy is picked up on the next visit.
 */
@Configuration
public class BuiltFilesCaching implements WebMvcConfigurer {

    private final String[] assetLocations;

    public BuiltFilesCaching(WebProperties web) {
        this.assetLocations = Arrays.stream(web.getResources().getStaticLocations())
                .map(location -> location + "assets/")
                .toArray(String[]::new);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(assetLocations)
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
    }
}

package com.chickenexpress.foodorder.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for ChickenExpress.
 *
 * Handles:
 * - Static resource serving (CSS, JS, images from /static/)
 * - Product image uploads served from the filesystem (configurable path)
 *
 * Note: Spring Boot auto-configures most defaults. Override here only
 * what needs custom behavior.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Serve uploaded product images from the local filesystem.
     *
     * Files placed in ./uploads/products/ on the server will be accessible
     * at /uploads/products/** in the browser.
     *
     * In development this resolves relative to the working directory.
     * In production, set an absolute path via an application property.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Uploaded product images (outside the classpath, writable at runtime)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");

        // Default classpath statics — Spring Boot handles /static/** automatically,
        // but declaring it here keeps everything visible in one place.
        registry.addResourceHandler("/css/**", "/js/**", "/images/**")
                .addResourceLocations(
                    "classpath:/static/css/",
                    "classpath:/static/js/",
                    "classpath:/static/images/"
                );
    }
}

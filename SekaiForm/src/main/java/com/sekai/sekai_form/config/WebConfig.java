package com.sekai.sekai_form.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private static Path resolveModelsDir() {
        String jarDir = System.getProperty("sekai.model.dir");
        if (jarDir != null && !jarDir.isBlank()) return Paths.get(jarDir).toAbsolutePath().normalize();
        Path cwd = Paths.get("").toAbsolutePath();
        Path candidate = cwd.resolve("models");
        if (Files.isDirectory(candidate)) return candidate.normalize();
        candidate = cwd.resolve("SekaiForm").resolve("models");
        if (Files.isDirectory(candidate)) return candidate.normalize();
        return cwd.resolve("models").normalize();
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path modelsPath = resolveModelsDir();
        registry.addResourceHandler("/models/**").addResourceLocations(modelsPath.toUri().toString()).setCachePeriod(0);
        Path live2dPath = modelsPath.resolve("2d");
        registry.addResourceHandler("/live2d-models/**").addResourceLocations(live2dPath.toUri().toString()).setCachePeriod(0);
    }
}
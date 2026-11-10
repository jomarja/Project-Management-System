package com.pms.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileStorageConfig {

    // Creates the upload folder on startup (if missing) and shares its location with other beans
    @Bean
    public Path uploadDirectory(@Value("${app.upload.dir}") String uploadDir) throws IOException {
        return Files.createDirectories(Path.of(uploadDir));
    }
}

package com.pms.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileStorageConfigTest {

    @TempDir
    Path tempDir;

    private final FileStorageConfig fileStorageConfig = new FileStorageConfig();

    @Test
    void uploadDirectory_createsMissingFolder() throws IOException {
        Path folder = tempDir.resolve("uploads/nested");

        Path result = fileStorageConfig.uploadDirectory(folder.toString());

        assertThat(result).isEqualTo(folder).isDirectory();
    }

    @Test
    void uploadDirectory_reusesExistingFolder() throws IOException {
        Path folder = Files.createDirectory(tempDir.resolve("uploads"));

        Path result = fileStorageConfig.uploadDirectory(folder.toString());

        assertThat(result).isEqualTo(folder).isDirectory();
    }
}

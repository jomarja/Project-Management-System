package com.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.pms.exception.InvalidFileException;

class FileStorageServiceTest {

    @TempDir
    Path uploadDirectory;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(uploadDirectory);
    }

    @Test
    void store_savesFileAndReturnsItsPath() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", "content".getBytes());

        Path storedFile = Path.of(fileStorageService.store(file));

        assertThat(storedFile.getParent()).isEqualTo(uploadDirectory);
        assertThat(storedFile.getFileName().toString()).endsWith("_report.pdf");
        assertThat(Files.readString(storedFile)).isEqualTo("content");
    }

    @Test
    void store_givesEveryUploadAUniqueName() {
        MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", "content".getBytes());

        String firstPath = fileStorageService.store(file);
        String secondPath = fileStorageService.store(file);

        assertThat(firstPath).isNotEqualTo(secondPath);
    }

    @Test
    void store_dropsFolderPartsFromFileName() {
        MockMultipartFile file = new MockMultipartFile("file", "../../evil.txt", "text/plain", "x".getBytes());

        Path storedFile = Path.of(fileStorageService.store(file));

        assertThat(storedFile.getParent()).isEqualTo(uploadDirectory);
        assertThat(storedFile.getFileName().toString()).endsWith("_evil.txt");
    }

    @Test
    void store_rejectsEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> fileStorageService.store(emptyFile))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("Cannot upload an empty file");
    }

    @Test
    void store_throwsWhenFileCannotBeWritten() {
        FileStorageService serviceWithMissingFolder = new FileStorageService(uploadDirectory.resolve("missing"));
        MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> serviceWithMissingFolder.store(file))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageStartingWith("Could not store file");
    }
}

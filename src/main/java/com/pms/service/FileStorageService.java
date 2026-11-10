package com.pms.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.pms.exception.InvalidFileException;

@Service
public class FileStorageService {

    private final Path uploadDirectory;

    // The upload folder comes from FileStorageConfig
    public FileStorageService(Path uploadDirectory) {
        this.uploadDirectory = uploadDirectory;
    }

    // Saves the file into the upload folder and returns the path where it was stored
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("Cannot upload an empty file");
        }

        Path target = uploadDirectory.resolve(buildUniqueFileName(file));
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store file " + target.getFileName(), e);
        }
        return target.toString();
    }

    // Keeps only the plain file name (drops folders like "../") and adds a random prefix so names never clash
    private String buildUniqueFileName(MultipartFile file) {
        String originalName = StringUtils.getFilename(StringUtils.cleanPath(file.getOriginalFilename()));
        return UUID.randomUUID() + "_" + originalName;
    }
}

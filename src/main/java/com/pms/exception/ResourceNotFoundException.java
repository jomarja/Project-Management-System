package com.pms.exception;

// Thrown when a requested project, task or tag does not exist (mapped to HTTP 404)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        this(resourceName + " not found with id " + id);
    }
}

package com.pms.exception;

// Thrown when an uploaded file cannot be accepted, e.g. it is empty (mapped to HTTP 400)
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}

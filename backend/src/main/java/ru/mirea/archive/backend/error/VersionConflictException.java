package ru.mirea.archive.backend.error;

import org.springframework.http.HttpStatus;

public class VersionConflictException extends ApiException {
    public VersionConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}

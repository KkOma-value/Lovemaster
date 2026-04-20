package org.example.springai_learn.ai.exception;

public class RewriteException extends RuntimeException {
    public RewriteException(String message) {
        super(message);
    }

    public RewriteException(String message, Throwable cause) {
        super(message, cause);
    }
}

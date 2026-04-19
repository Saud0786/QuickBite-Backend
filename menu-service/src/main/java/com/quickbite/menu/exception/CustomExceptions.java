package com.quickbite.menu.exception;

/**
 * Domain-specific exceptions for Restaurant-Service.
 * Mirrors the CustomExceptions pattern from auth-service.
 */
public class CustomExceptions {

    public static class RestaurantNotFoundException extends RuntimeException {
        public RestaurantNotFoundException(String message) {
            super(message);
        }
    }

    public static class RestaurantAlreadyExistsException extends RuntimeException {
        public RestaurantAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class UnauthorizedAccessException extends RuntimeException {
        public UnauthorizedAccessException(String message) {
            super(message);
        }
    }

    public static class RestaurantNotApprovedException extends RuntimeException {
        public RestaurantNotApprovedException(String message) {
            super(message);
        }
    }

    public static class InvalidOperationException extends RuntimeException {
        public InvalidOperationException(String message) {
            super(message);
        }
    }
}

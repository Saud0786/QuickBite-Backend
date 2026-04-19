package com.quickbite.menu.exception;

import com.quickbite.menu.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomExceptions.RestaurantNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(CustomExceptions.RestaurantNotFoundException ex) {
        log.error("Restaurant not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), 404));
    }

    @ExceptionHandler(CustomExceptions.RestaurantAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<?>> handleAlreadyExists(CustomExceptions.RestaurantAlreadyExistsException ex) {
        log.error("Restaurant already exists: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), 409));
    }

    @ExceptionHandler(CustomExceptions.UnauthorizedAccessException.class)
    public ResponseEntity<ApiResponse<?>> handleUnauthorized(CustomExceptions.UnauthorizedAccessException ex) {
        log.error("Unauthorized: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage(), 403));
    }

    @ExceptionHandler(CustomExceptions.RestaurantNotApprovedException.class)
    public ResponseEntity<ApiResponse<?>> handleNotApproved(CustomExceptions.RestaurantNotApprovedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage(), 403));
    }

    @ExceptionHandler(CustomExceptions.InvalidOperationException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidOp(CustomExceptions.InvalidOperationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), 400));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied", 403));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String field = ((FieldError) err).getField();
            errors.put(field, err.getDefaultMessage());
        });
        log.error("Validation errors: {}", errors);
        ApiResponse<Map<String, String>> response = ApiResponse.error("Validation failed", 400);
        response.setData(errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneric(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later.", 500));
    }
}

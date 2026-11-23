package com.lostfound.exception;

import com.lostfound.dto.ApiResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Centralised, consistent JSON error responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String message, Map<String, String> errors) {
        return ResponseEntity.status(status).body(ApiResponse.error(message, errors));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(ResourceNotFoundException e) { return build(HttpStatus.NOT_FOUND, e.getMessage(), null); }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> noResource(NoResourceFoundException e) { return build(HttpStatus.NOT_FOUND, "Resource not found", null); }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> duplicate(DuplicateResourceException e) { return build(HttpStatus.CONFLICT, e.getMessage(), null); }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> transition(InvalidStateTransitionException e) { return build(HttpStatus.CONFLICT, e.getMessage(), null); }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<Void>> rule(BusinessRuleException e) { return build(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), null); }

    @ExceptionHandler(ValidationFailedException.class)
    public ResponseEntity<ApiResponse<Void>> validation(ValidationFailedException e) { return build(HttpStatus.BAD_REQUEST, e.getMessage(), e.getErrors()); }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> unauthorized(UnauthorizedException e) { return build(HttpStatus.UNAUTHORIZED, e.getMessage(), null); }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> denied(AccessDeniedException e) { return build(HttpStatus.FORBIDDEN, e.getMessage() == null ? "Access denied" : e.getMessage(), null); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> invalid(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errors.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ApiResponse<Void>> badRequest(Exception e) { return build(HttpStatus.BAD_REQUEST, "Malformed or incomplete request", null); }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> method(HttpRequestMethodNotSupportedException e) { return build(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage(), null); }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> tooLarge(MaxUploadSizeExceededException e) { return build(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file is too large", null); }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> lock(PessimisticLockingFailureException e) { return build(HttpStatus.CONFLICT, "The record is being modified by someone else. Please retry.", null); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> integrity(DataIntegrityViolationException e) { return build(HttpStatus.CONFLICT, "The operation conflicts with existing data (duplicate or referenced record)", null); }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> database(DataAccessException e) { return build(HttpStatus.SERVICE_UNAVAILABLE, "Database is currently unavailable. Please try again later.", null); }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> other(Exception e) { return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", null); }
}

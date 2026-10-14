package com.lipari.bank.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String PROBLEM_TYPE_BASE = "https://api.liparibank.it/errors/";

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Risorsa non trovata: {} con ID '{}' — path: {}",
                ex.getResourceType(), ex.getResourceId(), request.getRequestURI());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setType(URI.create(PROBLEM_TYPE_BASE + "resource-not-found"));
        problemDetail.setTitle("Risorsa Non Trovata");
        problemDetail.setProperty("errorCode", ex.getErrorCode());
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("resourceType", ex.getResourceType());
        problemDetail.setProperty("resourceId", ex.getResourceId());

        return problemDetail;
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ProblemDetail handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        log.warn("Violazione regola di business: {} — errorCode: {} — path: {}",
                ex.getMessage(), ex.getErrorCode(), request.getRequestURI());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                ex.getHttpStatus(), ex.getMessage());
        problemDetail.setType(URI.create(PROBLEM_TYPE_BASE + "business-rule-violation"));
        problemDetail.setTitle("Regola di Business Violata");
        problemDetail.setProperty("errorCode", ex.getErrorCode());
        problemDetail.setProperty("timestamp", Instant.now().toString());

        return problemDetail;
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleConflictException(
            ConflictException ex, HttpServletRequest request) {

        log.warn("Conflitto: {} — path: {}", ex.getMessage(), request.getRequestURI());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setType(URI.create(PROBLEM_TYPE_BASE + "conflict"));
        problemDetail.setTitle("Conflitto");
        problemDetail.setProperty("errorCode", ex.getErrorCode());
        problemDetail.setProperty("timestamp", Instant.now().toString());

        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ValidationErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> new ValidationErrorDetail(
                        error.getField(),
                        error.getDefaultMessage(),
                        formatRejectedValue(error)
                ))
                .toList();

        log.warn("Errori di validazione: {} campo/i non valido/i — path: {}",
                fieldErrors.size(), request.getRequestURI());

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setType(URI.create(PROBLEM_TYPE_BASE + "validation-error"));
        problemDetail.setTitle("Errori di Validazione");
        problemDetail.setDetail(
                String.format("%d campo/i non valido/i nella richiesta", fieldErrors.size()));
        problemDetail.setProperty("errorCode", "VALIDATION_ERROR");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("errors", fieldErrors);

        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Errore interno non gestito — path: {} — tipo: {}",
                request.getRequestURI(), ex.getClass().getSimpleName(), ex);

        String requestId = MDC.get("requestId");

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Si è verificato un errore interno. Contattare il supporto con il riferimento: "
                        + (requestId != null ? requestId : "N/A"));
        problemDetail.setType(URI.create(PROBLEM_TYPE_BASE + "internal-error"));
        problemDetail.setTitle("Errore Interno del Server");
        problemDetail.setProperty("errorCode", "INTERNAL_SERVER_ERROR");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        if (requestId != null) {
            problemDetail.setProperty("supportReference", requestId);
        }

        return problemDetail;
    }

    record ValidationErrorDetail(String field, String message, String rejectedValue) {}

    private String formatRejectedValue(FieldError error) {
        if (error.getRejectedValue() == null) return null;
        String fieldName = error.getField().toLowerCase();
        if (fieldName.contains("password") || fieldName.contains("token")) {
            return "***";
        }
        return error.getRejectedValue().toString();
    }
}

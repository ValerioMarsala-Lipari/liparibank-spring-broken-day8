package com.lipari.bank.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class DatabaseExceptionHandler {

    private static final String PROBLEM_TYPE_BASE = "https://api.liparibank.it/errors/";

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.debug("DataIntegrityViolationException dettaglio interno: {}", ex.getMessage());
        log.warn("Violazione integrità dati — path: {}", request.getRequestURI());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Operazione non consentita: violazione di unicità o integrità dei dati. " +
                "Verificare che non esista già una risorsa con gli stessi dati univoci.");
        problemDetail.setType(URI.create(PROBLEM_TYPE_BASE + "data-integrity-violation"));
        problemDetail.setTitle("Violazione Integrità Dati");
        problemDetail.setProperty("errorCode", "DATA_INTEGRITY_VIOLATION");
        problemDetail.setProperty("timestamp", Instant.now().toString());

        return problemDetail;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {

        log.warn("Conflitto ottimistico su entità: {} — path: {}",
                ex.getPersistentClass() != null
                        ? ex.getPersistentClass().getSimpleName() : "unknown",
                request.getRequestURI());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Conflitto durante l'aggiornamento: la risorsa è stata modificata da " +
                "un'altra operazione. Ricaricare i dati e riprovare.");
        problemDetail.setType(URI.create(PROBLEM_TYPE_BASE + "optimistic-lock"));
        problemDetail.setTitle("Conflitto di Concorrenza");
        problemDetail.setProperty("errorCode", "OPTIMISTIC_LOCK_CONFLICT");
        problemDetail.setProperty("timestamp", Instant.now().toString());

        return problemDetail;
    }
}

package com.gamecoach.controller;

import com.gamecoach.service.CoachingUnavailableException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

/** Turns exceptions from any controller into proper HTTP error responses, in one place. */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Unknown match or player: 404 with the reason in the "detail" field. */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> notFound(NoSuchElementException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    /** The AI coach failed (bad key, rate limit, outage): 503 "try again later", with the reason. */
    @ExceptionHandler(CoachingUnavailableException.class)
    public ResponseEntity<ProblemDetail> coachingUnavailable(CoachingUnavailableException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }
}

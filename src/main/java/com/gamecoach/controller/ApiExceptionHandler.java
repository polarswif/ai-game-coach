package com.gamecoach.controller;

import com.gamecoach.parsing.BadUploadException;
import com.gamecoach.parsing.ScoreboardUnreadableException;
import com.gamecoach.service.CoachingUnavailableException;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

    /** The upload itself is unusable (empty, too big, wrong format, malformed score): 400. */
    @ExceptionHandler(BadUploadException.class)
    public ResponseEntity<ProblemDetail> badUpload(BadUploadException e) {
        return problem(400, e.getMessage());
    }

    /** The image arrived but no scoreboard could be read from it: 422 Unprocessable Content. */
    @ExceptionHandler(ScoreboardUnreadableException.class)
    public ResponseEntity<ProblemDetail> unreadable(ScoreboardUnreadableException e) {
        return problem(422, e.getMessage());
    }

    /** Larger than spring.servlet.multipart.max-file-size: 413 instead of a generic error page. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> tooLarge(MaxUploadSizeExceededException e) {
        return problem(413, "The upload is too large. Screenshots can be up to 5 MB.");
    }

    private static ResponseEntity<ProblemDetail> problem(int status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail));
    }
}

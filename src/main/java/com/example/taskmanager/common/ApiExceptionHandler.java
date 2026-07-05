package com.example.taskmanager.common;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(ResponseStatusException ex) {
        String message = ex.getReason() == null ? "Request failed" : ex.getReason();
        ProblemDetail problemDetail = ProblemDetail.forStatus(ex.getStatusCode());
        problemDetail.setDetail(message);
        return ResponseEntity.status(ex.getStatusCode()).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        if ("id".equals(ex.getName())) {
            String value = ex.getValue() == null ? "null" : ex.getValue().toString();
            ProblemDetail problemDetail = ProblemDetail.forStatus(400);
            problemDetail.setDetail("Invalid 'id' format: " + value + ". Expected UUID.");
            return ResponseEntity.badRequest().body(problemDetail);
        }

        ProblemDetail problemDetail = ProblemDetail.forStatus(400);
        problemDetail.setDetail("Invalid value for parameter '" + ex.getName() + "'.");
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(409);
        problemDetail.setDetail("Task has been updated by another user.");
        return ResponseEntity.status(409).body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            org.springframework.http.HttpStatusCode status,
            WebRequest request
    ) {
        Throwable rootCause = ex.getMostSpecificCause();
        String detail = rootCause != null && rootCause.getMessage() != null
                ? rootCause.getMessage()
                : "Failed to read request";

        ProblemDetail problemDetail = ProblemDetail.forStatus(400);
        problemDetail.setDetail(detail);
        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }
}



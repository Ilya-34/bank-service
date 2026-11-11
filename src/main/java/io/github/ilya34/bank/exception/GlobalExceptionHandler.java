package io.github.ilya34.bank.exception;

import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Maps every error to an RFC 7807 ProblemDetail with a stable machine-readable "code". */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    ProblemDetail badRequest(BadRequestException ex) {
        return problem(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Bad request", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, ex.getCode(), "Conflict", ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail businessRule(BusinessRuleException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), "Business rule violated", ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail optimisticLock(ObjectOptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "Conflict",
                "The resource was modified concurrently, please retry");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "CONSTRAINT_VIOLATION", "Conflict",
                "The request conflicts with existing data");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal error",
                "Unexpected error");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldViolation(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed",
                "Request validation failed");
        pd.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(pd);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> res = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (res != null && res.getBody() instanceof ProblemDetail pd && pd.getProperties() == null) {
            pd.setProperty("code", statusCode.is4xxClientError() ? "BAD_REQUEST" : "INTERNAL_ERROR");
        }
        return res;
    }

    private static ProblemDetail problem(HttpStatus status, String code, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("urn:bank-service:problem:" + code.toLowerCase().replace('_', '-')));
        pd.setProperty("code", code);
        return pd;
    }

    public record FieldViolation(String field, String message) {
    }
}

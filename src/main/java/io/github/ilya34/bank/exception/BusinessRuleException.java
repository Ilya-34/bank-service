package io.github.ilya34.bank.exception;

/** A well-formed request that violates a domain rule (maps to HTTP 422). */
public class BusinessRuleException extends RuntimeException {
    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}

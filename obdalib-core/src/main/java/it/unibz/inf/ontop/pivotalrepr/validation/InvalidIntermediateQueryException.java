package it.unibz.inf.ontop.pivotalrepr.validation;

/**
 * Thrown by validators
 */
public class InvalidIntermediateQueryException extends Exception {
    public InvalidIntermediateQueryException(String message) {
        super(message);
    }
}

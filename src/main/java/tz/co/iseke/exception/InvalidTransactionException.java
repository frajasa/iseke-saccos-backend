package tz.co.iseke.exception;

public class InvalidTransactionException extends BusinessException {
    public InvalidTransactionException(String message) {
        super(message);
    }
}
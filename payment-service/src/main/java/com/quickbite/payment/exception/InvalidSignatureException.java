package com.quickbite.payment.exception;

public class InvalidSignatureException extends RuntimeException {
    public InvalidSignatureException(String message) { super(message); }
}

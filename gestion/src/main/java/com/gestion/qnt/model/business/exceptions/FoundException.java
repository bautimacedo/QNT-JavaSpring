package com.gestion.qnt.model.business.exceptions;

public class FoundException extends Exception {

    public FoundException() {
        super();
    }

    public FoundException(String message) {
        super(message);
    }

    public FoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public FoundException(Throwable cause) {
        super(cause);
    }
}

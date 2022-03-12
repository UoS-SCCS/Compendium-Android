package com.castellate.compendium.exceptions;

public class CompendiumException extends Exception{
    public CompendiumException() {
    }

    public CompendiumException(String message) {
        super(message);
    }

    public CompendiumException(String message, Throwable cause) {
        super(message, cause);
    }

    public CompendiumException(Throwable cause) {
        super(cause);
    }

    public CompendiumException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

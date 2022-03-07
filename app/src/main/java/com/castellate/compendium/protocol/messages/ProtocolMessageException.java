package com.castellate.compendium.protocol.messages;

public class ProtocolMessageException extends Exception {
    public ProtocolMessageException() {
    }

    public ProtocolMessageException(String message) {
        super(message);
    }

    public ProtocolMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProtocolMessageException(Throwable cause) {
        super(cause);
    }

    public ProtocolMessageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

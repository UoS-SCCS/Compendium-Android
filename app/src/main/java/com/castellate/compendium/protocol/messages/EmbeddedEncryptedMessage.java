package com.castellate.compendium.protocol.messages;

public interface EmbeddedEncryptedMessage {
    public String getEncryptedMsgField();
    public Class<?> getEncryptedMessageClass();
}
